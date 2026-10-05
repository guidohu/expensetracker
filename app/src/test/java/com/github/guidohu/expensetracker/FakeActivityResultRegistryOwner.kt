package com.github.guidohu.expensetracker

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/**
 * No-op ActivityResultRegistryOwner for screenshot tests — a bare Paparazzi ComposeView isn't a
 * ComponentActivity, so screens using rememberLauncherForActivityResult (e.g. SettingsScreen's
 * notification-permission request) need this supplied explicitly via CompositionLocalProvider.
 */
fun fakeActivityResultRegistryOwner(): ActivityResultRegistryOwner = object : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) = Unit
    }
}
