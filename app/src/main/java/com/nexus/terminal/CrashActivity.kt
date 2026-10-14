package com.nexus.terminal

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = intent.getStringExtra("trace") ?: "no trace"
        t.setTextIsSelectable(true)
        t.textSize = 11f
        t.setPadding(24, 64, 24, 24)
        setContentView(ScrollView(this).apply { addView(t) })
    }
}
