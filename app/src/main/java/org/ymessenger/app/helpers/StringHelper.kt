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

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes

class StringHelper(private val context: Context) {

    fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    fun getIdentifier(name: String): Int {
        return context.resources.getIdentifier(name, "string", context.packageName)
    }

    fun getStringByName(name: String): String {
        val resourceName = getAsResourceName(name)
        val resId = getIdentifier(resourceName)
        return if (resId == 0) {
            Log.w(TAG, "'$resourceName' not found in resources. Raw string will be used: '$name'")
            name
        } else {
            getString(resId)
        }
    }

    private fun getAsResourceName(str: String): String {
        return str.trim()
            .filter { it.isLetter() || it.isWhitespace() }
            .replace("\\s+".toRegex(), " ")
            .replace(" ", "_")
            .toLowerCase()
    }

    companion object {
        private const val TAG = "StringHelper"
    }

}