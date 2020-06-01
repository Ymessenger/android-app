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

package org.ymessenger.app.data.local.db.models

import androidx.room.Embedded
import androidx.room.Relation
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.Channel
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.local.db.entities.ForwardedMessageInfo
import org.ymessenger.app.data.local.db.entities.User

class ForwardedMessageInfoModel {
    @Embedded
    lateinit var forwardedMessageInfo: ForwardedMessageInfo

    @Relation(parentColumn = "forwarded_from_user", entityColumn = "id")
    lateinit var users: List<User>

    @Relation(parentColumn = "forwarded_from_user", entityColumn = "user_id")
    lateinit var contacts: List<Contact>

    @Relation(parentColumn = "forwarded_from_conversation_id", entityColumn = "id")
    lateinit var channels: List<Channel>

    fun getAuthor() = users.firstOrNull()

    fun getAuthorUserModel(): UserModel? {
        val user = getAuthor()
        return if (user != null) {
            UserModel().apply {
                this.user = user
                this.contacts = this@ForwardedMessageInfoModel.contacts
            }
        } else {
            null
        }
    }

    fun isChannelMessage() =
        forwardedMessageInfo.forwardedFromConversationType == ConversationType.CHANNEL

    fun getChannel() = if (isChannelMessage()) channels.firstOrNull() else null
}