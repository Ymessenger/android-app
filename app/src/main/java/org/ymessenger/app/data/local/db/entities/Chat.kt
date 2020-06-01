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

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "photo") var photo: String?,
    @ColumnInfo(name = "about") var about: String?,
    @ColumnInfo(name = "tag") var tag: String?,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "deleted") var deleted: Boolean
) {

    fun getPhotoUrl() = UrlGenerator.getFileUrl(photo)

    fun getPhotoLabel(): String {
        var label = id.toString()

        tag?.let {
            label = it
        }

        name.let { chatName ->
            val photoLabelStringBuilder = java.lang.StringBuilder()
            val parts = chatName.split(" ")
            if (parts.size > 1) {
                parts.forEach { part ->
                    photoLabelStringBuilder.append(part[0])
                }
            } else {
                photoLabelStringBuilder.append(chatName)
            }
            label = photoLabelStringBuilder.toString()
        }

        return label
    }
}