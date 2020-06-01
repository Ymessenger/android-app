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

package org.ymessenger.app.helpers

import org.ymessenger.app.utils.removeWhiteSpaces

object PhotoLabelHelper {
    fun get(name: String): String {
        val photoLabelStringBuilder = java.lang.StringBuilder()
        val parts = name.removeWhiteSpaces().split(" ")
        if (parts.size > 1) {
            parts.forEach { part ->
                photoLabelStringBuilder.append(part.firstOrNull())
            }
        } else {
            photoLabelStringBuilder.append(name)
        }
        return photoLabelStringBuilder.toString()
    }
}