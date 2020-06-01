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
import androidx.room.Index
import androidx.room.PrimaryKey
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.UserActionType

@Entity(
    tableName = "user_action",
    indices = [Index(value = ["user_id", "conversation_id", "conversation_type"], unique = true)]/*,
    foreignKeys = [ForeignKey(
        entity = ChatPreview::class,
        parentColumns = ["conversation_id", "conversation_type"],
        childColumns = ["conversation_id", "conversation_type"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]*/
)
data class UserAction(
    @ColumnInfo(name = "conversation_id") val conversationId: Long,
    @ColumnInfo(name = "conversation_type") val conversationType: Int,
    @ColumnInfo(name = "action") val action: Int,
    @ColumnInfo(name = "time") val time: Long,
    @ColumnInfo(name = "user_id") val userId: Long?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null

    fun getActionLabelRes(): Int {
        return when (action) {
            UserActionType.TYPING -> R.string.typing
            UserActionType.RECORDING_VOICE -> R.string.recording_a_voice
            else -> R.string.unknown_action
        }
    }

    fun getActionPlaceholderLabelRes(): Int {
        return when (action) {
            UserActionType.TYPING -> R.string.user_typing_placeholder
            UserActionType.RECORDING_VOICE -> R.string.user_recording_a_voice_placeholder
            else -> R.string.unknown_action
        }
    }
}