// ==FishPiPlugin==
// @name         职业成长
// @author       Codex
// @version      1.0.0
// @scenes       me
// ==/FishPiPlugin==

var CFG_KEY = 'professionGrowthCfg';
var state = {
    page: null,
    me: null,
    names: {},
    selectedId: '',
    detail: null,
    records: [],
    nextCursor: null,
    ranking: null,
    publicProfile: null,
    loading: false
};

function cfg() {
    return storage.get(CFG_KEY, {
        dark: false,
        rankingLimit: 30,
        publicUserName: ''
    });
}

function saveCfg(next) {
    storage.set(CFG_KEY, next);
}

function rememberProfession(p) {
    if (!p) return;
    var id = String(p.professionId || p.id || '');
    var name = p.displayName || p.professionName || p.name || '';
    if (id && name) state.names[id] = name;
}

function pickPrimaryId(me) {
    if (!me) return '';
    if (me.primaryProfessionId) return String(me.primaryProfessionId);
    var progress = Array.isArray(me.progress) ? me.progress : [];
    if (progress.length && progress[0].professionId) return String(progress[0].professionId);
    var available = Array.isArray(me.availableProfessions) ? me.availableProfessions : [];
    if (available.length && available[0].professionId) return String(available[0].professionId);
    return '';
}

function professionOptions() {
    var me = state.me || {};
    var map = Object.assign({}, state.names);
    function add(p) {
        if (!p) return;
        var id = String(p.professionId || p.id || '');
        if (!id) return;
        map[id] = p.displayName || p.name || id;
        rememberProfession(p);
    }
    (me.availableProfessions || []).forEach(add);
    (me.progress || []).forEach(add);
    return Object.keys(map).map(function (id) {
        return { value: id, label: map[id] };
    });
}

function professionName(id) {
    if (state.names[String(id)]) return state.names[String(id)];
    var opts = professionOptions();
    for (var i = 0; i < opts.length; i++) if (opts[i].value === String(id)) return opts[i].label;
    var d = state.detail && state.detail.profession;
    return (d && d.displayName) || String(id || '未选择');
}

function fmtNum(n) {
    if (n === undefined || n === null || n === '') return '-';
    var x = Number(n);
    if (isNaN(x)) return String(n);
    return String(Math.round(x * 100) / 100);
}

