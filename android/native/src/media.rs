pub(crate) fn strip_html_tags(input: &str) -> String {
    let mut out = String::with_capacity(input.len());
    let mut in_tag = false;
    for c in input.chars() {
        match c {
            '<' => in_tag = true,
            '>' => in_tag = false,
            _ if !in_tag => out.push(c),
            _ => {}
        }
    }
    out.replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .trim()
        .to_string()
}


pub(crate) fn html_to_markdown(input: &str) -> String {
    let mut lines: Vec<String> = Vec::new();
    let mut current = String::new();
    let mut quote_depth = 0usize;
    let mut anchors: Vec<String> = Vec::new();
    let mut i = 0usize;
    while i < input.len() {
        let ch = input[i..].chars().next().unwrap_or_default();
        if ch != '<' {
            push_html_text(&mut current, ch);
            i += ch.len_utf8();
            continue;
        }
        let Some(end_rel) = input[i + 1..].find('>') else {
            for ch in input[i..].chars() {
                push_html_text(&mut current, ch);
            }
            break;
        };
        let tag_end = i + 1 + end_rel;
        let tag = &input[i + 1..tag_end];
        let lower = tag.trim().to_ascii_lowercase();
        let closing = lower.starts_with('/');
        let name = lower
            .trim_start_matches('/')
            .split_whitespace()
            .next()
            .unwrap_or("")
            .trim_end_matches('/');
        match name {
            "blockquote" => {
                flush_html_line(&mut lines, &mut current, quote_depth);
                if closing {
                    quote_depth = quote_depth.saturating_sub(1);
                    push_html_blank(&mut lines, quote_depth);
                } else {
                    quote_depth += 1;
                }
            }
            "p" | "div" | "h1" | "h2" | "h3" | "h4" | "h5" | "h6" | "li" | "tr" => {
                flush_html_line(&mut lines, &mut current, quote_depth);
                if closing {
                    push_html_blank(&mut lines, quote_depth);
                }
            }
            "br" => flush_html_line(&mut lines, &mut current, quote_depth),
            "img" => {
                if let Some(src) = html_tag_attr(tag, "src") {
                    current.push_str("![图片](");
                    current.push_str(&decode_html_entities(src));
                    current.push(')');
                }
            }
            "a" => {
                if closing {
                    if let Some(href) = anchors.pop() {
                        current.push_str("](");
                        current.push_str(&href);
                        current.push(')');
                    }
                } else if let Some(href) = html_tag_attr(tag, "href") {
                    anchors.push(decode_html_entities(href));
                    current.push('[');
                }
            }
            _ => {}
        }
        i = tag_end + 1;
    }
    flush_html_line(&mut lines, &mut current, quote_depth);
    while lines.last().is_some_and(|line| {
        line.trim().is_empty() || line.chars().all(|c| c == '>' || c.is_whitespace())
    }) {
        lines.pop();
    }
    let mut out = lines.join("\n");
    while out.contains("\n\n\n") {
        out = out.replace("\n\n\n", "\n\n");
    }
    out.trim().to_string()
}

fn push_html_text(out: &mut String, ch: char) {
    if ch.is_whitespace() {
        if !out.is_empty() && !out.ends_with(' ') {
            out.push(' ');
        }
    } else {
        out.push(ch);
    }
}

fn flush_html_line(lines: &mut Vec<String>, current: &mut String, depth: usize) {
    let text = decode_html_entities(current.trim());
    current.clear();
    if text.is_empty() {
        return;
    }
    let prefix = "> ".repeat(depth);
    let line = format!("{prefix}{text}");
    if lines.last() != Some(&line) {
        lines.push(line);
    }
}

fn push_html_blank(lines: &mut Vec<String>, depth: usize) {
    let blank = if depth == 0 {
        String::new()
    } else {
        "> ".repeat(depth).trim_end().to_string()
    };
    if lines.last() != Some(&blank) {
        lines.push(blank);
    }
}

fn html_tag_attr<'a>(tag: &'a str, name: &str) -> Option<&'a str> {
    let lower = tag.to_ascii_lowercase();
    let pos = lower.find(name)?;
    let rest = &tag[pos + name.len()..];
    let eq = rest.find('=')?;
    read_attr_value(rest[eq + 1..].trim_start())
}

fn decode_html_entities(input: &str) -> String {
    input
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
}

pub(crate) fn extract_image_urls(html: &str, markdown_or_text: &str) -> Vec<String> {
    let mut urls = Vec::new();
    collect_img_src_urls(html, &mut urls);
    collect_plain_image_urls(html, &mut urls);
    collect_markdown_image_urls(markdown_or_text, &mut urls);
    collect_plain_image_urls(markdown_or_text, &mut urls);
    urls.sort();
    urls.dedup();
    urls
}

pub(crate) fn extract_video_urls(html: &str, markdown_or_text: &str) -> Vec<String> {
    let mut urls = Vec::new();
    for url in extract_link_urls(html, markdown_or_text) {
        let lower = url.to_ascii_lowercase();
        if [".mp4", ".m3u8", ".webm", ".mov"]
            .iter()
            .any(|ext| lower.contains(ext))
        {
            urls.push(url);
        }
    }
    urls.sort();
    urls.dedup();
    urls
}

