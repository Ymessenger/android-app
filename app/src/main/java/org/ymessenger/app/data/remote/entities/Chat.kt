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

package org.ymessenger.app.data.remote.entities

import com.google.gson.annotations.SerializedName

data class Chat(
    @SerializedName("Id")
    val id: Long?,
    @SerializedName("Name")
    val name: String,
    @SerializedName("Tag")
    val tag: String?,
    @SerializedName("Photo")
    val photo: String?,
    @SerializedName("About")
    val about: String?,
    @SerializedName("NodesId")
    val nodesId: List<Long>?,
    @SerializedName("Type")
    val type: Int,
    @SerializedName("Users")
    val users: List<ChatUser>
) {
    object Type {
        const val PRIVATE = 0
        const val PUBLIC = 1
        const val CHANNEL = 2
        const val PRIVATE_CHANNEL = 3
    }
}