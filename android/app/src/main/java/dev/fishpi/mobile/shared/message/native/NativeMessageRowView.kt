package dev.fishpi.mobile.shared.message.native

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import coil3.ImageLoader

internal class NativeMessageRowView(
    context: Context,
    private val themeProvider: () -> NativeMessageTheme,
    avatarStore: SharedAvatarDrawableStore,
    imageLoaderProvider: () -> ImageLoader,
    onAvatarClick: (String) -> Unit,
    onAvatarLongPress: (String) -> Unit,
) : LinearLayout(context) {

    private val theme: NativeMessageTheme
        get() = themeProvider()

    private val column = LinearLayout(context).apply { orientation = VERTICAL }
    private val authorLine = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val authorName = TextView(context).apply {
        typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
    }
    private val metaHost = LinearLayout(context).apply { orientation = VERTICAL }
    private val reactionHost = LinearLayout(context).apply { orientation = VERTICAL }
    private val repeatHost = LinearLayout(context).apply { orientation = VERTICAL }

    val bubble = NativeChatBubbleView(context, themeProvider)
    val avatar = NativeChatAvatarView(
        context = context,
        store = avatarStore,
        imageLoaderProvider = imageLoaderProvider,
        onAvatarClick = onAvatarClick,
        onAvatarLongPress = onAvatarLongPress,
    )

    val content: LinearLayout
        get() = bubble.content

    init {
        orientation = HORIZONTAL
        clipChildren = false
        clipToPadding = false
        column.clipChildren = false
        column.clipToPadding = false
    }

    private var mine = false

    fun bindLayout(isMine: Boolean, showAvatars: Boolean) {
        mine = isMine
        gravity = if (isMine) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP
        removeAllViews()
        avatar.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            leftMargin = if (isMine) 0 else dp(3f)
            rightMargin = if (isMine) dp(3f) else 0
        }
        if (showAvatars && !isMine) addView(avatar)
        column.removeAllViews()
        column.gravity = if (isMine) Gravity.END else Gravity.START
        column.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            leftMargin = if (isMine) 0 else dp(theme.spacingItemDp)
            rightMargin = if (isMine) dp(theme.spacingItemDp) else 0
        }
        addView(column)
        if (showAvatars && isMine) addView(avatar)
    }

    fun bindAuthor(label: String) {
        authorLine.removeAllViews()
        authorLine.layoutParams = LayoutParams(
            dp(theme.authorLineWidthDp),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(theme.authorLineTopMarginDp)
        }
        authorLine.gravity = Gravity.CENTER_VERTICAL or (if (mine) Gravity.END else Gravity.START)
        authorLine.setPadding(0, 0, 0, 0)
        authorName.text = label
        authorName.setTextColor(theme.authorName)
        authorName.textSize = theme.authorNameTextSizeSp
        authorLine.addView(authorName)
        attachToColumn(authorLine)
    }

    fun attachBubble() {
        attachToColumn(bubble)
    }

    fun bindMeta(view: View?) {
        metaHost.removeAllViews()
        if (view == null) {
            metaHost.visibility = GONE
        } else {
            metaHost.visibility = VISIBLE
            metaHost.addView(view)
        }
        attachToColumn(metaHost)
    }

    fun bindReactions(view: View?) {
        reactionHost.removeAllViews()
        if (view == null) {
            reactionHost.visibility = GONE
        } else {
            reactionHost.visibility = VISIBLE
            reactionHost.addView(view)
        }
        attachToColumn(reactionHost)
    }

    fun bindRepeat(view: View?) {
        repeatHost.removeAllViews()
        if (view == null) {
            repeatHost.visibility = GONE
        } else {
            repeatHost.visibility = VISIBLE
            repeatHost.addView(view)
        }
        attachToColumn(repeatHost)
    }

    private fun attachToColumn(child: View) {
        if (child.parent == null) column.addView(child)
    }

    fun bindAvatar(state: ChatAvatarState) {
        avatar.bind(state)
    }

    fun playSakuraArrival() {
        val targets = mutableListOf<SakuraAnimatable>()
        collectSakuraTargets(this, targets)
        targets.forEach { it.playSakuraArrival() }
    }

    fun stopSakuraArrival() {
        val targets = mutableListOf<SakuraAnimatable>()
        collectSakuraTargets(this, targets)
        targets.forEach { it.stopSakuraArrival() }
    }

    fun recycle() {
        stopSakuraArrival()
        avatar.recycle()
        bubble.apply {
            background = null
            foreground = null
        }
        removeAllViews()
    }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun collectSakuraTargets(view: View, out: MutableList<SakuraAnimatable>) {
        if (view is SakuraAnimatable) out += view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectSakuraTargets(view.getChildAt(index), out)
            }
        }
    }
}