pub(crate) fn extract_link_urls(html: &str, markdown_or_text: &str) -> Vec<String> {
    let mut urls = Vec::new();
    collect_anchor_href_urls(html, &mut urls);
    collect_markdown_link_urls(markdown_or_text, &mut urls);
    collect_plain_link_urls(html, &mut urls);
    collect_plain_link_urls(markdown_or_text, &mut urls);
    urls.sort();
    urls.dedup();
    urls
}

pub(crate) fn collect_anchor_href_urls(input: &str, urls: &mut Vec<String>) {
    let lower = input.to_ascii_lowercase();
    let mut offset = 0;
    while let Some(anchor_pos) = lower[offset..].find("<a") {
        let start = offset + anchor_pos;
        let Some(end_rel) = lower[start..].find('>') else {
            break;
        };
        let tag_end = start + end_rel;
        let tag = &input[start..=tag_end];
        let tag_lower = &lower[start..=tag_end];
        if let Some(href_pos) = tag_lower.find("href") {
            let rest = &tag[href_pos + 4..];
            if let Some(eq_pos) = rest.find('=') {
                let value = rest[eq_pos + 1..].trim_start();
                if let Some(url) = read_attr_value(value) {
                    push_link_url(url, urls);
                }
            }
        }
        offset = tag_end + 1;
    }
}

pub(crate) fn collect_markdown_link_urls(input: &str, urls: &mut Vec<String>) {
    let mut offset = 0;
    while let Some(pos) = input[offset..].find("](") {
        let start = offset + pos + 2;
        if let Some(end_rel) = input[start..].find(')') {
            push_link_url(&input[start..start + end_rel], urls);
            offset = start + end_rel + 1;
        } else {
            break;
        }
    }
}

pub(crate) fn collect_plain_link_urls(input: &str, urls: &mut Vec<String>) {
    for token in
        input.split(|c: char| c.is_whitespace() || c == '"' || c == '\'' || c == '<' || c == '>')
    {
        push_link_url(token, urls);
    }
}

pub(crate) fn collect_img_src_urls(input: &str, urls: &mut Vec<String>) {
    let lower = input.to_ascii_lowercase();
    let mut offset = 0;
    while let Some(img_pos) = lower[offset..].find("<img") {
        let start = offset + img_pos;
        let Some(end_rel) = lower[start..].find('>') else {
            break;
        };
        let tag_end = start + end_rel;
        let tag = &input[start..=tag_end];
        let tag_lower = &lower[start..=tag_end];
        if let Some(src_pos) = tag_lower.find("src") {
            let rest = &tag[src_pos + 3..];
            if let Some(eq_pos) = rest.find('=') {
                let value = rest[eq_pos + 1..].trim_start();
                if let Some(url) = read_attr_value(value) {
                    push_image_url(url, urls);
                }
            }
        }
        offset = tag_end + 1;
    }
}

pub(crate) fn read_attr_value(input: &str) -> Option<&str> {
    let mut chars = input.chars();
    match chars.next()? {
        '"' => input[1..].find('"').map(|end| &input[1..1 + end]),
        '\'' => input[1..].find('\'').map(|end| &input[1..1 + end]),
        _ => Some(input.split_whitespace().next().unwrap_or(input)),
    }
}

pub(crate) fn collect_markdown_image_urls(input: &str, urls: &mut Vec<String>) {
    let mut offset = 0;
    while let Some(pos) = input[offset..].find("](") {
        let start = offset + pos + 2;
        if start >= 2
            && input[..start - 2].ends_with('!')
            && let Some(end_rel) = input[start..].find(')')
        {
            push_image_url(&input[start..start + end_rel], urls);
            offset = start + end_rel + 1;
            continue;
        }
        offset = start;
    }
}

pub(crate) fn collect_plain_image_urls(input: &str, urls: &mut Vec<String>) {
    for token in
        input.split(|c: char| c.is_whitespace() || c == '"' || c == '\'' || c == '<' || c == '>')
    {
        let cleaned = token.trim_matches(|c: char| matches!(c, ')' | '(' | ',' | ';'));
        push_image_url(cleaned, urls);
    }
}

pub(crate) fn push_image_url(candidate: &str, urls: &mut Vec<String>) {
    let url = clean_url(candidate);
    let lower = url.to_ascii_lowercase();
    let is_http = lower.starts_with("http://") || lower.starts_with("https://");
    let is_image = [".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp"]
        .iter()
        .any(|ext| lower.contains(ext));
    if is_http && is_image {
        urls.push(url);
    }
}

pub(crate) fn push_link_url(candidate: &str, urls: &mut Vec<String>) {
    let url = clean_url(candidate);
    let lower = url.to_ascii_lowercase();
    if lower.starts_with("http://") || lower.starts_with("https://") {
        urls.push(url);
    }
}

pub(crate) fn clean_url(candidate: &str) -> String {
    let cleaned = candidate
        .replace("&amp;", "&")
        .trim()
        .trim_matches(|c: char| matches!(c, '"' | '\'' | ')' | '(' | ',' | ';'))
        .to_string();
    if cleaned.starts_with("http://") || cleaned.starts_with("https://") {
        cleaned
            .split_whitespace()
            .next()
            .unwrap_or(&cleaned)
            .trim_matches(|c: char| matches!(c, '"' | '\'' | ')' | '(' | ',' | ';'))
            .to_string()
    } else {
        cleaned
    }
}
