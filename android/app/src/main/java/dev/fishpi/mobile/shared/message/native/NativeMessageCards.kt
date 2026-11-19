package dev.fishpi.mobile.shared.message.native

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.setPadding
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import dev.fishpi.mobile.R
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.feature.redpacket.RedPacketGestureOptions
import dev.fishpi.mobile.feature.redpacket.toRedPacketUiModel
import kotlin.math.roundToInt

private val Context.cardDensity: Float
    get() = resources.displayMetrics.density

private fun Context.cardPx(value: Int): Int = (value * cardDensity).toInt()

private fun Context.cardThemePx(value: Float): Int = (value * cardDensity).toInt()

private fun Context.cardRoundPx(value: Float): Int = (value * cardDensity).roundToInt()

private fun Context.cardRect(color: Int, radiusDp: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusDp * cardDensity
    }

private fun Int.withAlpha(alpha: Int): Int =
    (alpha.coerceIn(0, 255) shl 24) or (this and 0x00FFFFFF)

internal class MusicCardView(
    private val context: Context,
    private val themeProvider: () -> NativeMessageTheme,
    private val imageLoaderProvider: () -> ImageLoader,
    private val onOpenLink: (String) -> Unit,
    private val attachLongPress: (View) -> Unit,
) {
    private val theme: NativeMessageTheme
        get() = themeProvider()

    fun build(card: NativeMusicCard): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            attachLongPress(this)
            background = context.cardRect(theme.quoteBackground.withAlpha(150), theme.radiusFieldDp).apply {
                setStroke(context.cardRoundPx(theme.borderWidthDp).coerceAtLeast(0), theme.bubbleBorder)
            }
            isClickable = card.sourceUrl.isNotBlank()
            isFocusable = card.sourceUrl.isNotBlank()
            if (card.sourceUrl.isNotBlank()) {
                setOnClickListener { onOpenLink(card.sourceUrl) }
            }
            setPadding(controlPx(), controlPx(), controlPx(), controlPx())
            layoutParams = LinearLayout.LayoutParams(context.cardPx(236), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.cardThemePx(theme.spacingTightDp)
                bottomMargin = context.cardPx(6)
            }

            addView(cover(card.coverUrl), LinearLayout.LayoutParams(context.cardPx(48), context.cardPx(48)).apply {
                rightMargin = itemPx()
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = card.title.ifBlank { "音乐" }
                    setTextColor(theme.bubbleText)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                addView(TextView(context).apply {
                    text = card.from.ifBlank { "音乐分享" }
                    setTextColor(theme.weakText)
                    textSize = 11f
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = context.cardPx(5)
                    }
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    private fun cover(url: String): FrameLayout =
        FrameLayout(context).apply {
            background = context.cardRect(theme.clientBackground, theme.radiusFieldDp)
            clipToOutline = true
            val placeholder = TextView(context).apply {
                text = "♪"
                setTextColor(theme.accent)
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(placeholder, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            if (url.isBlank()) return@apply
            val image = MessageMediaImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                visibility = View.INVISIBLE
                tag = url
            }
            addView(image, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(context.cardPx(48), context.cardPx(48)))
                .precision(Precision.EXACT)
                .target(
                    onSuccess = { result ->
                        if (image.tag == url) {
                            image.setMediaDrawable(result.asDrawable(context.resources))
                            image.visibility = View.VISIBLE
                            placeholder.visibility = View.GONE
                        }
                    },
                    onError = {
                        if (image.tag == url) {
                            image.visibility = View.INVISIBLE
                            placeholder.visibility = View.VISIBLE
                        }
                    },
                )
                .build()
            imageLoaderProvider().enqueue(request)
        }

    private fun controlPx(): Int = context.cardRoundPx(theme.spacingControlDp)

    private fun itemPx(): Int = context.cardRoundPx(theme.spacingItemDp)
}

internal class RedPacketCardView(
    private val context: Context,
    private val themeProvider: () -> NativeMessageTheme,
    private val cardBackground: (Int, Float) -> GradientDrawable,
    private val attachLongPress: (View) -> Unit,
    private val onCardClick: (ChatRoomMessage) -> Unit,
    private val onGestureClick: (ChatRoomMessage, Int) -> Unit,
    private val dismissPopup: () -> Unit,
) {
    private val theme: NativeMessageTheme
        get() = themeProvider()

    fun build(message: ChatRoomMessage, isMine: Boolean): View {
        val packet = message.redPacket?.toRedPacketUiModel()
        val showGestureActions = packet?.needGesture == true && packet.openable && !packet.finished && !isMine
        val action = when {
            packet == null -> ""
            packet.finished -> "已抢完"
            isMine -> "查看详情"
            !packet.openable -> "不可领取"
            showGestureActions -> "猜拳领取"
            packet.needGesture -> "猜拳领取"
            else -> "拆红包"
        }
        val amount = packet
            ?.money
            ?.takeIf { it > 0 }
            ?.let { "$it 积分" }
            .orEmpty()
        val progress = packet?.let { "已领 ${it.got}/${it.count}" }.orEmpty()

        val card = LinearLayout(context).apply {
            attachLongPress(this)
            orientation = LinearLayout.VERTICAL
            background = cardBackground(NativeRedPacketBackground, theme.radiusBoxDp)
            setPadding(controlPx() + context.cardPx(3), controlPx() + context.cardPx(1), controlPx() + context.cardPx(3), controlPx())
            layoutParams = LinearLayout.LayoutParams(context.cardPx(220), ViewGroup.LayoutParams.WRAP_CONTENT)
            alpha = if (message.redPacket?.openable == true || isMine) 1f else 0.72f
            if (!showGestureActions) {
                setOnClickListener {
                    dismissPopup()
                    onCardClick(message)
                }
            }

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = "🧧 ${packet?.typeName ?: "红包"}"
                    setTextColor(NativeRedPacketText)
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (amount.isNotBlank()) {
                    addView(TextView(context).apply {
                        text = amount
                        setTextColor(NativeRedPacketBackground)
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        includeFontPadding = false
                        background = cardBackground(NativeRedPacketText, theme.radiusSelectorDp)
                        setPadding(controlPx(), context.cardPx(3), controlPx(), context.cardPx(3))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            marginStart = context.cardPx(8)
                        }
                    })
                }
            })

            packet?.message?.takeIf { it.isNotBlank() }?.let { packetMessage ->
                addView(TextView(context).apply {
                    text = packetMessage
                    setTextColor(NativeRedPacketText.withAlpha(232))
                    textSize = 13f
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = context.cardPx(7)
                    }
                })
            }

            if (progress.isNotBlank() || action.isNotBlank()) {
                addView(TextView(context).apply {
                    text = listOf(progress, action).filter(String::isNotBlank).joinToString(" · ")
                    setTextColor(NativeRedPacketText.withAlpha(210))
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = context.cardPx(9)
                    }
                })
            }
        }

        if (!showGestureActions) return card

        return FrameLayout(context).apply {
            attachLongPress(this)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addView(card, FrameLayout.LayoutParams(context.cardPx(220), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            })
            addView(gestureRail(message), FrameLayout.LayoutParams(context.cardPx(74), context.cardPx(116)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                leftMargin = context.cardPx(196)
            })
        }
    }

    private fun gestureRail(message: ChatRoomMessage): FrameLayout =
        FrameLayout(context).apply {
            RedPacketGestureOptions.forEachIndexed { index, option ->
                addView(gestureButton(message, option.value, option.label), FrameLayout.LayoutParams(context.cardPx(42), context.cardPx(42)).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = when (index) {
                        0 -> context.cardPx(1)
                        1 -> context.cardPx(37)
                        else -> context.cardPx(73)
                    }
                    marginEnd = if (index == 1) context.cardPx(0) else context.cardPx(26)
                })
            }
        }

    private fun gestureButton(message: ChatRoomMessage, value: Int, label: String): ImageButton =
        ImageButton(context).apply {
            setImageResource(gestureDrawable(value))
            background = ColorDrawable(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0)
            contentDescription = label
            setOnClickListener {
                dismissPopup()
                onGestureClick(message, value)
            }
        }

    @DrawableRes
    private fun gestureDrawable(value: Int): Int =
        when (value) {
            0 -> R.drawable.redpacket_gesture_rock
            1 -> R.drawable.redpacket_gesture_scissors
            else -> R.drawable.redpacket_gesture_paper
        }

    private fun controlPx(): Int = context.cardRoundPx(theme.spacingControlDp)
}
