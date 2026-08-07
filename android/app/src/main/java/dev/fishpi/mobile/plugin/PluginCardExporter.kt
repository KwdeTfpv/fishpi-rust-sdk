package dev.fishpi.mobile.plugin

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import dev.fishpi.mobile.chatui.ChatMarkdownRenderCache
import dev.fishpi.mobile.chatui.MarkwonContentRenderer
import dev.fishpi.mobile.chatui.MarkwonContentStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

internal object PluginCardExporter {

    private const val CARD_WIDTH_PX = 1080
    private const val PADDING_PX = 56
    private const val AVATAR_PX = 84
    private const val MAX_HEIGHT_PX = 12000

    private const val BG = 0xFFFFFFFF.toInt()
    private const val TITLE_COLOR = 0xFF1A1A1A.toInt()
    private const val BODY_COLOR = 0xFF2B2B2B.toInt()
    private const val AUTHOR_COLOR = 0xFF1A1A1A.toInt()
    private const val FOOTER_COLOR = 0xFF9AA0A6.toInt()
    private const val DIVIDER_COLOR = 0xFFECECEC.toInt()
    private const val ACCENT = 0xFF3B82F6.toInt()
    private const val CODE_BG = 0xFFF3F4F6.toInt()
    private const val AVATAR_PLACEHOLDER_BG = 0xFFE3EAF5.toInt()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun export(
        appContext: Context,
        title: String,
        author: String,
        avatarUrl: String,
        source: String,
        markdown: String,
        footer: String,
    ) {
        scope.launch {
            toast(appContext, "正在生成长图…")
            runCatching {
                val cache = ChatMarkdownRenderCache(maxEntries = 8, maxChars = 200_000)
                val renderer = MarkwonContentRenderer(
                    context = appContext,
                    style = MarkwonContentStyle(
                        textColor = BODY_COLOR,
                        weakTextColor = FOOTER_COLOR,
                        accentColor = ACCENT,
                        codeBackgroundColor = CODE_BG,
                        textSizeSp = 15f,
                        lineSpacingMultiplier = 1.3f,
                    ),
                    cache = cache,
                    scope = scope,
                    onLinkClick = {},
                    onMentionClick = {},
                )
                val bodySpanned = renderer.renderToSpanned(markdown)
                val avatarBitmap = loadAvatar(appContext, avatarUrl)

                val card = buildCard(appContext, title, author, source, avatarBitmap, bodySpanned, footer)
                val bitmap = withContext(Dispatchers.Main) { card.toBitmap() }

                withContext(Dispatchers.IO) { saveBitmapToPictures(appContext, bitmap) }
                bitmap.recycle()
                toast(appContext, "长图已存到相册")
            }.onFailure {
                toast(appContext, "生成长图失败：${it.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun toast(context: Context, text: String) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun loadAvatar(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return runCatching {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)   // 要画进 Canvas，禁硬件位图
                .build()
            val result = loader.execute(request)
            result.image?.asDrawable(context.resources)?.toBitmap()
        }.getOrNull()
    }

    private fun buildCard(
        context: Context,
        title: String,
        author: String,
        source: String,
        avatar: Bitmap?,
        body: CharSequence,
        footer: String,
    ): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(PADDING_PX, PADDING_PX, PADDING_PX, PADDING_PX)
            layoutParams = ViewGroup.LayoutParams(CARD_WIDTH_PX, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // 顶部：头像 + 作者 / 来源
        if (author.isNotBlank() || avatar != null || source.isNotBlank()) {
            root.addView(buildHeader(context, author, source, avatar))
            root.addView(spacer(context, 24))
        }

        if (title.isNotBlank()) {
            root.addView(TextView(context).apply {
                text = title
                setTextColor(TITLE_COLOR)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
                setLineSpacing(0f, 1.2f)
            })
            root.addView(spacer(context, 20))
            root.addView(divider(context))
            root.addView(spacer(context, 20))
        }

        root.addView(TextView(context).apply {
            text = body
            setTextColor(BODY_COLOR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            includeFontPadding = false
            setLineSpacing(0f, 1.3f)
        })

        if (footer.isNotBlank()) {
            root.addView(spacer(context, 24))
            root.addView(divider(context))
            root.addView(spacer(context, 14))
            root.addView(TextView(context).apply {
                text = footer
                setTextColor(FOOTER_COLOR)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                includeFontPadding = false
            })
        }
        return root
    }

    private fun buildHeader(context: Context, author: String, source: String, avatar: Bitmap?): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(AVATAR_PX, AVATAR_PX)
                setImageBitmap(circleAvatar(author, avatar))
            })
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(20, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = author.ifBlank { "鱼友" }
                    setTextColor(AUTHOR_COLOR)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = Typeface.DEFAULT_BOLD
                    includeFontPadding = false
                })
                if (source.isNotBlank()) {
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6)
                    })
                    addView(TextView(context).apply {
                        text = source
                        setTextColor(FOOTER_COLOR)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        includeFontPadding = false
                    })
                }
            })
        }
    }

    private fun circleAvatar(author: String, avatar: Bitmap?): Bitmap {
        val size = AVATAR_PX
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radius = size / 2f
        if (avatar != null) {
            val src = if (avatar.width != size || avatar.height != size) {
                avatar.scale(size)
            } else {
                avatar
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(radius, radius, radius, paint)
        } else {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AVATAR_PLACEHOLDER_BG }
            canvas.drawCircle(radius, radius, radius, bgPaint)
            val letter = author.trim().take(1).uppercase().ifBlank { "鱼" }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ACCENT
                textSize = size * 0.44f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val fm = textPaint.fontMetrics
            val baseline = radius - (fm.ascent + fm.descent) / 2f
            canvas.drawText(letter, radius, baseline, textPaint)
        }
        return out
    }

    private fun Bitmap.scale(size: Int): Bitmap {
        val minSide = min(width, height)
        val cropX = (width - minSide) / 2
        val cropY = (height - minSide) / 2
        val square = Bitmap.createBitmap(this, cropX, cropY, minSide, minSide)
        return Bitmap.createScaledBitmap(square, size, size, true)
    }

    private fun spacer(context: Context, heightPx: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
    }

    private fun divider(context: Context) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
        setBackgroundColor(DIVIDER_COLOR)
    }

    private fun View.toBitmap(): Bitmap {
        measure(
            View.MeasureSpec.makeMeasureSpec(CARD_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val height = measuredHeight.coerceIn(1, MAX_HEIGHT_PX)
        layout(0, 0, CARD_WIDTH_PX, height)
        val bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BG)
        draw(canvas)
        return bitmap
    }

    private fun saveBitmapToPictures(context: Context, bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "fishpi-summary-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FishPi")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        ) { "无法创建图片文件" }
        try {
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "无法写入图片文件" }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
