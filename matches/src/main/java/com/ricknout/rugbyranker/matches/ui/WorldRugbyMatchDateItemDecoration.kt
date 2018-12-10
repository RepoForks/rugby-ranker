package com.ricknout.rugbyranker.matches.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.graphics.withTranslation
import androidx.core.text.inSpans
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.RecyclerView
import com.ricknout.rugbyranker.common.util.DateUtils
import com.ricknout.rugbyranker.matches.vo.WorldRugbyMatch
import com.ricknout.rugbyranker.matches.R

class WorldRugbyMatchDateItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    var matches: List<WorldRugbyMatch> = emptyList()
        set(value) {
            field = value
            matchHeaders = indexMatchHeaders().map {
                it.first to createHeader(it.second)
            }.toMap()
        }

    private var matchHeaders = indexMatchHeaders().map {
        it.first to createHeader(it.second)
    }.toMap()

    private val paint: TextPaint
    private val width: Int
    private val paddingTop: Int
    private val dayMonthTextSize: Int
    private val yearTextSize: Int

    init {
        val attrs = context.obtainStyledAttributes(
                R.style.RugbyRankerWorldRugbyMatchDateHeader,
                R.styleable.WorldRugbyMatchDateHeader
        )
        paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.WorldRugbyMatchDateHeader_android_textColor)
            textSize = attrs.getDimensionOrThrow(R.styleable.WorldRugbyMatchDateHeader_dayMonthTextSize)
            try {
                typeface = ResourcesCompat.getFont(
                        context,
                        attrs.getResourceIdOrThrow(R.styleable.WorldRugbyMatchDateHeader_android_fontFamily)
                )
            } catch (_: Exception) {
            }
        }
        width = attrs.getDimensionPixelSizeOrThrow(R.styleable.WorldRugbyMatchDateHeader_android_width)
        paddingTop = attrs.getDimensionPixelSizeOrThrow(R.styleable.WorldRugbyMatchDateHeader_android_paddingTop)
        dayMonthTextSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.WorldRugbyMatchDateHeader_dayMonthTextSize)
        yearTextSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.WorldRugbyMatchDateHeader_yearTextSize)
        attrs.recycle()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (matchHeaders.isEmpty() || parent.isEmpty()) return

        var earliestFoundHeaderPos = -1
        var prevHeaderTop = Int.MAX_VALUE

        for (i in parent.childCount - 1 downTo 0) {
            val view = parent.getChildAt(i) ?: continue
            val viewTop = view.top + view.translationY.toInt()
            if (view.bottom > 0 && viewTop < parent.height) {
                val position = parent.getChildAdapterPosition(view)
                matchHeaders[position]?.let { layout ->
                    paint.alpha = (view.alpha * 255).toInt()
                    val top = (viewTop + paddingTop)
                            .coerceAtLeast(paddingTop)
                            .coerceAtMost(prevHeaderTop - layout.height)
                    c.withTranslation(y = top.toFloat()) {
                        layout.draw(c)
                    }
                    earliestFoundHeaderPos = position
                    prevHeaderTop = viewTop
                }
            }
        }

        if (earliestFoundHeaderPos < 0) {
            earliestFoundHeaderPos = parent.getChildAdapterPosition(parent[0]) + 1
        }

        for (headerPos in matchHeaders.keys.reversed()) {
            if (headerPos < earliestFoundHeaderPos) {
                matchHeaders[headerPos]?.let {
                    val top = (prevHeaderTop - it.height).coerceAtMost(paddingTop)
                    c.withTranslation(y = top.toFloat()) {
                        it.draw(c)
                    }
                }
                break
            }
        }
    }

    private fun indexMatchHeaders() = matches
            .mapIndexed { index, match -> index to match.timeMillis }
            .distinctBy { DateUtils.getDayMonthYear(it.second) }

    private fun createHeader(millis: Long): StaticLayout {
        val text = SpannableStringBuilder().apply {
            inSpans(AbsoluteSizeSpan(dayMonthTextSize)) {
                val dayMonth = DateUtils.getDate(DateUtils.DATE_FORMAT_D_MMM, millis)
                append(dayMonth)
            }
        }.apply {
            append(System.lineSeparator())
            inSpans(AbsoluteSizeSpan(yearTextSize)) {
                val year = DateUtils.getDate(DateUtils.DATE_FORMAT_YYYY, millis)
                append(year)
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .build()
        } else {
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
        }
    }
}
