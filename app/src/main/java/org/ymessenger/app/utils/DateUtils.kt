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

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DateUtils {
    companion object {
        val timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
        val dateFormat = SimpleDateFormat.getDateInstance()
        val dateFormatWithoutYear = (SimpleDateFormat.getDateInstance() as SimpleDateFormat).apply {
            applyPattern(
                toPattern().replace(
                    "([^\\p{Alpha}']|('[\\p{Alpha}]+'))*y+([^\\p{Alpha}']|('[\\p{Alpha}]+'))*".toRegex(),
                    ""
                )
            )
        }
        val dayOfWeekFormat = SimpleDateFormat("E")

        private val dateTimeFormat = SimpleDateFormat.getDateTimeInstance()

        fun timeFormat(calendar: Calendar): String = timeFormat.format(calendar.time)

        fun dateFormat(calendar: Calendar): String = dateFormat.format(calendar.time)

        fun dateFormatWithoutYear(calendar: Calendar): String {
            return dateFormatWithoutYear.format(calendar.time)
        }

        fun dateTimeFormat(unixtime: Long): String {
            val calendar = Calendar.getInstance().apply { timeInMillis = unixtime * 1000 }

            return dateTimeFormat.format(calendar.time)
        }

        fun smartFormat(unixtime: Long): String {
            return smartFormat(Calendar.getInstance().apply { timeInMillis = unixtime * 1000 })
        }

        fun smartFormat(date: Calendar): String {
            val now = Calendar.getInstance()

            val week: Long = 7 * 24 * 60 * 60 * 1000

            return when {
                DateUtils.isToday(date.timeInMillis) -> {
                    timeFormat.format(date.time)
                }
                now.timeInMillis - date.timeInMillis <= week -> {
                    dayOfWeekFormat.format(date.time)
                }
                now.get(Calendar.YEAR) == date.get(Calendar.YEAR) -> {
                    dateFormatWithoutYear.format(date.time)
                }
                else -> {
                    dateFormat.format(date.time)
                }
            }
        }
    }
}