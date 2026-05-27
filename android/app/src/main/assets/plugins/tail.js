// ==FishPiPlugin==
// @name         小尾巴
// @author       只有午安(Kirito)
// @version      1.0.0
// @scenes       chatRoom
// ==/FishPiPlugin==

fishpi.hook('sendMessage', function(text) {
    return text + '\n\n来自 [安卓客户端](https://github.com/KwdeTfpv/fishpi-rust-sdk/releases/latest)';
});
