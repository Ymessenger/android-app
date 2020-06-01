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

import androidx.room.*

@Entity(
    tableName = "forwarded_message_info",
    indices = [Index("message_id", "conversation_type", "conversation_id")],
    foreignKeys = [ForeignKey(
        entity = Message::class,
        parentColumns = ["global_id", "conversation_type", "conversation_id"],
        childColumns = ["message_id", "conversation_type", "conversation_id"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class ForwardedMessageInfo(
    @ColumnInfo(name = "message_id") var messageId: String,
    @ColumnInfo(name = "conversation_id") var conversationId: Long,
    @ColumnInfo(name = "conversation_type") var conversationType: Int,
    @ColumnInfo(name = "forwarded_from_user") var forwardedFromUser: Long?,
    @ColumnInfo(name = "forwarded_from_message_id") var forwardedFromMessageId: String,
    @ColumnInfo(name = "forwarded_from_conversation_id") var forwardedFromConversationId: Long,
    @ColumnInfo(name = "forwarded_from_conversation_type") var forwardedFromConversationType: Int
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null
}