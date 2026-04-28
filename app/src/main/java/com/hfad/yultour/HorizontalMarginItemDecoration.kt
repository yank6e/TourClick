package com.hfad.yultour

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalMarginItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val margin = context.resources.getDimensionPixelSize(R.dimen.horizontal_margin)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)

        if (position == 0) {
            outRect.left = margin
        }

        if (position == state.itemCount - 1) {
            outRect.right = margin
        }
    }
}