package dev.fishpi.mobile

import dev.fishpi.mobile.data.FishPiUser

internal data class AppSession(
    val apiKey: String,
    val user: FishPiUser,
    val v3Notice: String = "",
)
