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

import android.content.Context
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.*
import org.ymessenger.app.helpers.PhotoLabelHelper

class ChatPreviewModel {
    @Embedded
    lateinit var chatPreview: ChatPreview

    @Relation(parentColumn = "user_id", entityColumn = "id")
    lateinit var users: List<User>

    @Relation(parentColumn = "user_id", entityColumn = "user_id")
    lateinit var contacts: List<Contact>

    @Relation(parentColumn = "last_message_sender_id", entityColumn = "user_id")
    lateinit var lastMessageContacts: List<Contact>

    @Relation(
        parentColumn = "conversation_id",
        entityColumn = "conversation_id",
        entity = ProtectedConversation::class
    )
    lateinit var protectedConversationList: List<ProtectedConversation>

    @Relation(
        parentColumn = "conversation_id",
        entityColumn = "conversation_id",
        entity = DraftMessage::class
    )
    lateinit var draftMessageList: List<DraftMessage>

    @Relation(
        parentColumn = "conversation_id",
        entityColumn = "identifier",
        entity = FavoriteConversation::class
    )
    lateinit var favoriteConversationList: List<FavoriteConversation>

    // Really stupid, but there is 2 fields to watch (conversation_id and user_id), so we have to combine 2 lists
    @Relation(
        parentColumn = "user_id",
        entityColumn = "identifier",
        entity = FavoriteConversation::class
    )
    lateinit var favoriteUsersList: List<FavoriteConversation>

    /**
     * This variable stores last displayed online status
     */
    @Ignore
    var online = false

    /**
     * Gets user online status (if has) and stores it into online variable
     */
    fun isOnline(): Boolean {
        if (!chatPreview.isDialog()) return false

        online = getUser()?.isOnline() ?: false

        return online
    }

    fun getUser() = users.firstOrNull()

    fun getUserModel(): UserModel? {
        val user = getUser()
        return if (user != null) {
            UserModel().apply {
                this.user = user
                this.contacts = this@ChatPreviewModel.contacts
            }
        } else {
            null
        }
    }

    fun getDisplayChatName(): String? {
        return if (chatPreview.isDialog()) {
            getUserModel()?.getDisplayName() ?: chatPreview.chatName
        } else {
            chatPreview.chatName
        }
    }

    fun getLastMessageSenderName(): String? {
        val lastMessageSenderContact = lastMessageContacts.firstOrNull()
        return lastMessageSenderContact?.name ?: chatPreview.userName
    }

    // This is workaround since we can't get relation by composite primary key. Yes, this is effect on performance
    fun isFavourite(): Boolean {
        var isFavorite = false

        // Skip this loop if it's not a dialog
        if (chatPreview.userId == null) {
            // This loop is for chats and channels
            for (favoriteConversation in favoriteConversationList) {
                if (favoriteConversation.conversationType == chatPreview.conversationType) {
                    isFavorite = true
                    break
                }
            }
        } else {
            // This loop is for dialogs
            for (favoriteUser in favoriteUsersList) {
                if (favoriteUser.conversationType == chatPreview.conversationType) {
                    isFavorite = true
                    break
                }
            }
        }

        return isFavorite
    }

    // This is workaround since we can't get relation by composite primary key. Yes, this is effect on performance
    fun isProtected(): Boolean {
        var isProtected = false

        for (protectedConversation in protectedConversationList) {
            if (protectedConversation.conversationType == chatPreview.conversationType) {
                isProtected = true
                break
            }
        }

        return isProtected
    }

    // This is workaround since we can't get relation by composite primary key. Yes, this is effect on performance
    fun getDraft(): DraftMessage? {

        for (draftMessage in draftMessageList) {
            if (draftMessage.conversationType == chatPreview.conversationType) {
                return draftMessage
            }
        }

        return null
    }

    fun getPhotoLabel(): String {
        var label = chatPreview.userId.toString()

        getUser()?.tag?.let {
            label = it
        }

        chatPreview.chatName?.let { chatName ->
            label = PhotoLabelHelper.get(chatName)
        }

        getUserModel()?.getDisplayName()?.let {
            label = PhotoLabelHelper.get(it)
        }

        return label
    }

    fun getAttachmentDescriptionRes() =
        Attachment.getAttachmentDescriptionRes(chatPreview.attachmentType)

    fun hasAttachments(): Boolean {
        return getAttachmentTypes().isNotEmpty()
    }

    private fun getAttachmentTypes(): List<Int> {
        val gson = Gson()

        return gson.fromJson(
            chatPreview.attachmentTypes,
            object : TypeToken<List<Int>>() {}.type
        )
    }

    fun getAttachmentDescription(context: Context): String {
        val attachmentTypes = getAttachmentTypes()
        return Attachment.getAttachmentsDescription(context, attachmentTypes)
    }

    fun isMuted() = chatPreview.isMuted

    fun isDialog() = chatPreview.conversationType == ConversationType.DIALOG
}