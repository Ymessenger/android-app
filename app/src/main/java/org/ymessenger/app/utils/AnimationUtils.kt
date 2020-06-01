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

import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation

class AnimationUtils {
    companion object {
        fun shakeError(): TranslateAnimation {
            val shake = TranslateAnimation(0f, 10f, 0f, 0f)
            shake.duration = 500
            shake.interpolator = CycleInterpolator(7f)
            return shake
        }

        fun toggleArrow(view: View, isExpanded: Boolean, duration: Long = 200): Boolean {
            if (isExpanded) {
                view.animate().setDuration(duration).rotation(180F)
            } else {
                view.animate().setDuration(duration).rotation(0F)
            }

            return isExpanded
        }
    }
}