/*
 * This file is part of Y messenger.
 *
 * Y messenger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Y messenger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Y messenger.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ymessenger.app.controllers

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.ymessenger.app.R

class DotIndicatorController(private val context: Context, private val dotContainer: LinearLayout) {

    private var slideCount = 0
    private var currentPosition = 0

    fun initialize(slideCount: Int) {
        this.slideCount = slideCount

        for (i in 0 until slideCount) {
            val dot = ImageView(this.context)
            dot.setImageDrawable(ContextCompat.getDrawable(this.context, R.drawable.dot_indicator))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dotContainer.addView(dot, params)
        }

        setSelected(0)
    }

    fun setSelected(position: Int) {
        val drawableUnselected = ContextCompat.getDrawable(this.context, R.drawable.dot_indicator)
        val drawableSelected =
            ContextCompat.getDrawable(this.context, R.drawable.dot_selected_indicator)

        (dotContainer.getChildAt(currentPosition) as ImageView).setImageDrawable(drawableUnselected)
        (dotContainer.getChildAt(position) as ImageView).setImageDrawable(drawableSelected)

        currentPosition = position
    }

}