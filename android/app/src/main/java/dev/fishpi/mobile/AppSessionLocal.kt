package dev.fishpi.mobile

import androidx.compose.runtime.compositionLocalOf

internal val LocalAppSession = compositionLocalOf<AppSession> {
    error("LocalAppSession is not provided")
}

