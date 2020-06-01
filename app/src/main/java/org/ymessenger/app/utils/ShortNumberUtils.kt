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

import java.math.RoundingMode
import java.text.DecimalFormat

object ShortNumberUtils {

    fun getShortNumber(number: Int): String {
        return when {
            number < 1000 -> number.toString()
            number < 1000000 -> {
                val df = DecimalFormat("#.#K")
                df.roundingMode = RoundingMode.HALF_UP
                df.format(number / 1000.0)
            }
            else -> {
                val df = DecimalFormat("#.#M")
                df.roundingMode = RoundingMode.HALF_UP
                df.format(number / 1000000.0)
            }
        }
    }

}