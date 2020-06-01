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
import androidx.room.Ignore
import androidx.room.Relation
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.*
import org.ymessenger.app.helpers.PhotoLabelHelper

class FavoriteConversationModel {
    @Embedded
    lateinit var favoriteConversation: FavoriteConversation

    @Relation(parentColumn = "identifier", entityColumn = "id")
    lateinit var users: List<User>

    @Relation(parentColumn = "identifier", entityColumn = "user_id")
    lateinit var contacts: List<Contact>

    fun getUser() = users.firstOrNull()

    private fun getContact() = contacts.firstOrNull()

    @Relation(parentColumn = "identifier", entityColumn = "id")
    lateinit var chats: List<Chat>

    fun getChat() = chats.firstOrNull()


    @Relation(parentColumn = "identifier", entityColumn = "id")
    lateinit var channels: List<Channel>

    fun getChannel() = channels.firstOrNull()

    /**
     * This variable stores last displayed online status
     */
    @Ignore
    var online = false

    /**
     * Gets user online status (if has) and stores it into online variable
     */
    fun isOnline(): Boolean {
        if (favoriteConversation.conversationType != ConversationType.DIALOG) return false

        online = getUser()?.isOnline() ?: false

        return online
    }

    fun getPhotoLabel(): String {
        val photoLabel: String

        when (favoriteConversation.conversationType) {
            ConversationType.DIALOG -> {
                val contact = getContact()
                val user = getUser()
                photoLabel = if (contact != null && contact.name.isNotBlank()) {
                    PhotoLabelHelper.get(contact.name)
                } else {
                    user?.getPhotoLabel() ?: favoriteConversation.identifier.toString()
                }
            }

            ConversationType.CHAT -> {
                val chat = getChat()

                photoLabel = chat?.getPhotoLabel() ?: favoriteConversation.identifier.toString()
            }

            ConversationType.CHANNEL -> {
                val channel = getChannel()

                photoLabel = channel?.getPhotoLabel() ?: favoriteConversation.identifier.toString()
            }

            else -> {
                photoLabel = favoriteConversation.identifier.toString()
            }
        }

        return photoLabel
    }

    fun getPhotoUrl(): String? {
        val photoUrl: String?

        when (favoriteConversation.conversationType) {
            ConversationType.DIALOG -> {
                val user = getUser()
                photoUrl = user?.getPhotoUrl()
            }

            ConversationType.CHAT -> {
                val chat = getChat()

                photoUrl = chat?.getPhotoUrl()
            }

            ConversationType.CHANNEL -> {
                val channel = getChannel()

                photoUrl = channel?.getPhotoUrl()
            }

            else -> {
                photoUrl = null
            }
        }

        return photoUrl
    }

}