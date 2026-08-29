package com.countup.app

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
class WidgetHostActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, CountUpWidgetReceiver::class.java)
        val host = AppWidgetHost(this, HOST_ID)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#E0E0E0"))
        val card = FrameLayout(this)
        card.layoutParams = FrameLayout.LayoutParams(WIDGET_WIDTH_PX, WIDGET_HEIGHT_PX).apply {
            gravity = Gravity.CENTER
        }
        root.addView(card)
        setContentView(root)

        // Directly inflate the widget RemoteViews in debug host for instantaneous rendering
        val views = buildBaseViews(this)
        val inflated = views.apply(this, card)
        card.addView(inflated)

        val store = CountUpStore(this)
        var items = store.items().filter { it.showInWidget }
        if (items.isEmpty()) {
            items = listOf(
                CountUpItem(id = "1", name = "Meditation", epochDay = 20525),
                CountUpItem(id = "2", name = "Water Bonsai", epochDay = 20635),
                CountUpItem(id = "3", name = "Reading Book", epochDay = 20605),
                CountUpItem(id = "4", name = "Tokyo Trip", epochDay = 20660, futureFlag = true),
                CountUpItem(id = "5", name = "Yoga Stretch", epochDay = 20662),
                CountUpItem(id = "6", name = "Clean Desk", epochDay = 20577),
            )
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
        const val HOST_ID = 2048
        const val WIDGET_WIDTH_PX = 860
        const val WIDGET_HEIGHT_PX = 430
    }
}
