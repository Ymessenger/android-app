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

package org.ymessenger.app.utils

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import org.ymessenger.app.R

class WidgetUtils {

    companion object {
        /**
         * By default error message in TextInputLayout has gravity START.
         * But we need to center it on specific widget.
         * @param textInputLayout widget which error message should be centered
         */
        fun textInputLayoutCenterErrorMessage(textInputLayout: TextInputLayout) {
            val errorTextView = textInputLayout.findViewById<TextView>(R.id.textinput_error)
            val errorFrameLayout = errorTextView.parent as FrameLayout
            errorTextView.gravity = Gravity.CENTER
            errorFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
}