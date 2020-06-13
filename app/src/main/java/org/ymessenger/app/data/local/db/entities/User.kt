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

package org.ymessenger.app.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ymessenger.app.data.remote.UrlGenerator
import org.ymessenger.app.helpers.PhotoLabelHelper
import java.util.*

@Entity(tableName = "users")
data class User(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "first_name") var firstName: String?,
    @ColumnInfo(name = "last_name") var lastName: String?,
    @ColumnInfo(name = "about") var about: String?,
    @ColumnInfo(name = "email") var email: String?,
    @ColumnInfo(name = "phone") var phone: String?,
    @ColumnInfo(name = "photo") var photo: String?,
    @ColumnInfo(name = "online") var online: Long?,
    @ColumnInfo(name = "tag") var tag: String?,
    @ColumnInfo(name = "privacy") var privacy: String?,
    @ColumnInfo(name = "confirmed") var confirmed: Boolean,
    @ColumnInfo(name = "banned") var banned: Boolean,
    @ColumnInfo(name = "node_id") var nodeId: Long?
) {
    val fullName: String?
        get(): String? {
            val sb = StringBuilder()
            firstName?.let {
                sb.append(it)
            }
            if (!lastName.isNullOrBlank()) {
                sb.append(" ")
                sb.append(lastName)
            }

            return if (sb.isBlank())
                null
            else
                sb.toString()
        }

    fun isOnline(): Boolean {
        online?.let {
            val now = Calendar.getInstance()
            val diff = now.timeInMillis - it * 1000
            return diff < 1000 * 60
        } ?: return false
    }

    fun getPhotoUrl() = UrlGenerator.getFileUrl(photo)

    fun getPhotoLabel(): String {
        var label = id.toString()

        tag?.let {
            label = it
        }

        fullName?.let { userName ->
            label = PhotoLabelHelper.get(userName)
        }

        return label
    }
}