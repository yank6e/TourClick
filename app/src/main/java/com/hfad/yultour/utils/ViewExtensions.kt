package com.hfad.yultour.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.hfad.yultour.MainActivity

/**
 * Extension-функция для применения отступа снизу с учётом
 * высоты BottomNavigationView и системной навигационной панели
 */
fun Fragment.applyBottomPadding(view: View) {
    view.doOnLayout {
        val bottomNav = (requireActivity() as? MainActivity)?.binding?.bottomNavigation
        if (bottomNav != null && bottomNav.visibility == View.VISIBLE) {
            val systemBarsBottom = ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0

            val bottomPadding = bottomNav.height + systemBarsBottom

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bottomPadding
            )
        }
    }
}

/**
 * Extension-функция для применения отступа снизу с фиксированным значением
 * (более простая версия для статического отступа)
 */
fun Fragment.applyStaticBottomPadding(view: View, paddingDp: Int = 80) {
    val paddingPx = (paddingDp * requireContext().resources.displayMetrics.density).toInt()
    view.setPadding(
        view.paddingLeft,
        view.paddingTop,
        view.paddingRight,
        view.paddingBottom.coerceAtLeast(paddingPx)
    )
}

/**
 * Extension-функция для конвертации dp в px
 */
fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

/**
 * Extension-функция для конвертации px в dp
 */
fun Int.pxToDp(context: android.content.Context): Int {
    return (this / context.resources.displayMetrics.density).toInt()
}