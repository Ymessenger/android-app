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

package org.ymessenger.app.data.mappers

import com.google.gson.Gson
import org.ymessenger.app.data.local.db.entities.ChatPreview

class ChatPreviewMapper {

    private val gson = Gson()

    fun toDb(from: org.ymessenger.app.data.remote.entities.ChatPreview) = ChatPreview(
        from.conversationId,
        from.conversationType,
        from.title,
        from.previewText,
        from.secondUid,
        from.lastMessageSenderName,
        from.photo,
        from.lastMessageTime,
        from.lastMessageSenderId,
        from.unreadedCount,
        from.chatType,
        from.lastMessageId,
        from.attachmentType,
        gson.toJson(from.attachmentTypes),
        from.read,
        from.isMuted
    )
}