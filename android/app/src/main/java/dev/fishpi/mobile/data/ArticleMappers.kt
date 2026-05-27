package dev.fishpi.mobile.data

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toArticleSummary(): ArticleSummary {
    return ArticleSummary(
        id = optString("id"),
        title = optString("title"),
        author = optString("author"),
        time = optString("time"),
        tags = optString("tags"),
        preview = optString("preview"),
        commentCount = optLong("commentCount"),
        goodCount = optLong("goodCount"),
        viewCount = optLong("viewCount"),
        sticky = optBoolean("sticky", false),
        perfect = optBoolean("perfect", false),
        avatar = optString("avatar").ifBlank { optString("thumbnail") },
        thumbnail = optString("thumbnail"),
    )
}

internal fun JSONObject.toArticleDetailView(): ArticleDetailView {
    return ArticleDetailView(
        id = optString("id"),
        title = optString("title"),
        author = optString("author"),
        authorUserName = optString("authorUserName").ifBlank { optString("author") },
        avatar = optString("avatar").ifBlank { optString("thumbnail") },
        time = optString("time"),
        tags = optString("tags"),
        markdown = optString("markdown"),
        imageUrls = optJSONArray("imageUrls").toStringList(),
        linkUrls = optJSONArray("linkUrls").toStringList(),
        goodCount = optLong("goodCount"),
        badCount = optLong("badCount"),
        thankCount = optLong("thankCount"),
        collectCount = optLong("collectCount"),
        watchCount = optLong("watchCount"),
        commentCount = optLong("commentCount"),
        viewCount = optLong("viewCount"),
        following = optBoolean("following", false),
        watching = optBoolean("watching", false),
        thanked = optBoolean("thanked", false),
        rewarded = optBoolean("rewarded", false),
        rewardedCount = optLong("rewardedCount"),
        rewardPoint = optLong("rewardPoint"),
        rewardContent = optString("rewardContent"),
        voteState = optInt("voteState"),
        commentNextPage = optInt("commentNextPage", 2),
        commentHasMore = optBoolean("commentHasMore", false),
        comments = optJSONArray("comments").toArticleComments(),
    )
}

internal fun JSONArray?.toArticleComments(): List<ArticleCommentView> {
    return mapObjects { item ->
        ArticleCommentView(
            id = item.optString("id"),
            author = item.optString("author"),
            displayName = item.optString("displayName"),
            userName = item.optString("userName"),
            time = item.optString("time"),
            content = item.optString("content"),
            imageUrls = item.optJSONArray("imageUrls").toStringList(),
            linkUrls = item.optJSONArray("linkUrls").toStringList(),
            goodCount = item.optLong("goodCount"),
            badCount = item.optLong("badCount"),
            thankCount = item.optLong("thankCount"),
            voteState = item.optInt("voteState"),
            thanked = item.optBoolean("thanked", false),
            replyId = item.optString("replyId"),
            avatar = item.optString("avatar"),
        )
    }
}

internal fun JSONObject.toArticleDraftView(): ArticleDraftView {
    return ArticleDraftView(
        id = optString("id"),
        title = optString("title"),
        summary = optString("summary"),
        tags = optString("tags"),
        type = optInt("type"),
        columnId = optString("columnId"),
        columnTitle = optString("columnTitle"),
        chapterNo = optString("chapterNo"),
        updatedTime = optLong("updatedTime"),
    )
}

internal fun JSONObject.toArticleDraftDetailView(): ArticleDraftDetailView {
    return ArticleDraftDetailView(
        id = optString("id"),
        title = optString("title"),
        content = optString("content"),
        thoughtContent = optString("thoughtContent"),
        tags = optString("tags"),
        type = optInt("type"),
        columnId = optString("columnId"),
        columnTitle = optString("columnTitle"),
        chapterNo = optString("chapterNo"),
        rewardContent = optString("rewardContent"),
        rewardPoint = optString("rewardPoint"),
        qnaOfferPoint = optInt("qnaOfferPoint"),
        commentable = optBoolean("commentable", true),
        anonymous = optBoolean("anonymous", false),
        notifyFollowers = optBoolean("notifyFollowers", false),
        showInList = optInt("showInList", 1),
        statement = optInt("statement"),
        updatedTime = optLong("updatedTime"),
    )
}
