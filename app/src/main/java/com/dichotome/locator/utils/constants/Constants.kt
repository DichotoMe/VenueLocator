package com.dichotome.locator.utils.constants

import android.content.res.Resources
import android.os.Build


val DISPLAY_WIDTH: Int = Resources.getSystem().displayMetrics.widthPixels
val DISPLAY_HEIGHT: Int = Resources.getSystem().displayMetrics.heightPixels

val FILE_PROVIDER = "com.kpiroom.bubble.fileprovider"

fun android(api: Int, action: () -> Unit) {
    if (Build.VERSION.SDK_INT >= api) action()
}

fun androidIf(api: Int, action: () -> Unit, action2: () -> Unit) {
    if (Build.VERSION.SDK_INT >= api) action() else action2()
}

fun <T> androidResult(api: Int, action: () -> T?): T? =
    if (Build.VERSION.SDK_INT >= api) action() else null
