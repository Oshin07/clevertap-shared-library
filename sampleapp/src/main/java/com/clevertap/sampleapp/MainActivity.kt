package com.clevertap.sampleapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.sampleapp.edgecases.EdgeCaseTestActivity
import com.clevertap.sampleapp.scenario.NoCleverTapScenarioActivity
import com.clevertap.sampleapp.scenario.WithCleverTapScenarioActivity
import com.clevertap.sharedeventlib.SharedEventTracker

class MainActivity : AppCompatActivity() {

    data class TestScreen(val title: String, val description: String, val intent: () -> Intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Track the home screen view through the shared library
        SharedEventTracker.trackHomeScreenViewed()

        val screens = listOf(
            TestScreen(
                "Scenario 1 — No Host CleverTap",
                "App uses only the shared library. No app-level CleverTap exists. " +
                        "Events should appear only in Common Account C.",
                { Intent(this, NoCleverTapScenarioActivity::class.java) }
            ),
            TestScreen(
                "Scenario 2 — Host App Has CleverTap",
                "App has its own CleverTap (Account A) AND uses the shared library (Account C). " +
                        "Verify events go to the correct account and neither instance interferes.",
                { Intent(this, WithCleverTapScenarioActivity::class.java) }
            ),
            TestScreen(
                "Edge Case Test Suite (25 cases)",
                "Runs through all edge cases: multi-init, pre-init calls, threading, " +
                        "offline behavior, rapid events, lifecycle, and encapsulation verification.",
                { Intent(this, EdgeCaseTestActivity::class.java) }
            )
        )

        // Programmatic layout — no XML required for the launcher screen
        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = ScreenAdapter(screens)
            setPadding(32, 32, 32, 32)
        }

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(
                TextView(this@MainActivity).apply {
                    text = "CleverTap Shared Library — Test App"
                    textSize = 20f
                    setPadding(32, 48, 32, 8)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            )
            addView(
                TextView(this@MainActivity).apply {
                    text = "sourceApp: ${SharedEventTracker.getSourceApp()}\n" +
                            "initialized: ${SharedEventTracker.isInitialized()}"
                    textSize = 13f
                    setPadding(32, 0, 32, 24)
                    setTextColor(0xFF666666.toInt())
                }
            )
            addView(rv, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            ))
        }

        setContentView(root)
    }

    private inner class ScreenAdapter(private val screens: List<TestScreen>) :
        RecyclerView.Adapter<ScreenAdapter.VH>() {

        inner class VH(val card: CardView) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val card = CardView(parent.context).apply {
                radius = 16f
                cardElevation = 4f
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 24) }
            }
            val content = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(40, 32, 40, 32)
            }
            content.addView(TextView(parent.context).apply { tag = "title"; textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD) })
            content.addView(TextView(parent.context).apply { tag = "desc";  textSize = 13f; setTextColor(0xFF555555.toInt()); setPadding(0, 8, 0, 0) })
            card.addView(content)
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = screens[position]
            (holder.card.getChildAt(0) as android.widget.LinearLayout).let { ll ->
                (ll.findViewWithTag<TextView>("title")).text = s.title
                (ll.findViewWithTag<TextView>("desc")).text  = s.description
            }
            holder.card.setOnClickListener { startActivity(s.intent()) }
        }

        override fun getItemCount() = screens.size
    }
}
