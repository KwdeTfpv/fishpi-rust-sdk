package dev.fishpi.mobile.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * 全局动画令牌 + 点击反馈的唯一入口。
 */
object FishPiMotion {
    const val durationFast = 120
    const val durationMedium = 180
    const val durationSlow = 240

    // 缓动曲线。
    val easingStandard: Easing = FastOutSlowInEasing
    val easingDecelerate: Easing = LinearOutSlowInEasing // 入场：快进慢出
    val easingAccelerate: Easing = FastOutLinearInEasing // 出场：慢进快出
    val easingPress: Easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)

    // 便捷 tween spec。
    fun <T> tweenFast(easing: Easing = easingStandard): FiniteAnimationSpec<T> =
        tween(durationMillis = durationFast, easing = easing)

    fun <T> tweenMedium(easing: Easing = easingStandard): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMedium, easing = easing)

    fun <T> tweenSlow(easing: Easing = easingStandard): FiniteAnimationSpec<T> =
        tween(durationMillis = durationSlow, easing = easing)

    // ---- 浮层 / BottomSheet 进出场（供 AnimatedVisibility 使用） ----

    /** 通用浮层进场：淡入 + 轻微放大（0.98→1）。 */
    val enterOverlay: EnterTransition =
        fadeIn(tweenMedium(easingDecelerate)) +
            scaleIn(initialScale = 0.98f, animationSpec = tweenMedium(easingDecelerate))

    /** 通用浮层出场：淡出 + 轻微缩小。 */
    val exitOverlay: ExitTransition =
        fadeOut(tweenFast(easingAccelerate)) +
            scaleOut(targetScale = 0.98f, animationSpec = tweenFast(easingAccelerate))

    /** 底部面板进场：底部滑入 + 淡入。 */
    val enterBottomSheet: EnterTransition =
        slideInVertically(tweenMedium(easingDecelerate)) { it } +
            fadeIn(tweenMedium(easingDecelerate))

    /** 底部面板出场：向底部滑出 + 淡出。 */
    val exitBottomSheet: ExitTransition =
        slideOutVertically(tweenFast(easingAccelerate)) { it } +
            fadeOut(tweenFast(easingAccelerate))

    /** 详情页推入（右侧滑入 + 淡入） */
    val enterPush: EnterTransition =
        slideInHorizontally(tweenMedium(easingDecelerate)) { it } +
            fadeIn(tweenMedium(easingDecelerate))

    /** 详情页推出（向右滑出 + 淡出）。 */
    val exitPush: ExitTransition =
        slideOutHorizontally(tweenFast(easingAccelerate)) { it } +
            fadeOut(tweenFast(easingAccelerate))

    /**
     * 列表 ↔ 详情二选一切换，供 `AnimatedContent.transitionSpec` 使用。
     *
     * [enteringDetail] = true 表示正在进入详情（列表→详情）：新页（详情）从右滑入，
     *
     * 用 zIndex 让详情始终压在列表之上，避免退场时详情被列表盖住。
     */
    fun pushTransform(enteringDetail: Boolean): ContentTransform =
        if (enteringDetail) {
            (slideInHorizontally(tweenMedium(easingDecelerate)) { it } + fadeIn(tweenMedium(easingDecelerate)))
                .togetherWith(
                    slideOutHorizontally(tweenMedium(easingAccelerate)) { -it / 5 } +
                        fadeOut(tweenFast(easingAccelerate)),
                )
        } else {
            (slideInHorizontally(tweenMedium(easingDecelerate)) { -it / 5 } + fadeIn(tweenMedium(easingDecelerate)))
                .togetherWith(
                    slideOutHorizontally(tweenMedium(easingAccelerate)) { it } +
                        fadeOut(tweenFast(easingAccelerate)),
                )
        }
}

/**
 * 点击反馈的唯一来源：涟漪 + 轻按压缩放。
 *
 * 形态约定：
 * - 全宽行 / 卡片：`pressScale = 1f`（只涟漪不缩放，防布局跳动）。
 * - 小圆 / 图标按钮：`rippleBounded = false`。
 * - chip / pill：默认 0.97f + bounded ripple。
 */
fun Modifier.fishClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    pressScale: Float = 0.97f,
    rippleBounded: Boolean = true,
    rippleColor: Color = Color.Unspecified,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && pressScale != 1f) pressScale else 1f,
        animationSpec = FishPiMotion.tweenFast(FishPiMotion.easingPress),
        label = "fishClickableScale",
    )
    this
        .then(
            if (pressScale != 1f) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                Modifier
            },
        )
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = rippleBounded, color = rippleColor),
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}

/** 带长按的变体（供 article 消息块等 combinedClickable 场景用）。 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.fishCombinedClickable(
    enabled: Boolean = true,
    role: Role? = null,
    pressScale: Float = 1f,
    rippleBounded: Boolean = true,
    rippleColor: Color = Color.Unspecified,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && pressScale != 1f) pressScale else 1f,
        animationSpec = FishPiMotion.tweenFast(FishPiMotion.easingPress),
        label = "fishCombinedClickableScale",
    )
    this
        .then(
            if (pressScale != 1f) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                Modifier
            },
        )
        .combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = rippleBounded, color = rippleColor),
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}
