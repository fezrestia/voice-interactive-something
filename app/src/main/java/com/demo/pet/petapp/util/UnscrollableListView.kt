package com.demo.pet.petapp.util

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

class UnscrollableListView : ListView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(width: Int, height: Int) {
        val convertedHeight = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE shr 2, MeasureSpec.AT_MOST)

        super.onMeasure(width, convertedHeight)

        val params = layoutParams
        params.height = measuredHeight
    }

}
