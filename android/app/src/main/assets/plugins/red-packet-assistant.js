// ==FishPiPlugin==
// @name         快捷助手
// @author       只有午安(Kirito)
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==

// 启动即初始化默认设置，保证设置面板可见
storage.get('skipTypes', ['rockPaperScissors']);
storage.get('delaySec', 1.0);

fishpi.toolbar.register({
    id: 'quick-actions',
    title: '快捷助手',
    actions: [
        { id: 'bingbing', label: '去打劫', subtitle: '发送去打劫' },
        { id: 'ge', label: '行行好', subtitle: '发送鸽行行好' }
    ]
});

on('toolbarAction', function(action) {
    if (!action || action.entryId !== 'quick-actions') return;
    if (action.actionId === 'bingbing') {
        fishpi.call('sendChatRoomMessage', {content: '冰冰 去打劫'});
    }
    if (action.actionId === 'ge') {
        fishpi.call('sendChatRoomMessage', {content: '鸽 行行好吧'});
    }
});

on('message', function(msg) {
    if (msg.type !== 'redPacket') return;
    var rp = msg.redPacket;
    if (!rp.openable) return;

    // 每次读取最新设置，修改即时生效
    var skip = storage.get('skipTypes', ['rockPaperScissors']);
    var delaySec = Number(storage.get('delaySec', 3.0));
    if (!isFinite(delaySec) || delaySec < 0) delaySec = 3.0;
    if (skip.indexOf(rp.type) >= 0) return;

    // 专属红包：只有 receivers 包含自己才抢
    if (rp.type === 'specify') {
        var recv = rp.receivers || [];
        if (userName && recv.indexOf(userName) < 0) return;
    }

    setTimeout(function() {
        fishpi.call('openRedPacket', {messageId: msg.oId, gesture: -1}).then(function(r) {
            if (r.ok === false) { log('openRedPacket error: ' + r.error); return; }
            var me = r.who.length > 0 ? r.who[r.who.length - 1] : null;
            var got = me ? me.userMoney : 0;
            ui.toast('[红包助手] 🧧 抢到' + got + '积分');
        });
    }, Math.round(delaySec * 1000));
});
