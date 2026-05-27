package dev.fishpi.mobile.feature.home.model

import dev.fishpi.mobile.data.HomeWorkSettings

internal data class HomeWorkSettingsDraft(
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val weekendMode: String = HomeWorkSettings.WEEKEND_DOUBLE,
    val customRestDays: Set<Int> = setOf(7),
) {
    val isValid: Boolean
        get() = startTime.isHomeTimeText() && endTime.isHomeTimeText() &&
            homeTimeMinutes(startTime) < homeTimeMinutes(endTime)

    fun toSettings(): HomeWorkSettings =
        HomeWorkSettings(
            startTime = startTime,
            endTime = endTime,
            weekendMode = weekendMode,
            customRestDays = customRestDays.ifEmpty { setOf(7) },
        )
}

internal fun HomeWorkSettings.toDraft(): HomeWorkSettingsDraft =
    HomeWorkSettingsDraft(
        startTime = startTime,
        endTime = endTime,
        weekendMode = weekendMode,
        customRestDays = customRestDays.ifEmpty { setOf(7) },
    )

internal fun String.isHomeTimeText(): Boolean =
    Regex("""^([01]\d|2[0-3]):[0-5]\d$""").matches(this)

internal fun homeTimeMinutes(value: String): Int {
    if (!value.isHomeTimeText()) return 0
    val parts = value.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

internal fun String.toHomeTimeParts(): Pair<Int, Int> {
    if (!isHomeTimeText()) return 9 to 0
    val parts = split(":")
    return parts[0].toInt() to parts[1].toInt()
}
