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

import android.content.Context
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.EncryptedMessage
import org.ymessenger.app.data.remote.entities.FileInfo
import org.ymessenger.app.data.remote.entities.Poll

@Entity(
    tableName = "attachments",
    indices = [Index("message_id", "conversation_type", "conversation_id")],
    foreignKeys = [ForeignKey(
        entity = Message::class,
        parentColumns = ["global_id", "conversation_type", "conversation_id"],
        childColumns = ["message_id", "conversation_type", "conversation_id"],
        onUpdate = CASCADE,
        onDelete = CASCADE
    )]
)
data class Attachment(
    @ColumnInfo(name = "message_id") var messageId: String,
    @ColumnInfo(name = "conversation_id") var conversationId: Long,
    @ColumnInfo(name = "conversation_type") var conversationType: Int,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "payload") var payload: String,
    @ColumnInfo(name = "saved_at") var savedAt: String?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null

    object Type {
        const val AUDIO = 0
        const val FILE = 1
        const val PICTURE = 2
        const val VIDEO = 3
        const val ENCRYPTED_MESSAGE = 4
        const val FORWARDED_MESSAGES = 5
        const val EXCHANGE_KEY_MESSAGE = 6
        const val POLL = 7
        const val VOICE = 8
    }

    fun isAudio() = type == Type.AUDIO

    fun isFile() = type == Type.FILE

    fun isPicture() = type == Type.PICTURE

    fun isVideo() = type == Type.VIDEO

    fun isEncryptedMessage() = type == Type.ENCRYPTED_MESSAGE

    fun isForwardedMessages() = type == Type.FORWARDED_MESSAGES

    fun isExchangeKeyMessage() = type == Type.EXCHANGE_KEY_MESSAGE

    fun isPoll() = type == Type.POLL

    fun isVoice() = type == Type.VOICE

    fun getPayloadAsFile() = getPayloadAs<FileInfo?>()

    fun getPayloadAsForwardedMessages() =
        getPayloadAs<List<org.ymessenger.app.data.remote.entities.Message>>()

    fun getPayloadAsEncryptedMessage() = getPayloadAs<EncryptedMessage>()

    fun getPayloadAsPoll() = getPayloadAs<Poll?>()

    private fun <T> getPayloadOfType(typeOfT: java.lang.reflect.Type): T {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                org.ymessenger.app.data.remote.entities.Attachment::class.java,
                WebSocketService.AttachmentsDeserializer()
            )
            .create()
        return gson.fromJson<T>(payload, typeOfT)
    }

    private inline fun <reified T> getPayloadAs() =
        this.getPayloadOfType<T>(object : TypeToken<T>() {}.type)

    companion object {
        fun getAttachmentDescriptionRes(attachmentType: Int?) = when (attachmentType) {
            Type.AUDIO -> R.string.at_audio
            Type.PICTURE -> R.string.at_picture
            Type.FILE -> R.string.at_file
            Type.ENCRYPTED_MESSAGE -> R.string.at_encrypted_message
            Type.FORWARDED_MESSAGES -> R.string.at_forwarded_message
            Type.EXCHANGE_KEY_MESSAGE -> R.string.symmetric_encryption_key
            Type.POLL -> R.string.poll
            Type.VOICE -> R.string.voice_message
            else -> R.string.attachment
        }

        fun getAttachmentsDescription(context: Context, attachmentTypes: List<Int>): String {
            if (attachmentTypes.isNotEmpty()) {
                val size = attachmentTypes.size
                if (size == 1) {
                    return context.getString(getAttachmentDescriptionRes(attachmentTypes[0]))
                } else {
                    var oneType = true
                    var firstAttachmentType = attachmentTypes[0]
                    for (attachmentType in attachmentTypes) {
                        if (attachmentType != firstAttachmentType) {
                            oneType = false
                            break
                        }
                    }

                    if (oneType) {
                        return when (firstAttachmentType) {
                            Type.PICTURE -> {
                                context.resources.getQuantityString(
                                    R.plurals.photo_plurals,
                                    size,
                                    size
                                )
                            }

                            Type.FILE -> {
                                context.resources.getQuantityString(
                                    R.plurals.file_plurals,
                                    size,
                                    size
                                )
                            }

                            else -> {
                                context.resources.getQuantityString(
                                    R.plurals.attachment_plurals,
                                    size,
                                    size
                                )
                            }
                        }
                    } else {
                        return context.resources.getQuantityString(
                            R.plurals.attachment_plurals,
                            size,
                            size
                        )
                    }
                }
            } else {
                return ""
            }
        }

    }
}