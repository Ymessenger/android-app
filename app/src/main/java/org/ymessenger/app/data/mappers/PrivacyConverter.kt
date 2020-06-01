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

package org.ymessenger.app.data.mappers

object PrivacyConverter {
    fun toString(visible: BooleanArray?): String? {
        if (visible == null) return null

        val sb = StringBuilder()
        for (value in visible) {
            sb.append(if (value) 1 else 0)
        }

        return sb.toString()
    }

    fun toBooleanArray(visibleStr: String?): BooleanArray? {
        if (visibleStr == null) return null

        val visible = ArrayList<Boolean>()

        for (value in visibleStr) {
            visible.add(value == '1')
        }

        return visible.toBooleanArray()
    }
}