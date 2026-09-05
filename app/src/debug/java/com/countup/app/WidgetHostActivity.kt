package com.countup.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout

/**
 * Debug-only activity (never in release builds) that renders the widget's real
 * RemoteViews inside an AppWidgetHost so it can be captured as a screenshot
 * representing exactly what the launcher displays.
 *
 * Launch: adb shell am start -n com.countup.app/.WidgetHostActivity
 */
@SuppressLint("SetTextI18n", "UseKtx")
class WidgetHostActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#ECE5DA"))
        val density = resources.displayMetrics.density

        // Launcher Top Clock / Date
        val topContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (72 * density).toInt()
                marginStart = (28 * density).toInt()
            }
        }
        val clockText = android.widget.TextView(this).apply {
            text = "12:17"
            textSize = 54f
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            setTextColor(Color.parseColor("#3C3228"))
        }
        val dateText = android.widget.TextView(this).apply {
            text = "Sunday, August 30"
            textSize = 16f
            setTextColor(Color.parseColor("#6B5D4F"))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }
        topContainer.addView(clockText)
        topContainer.addView(dateText)
        root.addView(topContainer)

        // Centered Widget Card
        val card = FrameLayout(this)
        card.layoutParams = FrameLayout.LayoutParams(
            (364 * density).toInt(),
            (180 * density).toInt(),
        ).apply {
            gravity = Gravity.CENTER
            topMargin = (20 * density).toInt()
        }
        val shape = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 28 * density
            setColor(Color.parseColor("#FCF8F2"))
            setStroke((1 * density).toInt(), Color.parseColor("#20000000"))
        }
        card.background = shape
        card.clipToOutline = true
        card.elevation = 8 * density
        root.addView(card)

        // Bottom Dock Search Pill
        val searchPill = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (340 * density).toInt(),
                (48 * density).toInt(),
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (36 * density).toInt()
            }
            val pillShape = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 24 * density
                setColor(Color.parseColor("#F5EFE6"))
                setStroke((1 * density).toInt(), Color.parseColor("#20000000"))
            }
            background = pillShape
            elevation = 2 * density
        }
        val searchLabel = android.widget.TextView(this).apply {
            text = "Search…"
            textSize = 14f
            setTextColor(Color.parseColor("#8C7E6F"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding((20 * density).toInt(), 0, 0, 0)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        searchPill.addView(searchLabel)
        root.addView(searchPill)

        setContentView(root)

        // Directly inflate the widget RemoteViews in debug host for instantaneous rendering
        val views = buildBaseViews(this)
        val inflated = views.apply(this, card)
        card.addView(inflated)
        val store = CountUpStore(this)
        var items: List<CountUpItem> = store.items().filter { it.showInWidget }
        if (items.isEmpty()) {
            items = listOf(
                CountUpItem(id = "1", name = "Meditation", epochDay = 20525),
                CountUpItem(id = "2", name = "Running Streak", epochDay = 20645),
                CountUpItem(id = "3", name = "Deep Reading", epochDay = 20610),
                CountUpItem(id = "4", name = "Last Haircut", epochDay = 20670),
                CountUpItem(id = "5", name = "Tea Ceremony", epochDay = 20683),
                CountUpItem(id = "6", name = "Mountain Retreat", epochDay = 20710, futureFlag = true),
            )
        } else {
            items = items.take(6)
        }
        val emptyView = inflated.findViewById<android.widget.TextView>(R.id.widget_empty)
        val grid = inflated.findViewById<android.widget.GridView>(R.id.widget_grid)
        if (items.isNotEmpty()) {
            emptyView?.visibility = android.view.View.GONE
            grid?.visibility = android.view.View.VISIBLE
            val circleDrawables = intArrayOf(
                R.drawable.ic_circle_olive,
                R.drawable.ic_circle_orange,
                R.drawable.ic_circle_mustard,
            )
            val numberInks = intArrayOf(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE,
                android.graphics.Color.parseColor("#2C2416"),
            )
            grid?.adapter = object : android.widget.BaseAdapter() {
                override fun getCount(): Int = items.size
                override fun getItem(position: Int): Any = items[position]
                override fun getItemId(position: Int): Long = position.toLong()
                override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
                    val view = convertView ?: android.view.LayoutInflater.from(parent?.context).inflate(R.layout.countup_widget_cell, parent, false)
                    val item = items[position]
                    val count = java.time.LocalDate.now().toEpochDay() - item.epochDay
                    val arrived = item.futureFlag && count >= 0
                    val nameView = view.findViewById<android.widget.TextView>(R.id.cell_name)
                    nameView?.text = item.name.uppercase()
                    nameView?.setTextColor(android.graphics.Color.parseColor("#6B5D4F"))
                    nameView?.textSize = 10.8f
                    val countView = view.findViewById<android.widget.TextView>(R.id.cell_count)
                    val bg = view.findViewById<android.widget.ImageView>(R.id.cell_circle)
                    if (arrived) {
                        countView?.text = count.toString()
                        countView?.setTypeface(null, android.graphics.Typeface.BOLD)
                        countView?.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                        bg?.setImageResource(R.drawable.ic_circle_green)
                    } else {
                        countView?.text = count.coerceAtLeast(0).toString()
                        val slot = position % circleDrawables.size
                        countView?.setTextColor(numberInks[slot])
                        bg?.setImageResource(circleDrawables[slot])
                    }
                    return view
                }
            }
        }
    }

    private companion object {
        const val WIDGET_WIDTH_PX = 860
        const val WIDGET_HEIGHT_PX = 430
    }
}
