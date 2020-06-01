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

data class Attachment(
    @SerializedName("Type")
    val type: Int?,
    @SerializedName("Hash")
    val hash: String?,
    @SerializedName("MessageId")
    val messageId: Long,
    @SerializedName("Payload")
    val payload: String,
    @SerializedName("NodesId")
    val nodesId: List<Long>?
) {
    object Type {
        const val AUDIO = 0
        const val FILE = 1
        const val PICTURE = 2
        const val VIDEO = 3
        const val ENCRYPTED_MESSAGE = 4
        const val FORWARDED_MESSAGES = 5
        const val KEY_EXCHANGE_MESSAGE = 6
        const val POLL = 7
        const val VOICE = 8
    }
}