function fmtTime(ms) {
    if (!ms) return '-';
    var d = new Date(Number(ms));
    function p(n) { return n < 10 ? '0' + n : String(n); }
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

function currentLevel(detail) {
    detail = detail || state.detail || {};
    var p = detail.profession || {};
    if (p.levelName) return p.levelName;
    if (p.levelDisplayName) return p.levelDisplayName;
    var total = Number(p.totalExperience || 0);
    var levels = (detail.levels || []).slice().sort(function (a, b) {
        return Number(a.requiredExperience || 0) - Number(b.requiredExperience || 0);
    });
    var cur = null;
    levels.forEach(function (lv) {
        if (total >= Number(lv.requiredExperience || 0)) cur = lv;
    });
    return cur ? (cur.displayName || cur.levelName || cur.code || '-') : '-';
}

function nextLevel(detail) {
    detail = detail || state.detail || {};
    var p = detail.profession || {};
    var total = Number(p.totalExperience || 0);
    var levels = (detail.levels || []).slice().sort(function (a, b) {
        return Number(a.requiredExperience || 0) - Number(b.requiredExperience || 0);
    });
    for (var i = 0; i < levels.length; i++) {
        if (total < Number(levels[i].requiredExperience || 0)) return levels[i];
    }
    return null;
}

function renderSkeleton(title) {
    if (!state.page) state.page = ui.page('职业成长');
    state.page.nodes.length = 0;
    state.page.text(title || '职业成长', { style: 'title' });
}

function addTopActions(page) {
    page.actionBar([
        { id: 'refresh', label: '刷新', onClick: function () { loadAll(true); } },
        { id: 'settings', label: '设置', onClick: openSettings },
        { id: 'public', label: '公开资料', onClick: openPublicSearch }
    ]);
}

function render() {
    renderSkeleton('职业成长');
    addTopActions(state.page);

    if (state.loading) state.page.loading('正在读取职业数据...');

    if (!state.me) {
        state.page.empty('还没有加载职业数据。');
        state.page.button('立即加载', function () { loadAll(true); });
        state.page.open();
        return;
    }

    var opts = professionOptions();
    if (opts.length) {
        state.page.select('professionId', '当前职业', opts, { value: state.selectedId });
        state.page.button('切换查看', function (values) {
            var id = values.professionId || state.selectedId;
            if (!id) return ui.toast('请选择职业');
            state.selectedId = id;
            loadProfession(id, true);
        });
    }

    renderOverview(state.page);
    renderDetail(state.page);
    renderRecords(state.page);
    renderRanking(state.page);
    renderWriteActions(state.page);
    state.page.open();
}

function renderOverview(page) {
    var me = state.me || {};
    var primary = me.primaryProfessionId ? professionName(me.primaryProfessionId) : '未设置';
    page.section('我的职业', [
        { type: 'stat', label: '主职业', value: primary, detail: me.primaryProfessionId || '' },
        { type: 'stat', label: '已累计职业', value: fmtNum((me.progress || []).length), detail: '有经验记录的职业数量' },
        { type: 'stat', label: '可选择职业', value: fmtNum((me.availableProfessions || []).length), detail: '当前可设置为主职业' }
    ]);

    var progressRows = (me.progress || []).slice(0, 8).map(function (p) {
        return [professionName(p.professionId), fmtNum(p.totalExperience)];
    });
    if (progressRows.length) page.table(['职业', '累计经验'], progressRows);

    var guideLines = [];
    var guides = me.growthGuides || {};
    Object.keys(guides).forEach(function (id) {
        var actions = Array.isArray(guides[id]) ? guides[id].join(', ') : String(guides[id]);
        guideLines.push('- ' + professionName(id) + ': ' + actions);
    });
    if (guideLines.length) page.markdown('**升级来源**\n\n' + guideLines.join('\n'));
}

function renderDetail(page) {
    if (!state.detail) return;
    var d = state.detail;
    var p = d.profession || {};
    var n = nextLevel(d);
    var rank = d.rarity || {};
    page.section('职业详情', [
        { type: 'stat', label: '等级', value: currentLevel(d), detail: p.levelCode || '' },
        { type: 'stat', label: '累计经验', value: fmtNum(p.totalExperience), detail: n ? ('距 ' + (n.displayName || n.levelName || n.code) + ' 还差 ' + fmtNum(Number(n.requiredExperience || 0) - Number(p.totalExperience || 0))) : '已到当前路线最高等级' },
        { type: 'stat', label: '排行', value: rank.rank ? ('#' + rank.rank) : '-', detail: rank.surpassedPercent !== undefined ? ('超过 ' + fmtNum(rank.surpassedPercent) + '% 用户') : '' }
    ]);

    var levels = (d.levels || []).map(function (lv) {
        return [lv.displayName || lv.levelName || lv.code || '-', fmtNum(lv.requiredExperience)];
    });
    if (levels.length) page.table(['等级路线', '所需经验'], levels);

    var cons = (d.contributions || []).map(function (c) {
        return [c.actionCode || c.sourceLabel || '-', fmtNum(c.eventCount), fmtNum(c.experienceSum)];
    });
    if (cons.length) page.table(['贡献动作', '次数', '经验'], cons);
}

function renderRecords(page) {
    if (!state.records.length) return;
    page.text('最近记录', { style: 'title' });
    page.list(state.records.slice(0, 30).map(function (r, i) {
        var delta = Number(r.experienceDelta || 0);
        return {
            id: String(r.effectId || i),
            title: (delta >= 0 ? '+' : '') + fmtNum(delta) + ' 经验',
            subtitle: (r.sourceLabel || r.actionCode || '职业变化') + ' · ' + fmtTime(r.occurredAt) + ' · ' + fmtNum(r.beforeExperience) + ' -> ' + fmtNum(r.afterExperience)
        };
    }));
    if (state.nextCursor) {
        page.button('加载更多记录', function () { loadMoreRecords(); });
    }
}

function renderRanking(page) {
    if (!state.ranking) return;
    var rk = state.ranking;
    var title = rk.selectedProfession && rk.selectedProfession.displayName ? rk.selectedProfession.displayName + '排行' : '职业排行';
    page.text(title, { style: 'title' });
    var rows = (rk.entries || []).slice(0, cfg().rankingLimit || 30).map(function (e) {
        return [
            '#' + fmtNum(e.rank),
            e.userNickname || e.userName || '-',
            e.levelName || e.professionName || '-',
            e.totalExperience === undefined ? '隐藏' : fmtNum(e.totalExperience)
        ];
    });
    if (rows.length) page.table(['名次', '用户', '等级', '经验'], rows);
}

function renderWriteActions(page) {
    page.text('设置操作', { style: 'title' });
    page.select('privacyPreset', '隐私预设', [
        { value: 'ALL_PUBLIC', label: '全部公开' },
        { value: 'LEVEL_ONLY', label: '仅等级' },
        { value: 'PRIMARY_ONLY', label: '仅主职业' },
        { value: 'SELF_ONLY', label: '仅自己' },
        { value: 'FULLY_HIDDEN', label: '完全隐藏' }
    ], { value: 'LEVEL_ONLY' });
    page.actionBar([
        { id: 'set-primary', label: '设为主职业', enabled: !!state.selectedId, onClick: function () { setPrimary(state.selectedId); } },
        { id: 'save-privacy', label: '保存隐私', onClick: function (values) { setPrivacy(values.privacyPreset || 'LEVEL_ONLY'); } },
        { id: 'skip', label: '结束提示', onClick: skipPrompt }
    ]);
}

function loadAll(forceOpen) {
    state.loading = true;
    if (forceOpen) render();
    fishpi.call('getProfessionMe', {}).then(function (me) {
        if (me && me.ok === false) throw new Error(me.error || '读取职业设置失败');
        state.me = me || {};
        (state.me.availableProfessions || []).forEach(rememberProfession);
        (state.me.progress || []).forEach(rememberProfession);
        state.selectedId = state.selectedId || pickPrimaryId(me);
        if (!state.selectedId) return null;
        return loadProfession(state.selectedId, false);
    }).then(function () {
        state.loading = false;
        render();
    }).catch(function (e) {
        state.loading = false;
        showError(e);
    });
}

function loadProfession(id, forceOpen) {
    var c = cfg();
    state.detail = null;
    state.records = [];
    state.nextCursor = null;
    state.ranking = null;
    if (forceOpen) {
        state.loading = true;
        render();
    }
    return fishpi.call('getProfessionDetail', { professionId: id, dark: !!c.dark })
        .then(function (detail) {
            if (detail && detail.ok === false) throw new Error(detail.error || '读取职业详情失败');
            state.detail = detail;
            rememberProfession(detail && detail.profession);
            return fishpi.call('getProfessionRecords', {
                professionId: id,
                beforeOccurredAt: 0,
                beforeEffectId: '',
                limit: 30
            });
        })
        .then(function (page) {
            if (page && page.ok === false) throw new Error(page.error || '读取职业记录失败');
            state.records = (page && page.records) || [];
            state.nextCursor = page && page.nextCursor;
            return fishpi.call('getProfessionRanking', { professionId: id, dark: !!c.dark });
        })
        .then(function (ranking) {
            if (ranking && ranking.ok === false) throw new Error(ranking.error || '读取职业排行失败');
            state.ranking = ranking;
            rememberProfession(ranking && ranking.selectedProfession);
            (ranking && ranking.professions || []).forEach(rememberProfession);
            state.loading = false;
            if (forceOpen) render();
        })
        .catch(function (e) {
            state.loading = false;
            showError(e);
        });
}

function loadMoreRecords() {
    if (!state.selectedId || !state.nextCursor) return;
    var cur = state.nextCursor;
    fishpi.call('getProfessionRecords', {
        professionId: state.selectedId,
        limit: 30,
        beforeOccurredAt: cur.beforeOccurredAt,
        beforeEffectId: cur.beforeEffectId
    }).then(function (page) {
        if (page && page.ok === false) throw new Error(page.error || '读取职业记录失败');
        state.records = state.records.concat((page && page.records) || []);
        state.nextCursor = page && page.nextCursor;
        render();
    }).catch(showError);
}

function setPrimary(id) {
    if (!id) return ui.toast('请选择职业');
    fishpi.call('setPrimaryProfession', { professionId: id }).then(function (me) {
        if (me && me.ok === false) throw new Error(me.error || '更新主职业失败');
        state.me = me || state.me;
        ui.notify('主职业已更新', 'success');
        loadAll(false);
    }).catch(showError);
}

function setPrivacy(preset) {
    fishpi.call('updateProfessionPrivacy', {
        preset: preset,
        professions: true,
        experience: true,
        contributions: true,
        activity: true,
        ranking: true
    }).then(function (r) {
        if (r && r.ok === false) throw new Error(r.error || '更新职业隐私失败');
        ui.notify('职业隐私已更新', 'success');
    }).catch(showError);
}

function skipPrompt() {
    fishpi.call('skipProfessionPrompt', {}).then(function (r) {
        if (r && r.ok === false) throw new Error(r.error || '结束职业提示失败');
        ui.notify('已结束首次选择提示', 'success');
    }).catch(showError);
}

function openSettings() {
    var c = cfg();
    var dlg = ui.dialog('职业成长设置');
    dlg.switch('dark', '深色样式', { checked: !!c.dark });
    dlg.number('rankingLimit', '榜单数量', { value: Number(c.rankingLimit || 30), min: 1, max: 100 });
    dlg.input('publicUserName', '公开资料用户名', { value: c.publicUserName || '', placeholder: userName || 'adlered' });
    dlg.button('保存', function (v) {
        var next = {
            dark: !!v.dark,
            rankingLimit: Math.max(1, Math.min(100, Number(v.rankingLimit || c.rankingLimit || 30))),
            publicUserName: (v.publicUserName || '').trim()
        };
        saveCfg(next);
        ui.toast('已保存');
        dlg.close();
        loadAll(false);
    });
    dlg.button('关闭', function () { dlg.close(); });
    dlg.open();
}

function openPublicSearch() {
    var c = cfg();
    var dlg = ui.dialog('公开职业资料');
    dlg.input('targetUser', '用户名', { value: c.publicUserName || userName || '', placeholder: 'adlered' });
    dlg.select('position', '展示位置', [
        { value: 'professionPage', label: '职业页' },
        { value: 'homeProfile', label: '主页资料' },
        { value: 'userCard', label: '用户卡片' },
        { value: 'ranking', label: '排行' },
        { value: 'compactMobile', label: '移动紧凑' }
    ], { value: 'professionPage' });
    dlg.button('查询', function (v) {
        var name = (v.targetUser || '').trim();
        if (!name) return ui.toast('请输入用户名');
        saveCfg(Object.assign({}, cfg(), { publicUserName: name }));
        fishpi.call('getUserProfession', {
            userName: name,
            position: v.position || 'professionPage',
            dark: !!cfg().dark
        }).then(function (data) {
            if (data && data.ok === false) throw new Error(data.error || '读取公开职业资料失败');
            showPublicProfile(name, data);
        }).catch(showError);
    });
    dlg.button('关闭', function () { dlg.close(); });
    dlg.open();
}

function showPublicProfile(name, data) {
    var page = ui.page(name + ' 的职业');
    var primary = data && data.primaryProfession;
    if (primary) {
        page.section('主职业', [
            { type: 'stat', label: '职业', value: primary.displayName || primary.professionName || '-', detail: primary.levelCode || '' },
            { type: 'stat', label: '等级', value: primary.levelName || primary.levelDisplayName || '-', detail: primary.totalExperience === undefined ? '经验隐藏' : ('经验 ' + fmtNum(primary.totalExperience)) }
        ]);
    }
    var professions = data && data.professions || [];
    if (professions.length) {
        page.table(['职业', '等级', '经验'], professions.map(function (p) {
            return [p.displayName || p.professionName || '-', p.levelName || p.levelCode || '-', p.totalExperience === undefined ? '隐藏' : fmtNum(p.totalExperience)];
        }));
    }
    var stats = data && data.contributionStats || data && data.majorContributions || [];
    if (stats.length) {
        page.table(['贡献', '次数', '经验'], stats.map(function (s) {
            return [s.actionCode || s.sourceLabel || '-', fmtNum(s.eventCount), fmtNum(s.experienceSum)];
        }));
    }
    var feed = data && data.activityFeed || [];
    if (feed.length) {
        page.list(feed.slice(0, 20).map(function (it, i) {
            return {
                id: String(i),
                title: it.sourceLabel || it.actionCode || '职业动态',
                subtitle: fmtTime(it.occurredAt) + (it.experienceDelta === undefined ? '' : (' · ' + fmtNum(it.experienceDelta) + ' 经验'))
            };
        }));
    }
    page.button('关闭', function () { page.close(); });
    page.open();
}

function showError(e) {
    var msg = e && e.message ? e.message : String(e || '请求失败');
    ui.notify(msg, 'error');
    renderSkeleton('职业成长');
    state.page.error(msg);
    state.page.button('打开设置', openSettings);
    state.page.button('重试', function () { loadAll(true); });
    state.page.open();
}

fishpi.toolbar.register({
    id: 'profession-growth',
    title: '职业成长',
    actions: [
        { id: 'open', label: '成长面板', subtitle: '查看等级、记录和排行' },
        { id: 'public', label: '公开资料', subtitle: '查询用户公开职业信息' },
        { id: 'settings', label: '设置', subtitle: '显示偏好' }
    ]
});

on('toolbarAction', function (e) {
    if (!e || e.entryId !== 'profession-growth') return;
    if (e.actionId === 'settings') return openSettings();
    if (e.actionId === 'public') return openPublicSearch();
    loadAll(true);
});
