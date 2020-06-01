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

@Entity(
    tableName = "replied_messages",
    primaryKeys = ["conversation_id", "conversation_type", "global_id"]
)
data class RepliedMessage(
//    @PrimaryKey @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "sent_at") val sentAt: Long,
    @ColumnInfo(name = "sender_id") val senderId: Long?,
    @ColumnInfo(name = "receiver_id") val receiverId: Long?,
    @ColumnInfo(name = "conversation_id") val conversationId: Long,
    @ColumnInfo(name = "conversation_type") val conversationType: Int,
    @ColumnInfo(name = "read") var read: Boolean,
    @ColumnInfo(name = "reply_to") val replyTo: String?,
    @ColumnInfo(name = "global_id") val globalId: String
) {
}