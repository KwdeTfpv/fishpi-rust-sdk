package dev.fishpi.mobile.shared.message

internal data class ReactionOption(
    val value: String,
    val emoji: String,
)

internal val QuickReactionOptions = listOf(
    ReactionOption("thumbsup", "👍"),
    ReactionOption("plus", "➕1️⃣"),
    ReactionOption("thumbsdown", "👎"),
    ReactionOption("check", "✅"),
    ReactionOption("cross", "❌"),
    ReactionOption("star", "⭐️"),
    ReactionOption("heart", "❤️"),
    ReactionOption("fire", "🔥"),
    ReactionOption("party", "🎉"),
    ReactionOption("laugh", "😂"),
    ReactionOption("wow", "😮"),
    ReactionOption("clap", "👏"),
    ReactionOption("hundred", "💯"),
    ReactionOption("rocket", "🚀"),
    ReactionOption("salute", "🖖"),
    ReactionOption("handshake", "🤝"),
    ReactionOption("raisedhands", "🙌"),
    ReactionOption("mindblown", "🤯"),
    ReactionOption("thinking", "🤔"),
    ReactionOption("eyes", "👀"),
    ReactionOption("cry", "😢"),
    ReactionOption("angry", "😡"),
    ReactionOption("pray", "🙏"),
    ReactionOption("brokenheart", "💔"),
    ReactionOption("heartonfire", "❤️🔥"),
    ReactionOption("skull", "💀"),
    ReactionOption("clown", "🤡"),
    ReactionOption("poop", "💩"),
)

