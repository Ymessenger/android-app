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
import org.ymessenger.app.data.local.db.entities.ChatUser
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.helpers.PhotoLabelHelper

class ChatUserModel {
    @Embedded
    lateinit var chatUser: ChatUser

    @Relation(parentColumn = "user_id", entityColumn = "id")
    lateinit var users: List<User>

    @Relation(parentColumn = "user_id", entityColumn = "user_id")
    lateinit var contacts: List<Contact>

    fun getUser() = users.firstOrNull()

    fun getUserModel(): UserModel? {
        val user = getUser()
        return if (user != null) {
            UserModel().apply {
                this.user = user
                this.contacts = this@ChatUserModel.contacts
            }
        } else {
            null
        }
    }

    fun canEdit(): Boolean {
        return isCreator() || isAdmin()
    }

    fun isCreator() = chatUser.userRole == 3

    fun isAdmin() = chatUser.userRole == 2

    fun isModerator() = chatUser.userRole == 1

    fun isChatUser() = chatUser.userRole == 0

    fun getPhotoLabel(): String {
        var label = chatUser.userId.toString()

        getUserModel()?.user?.tag?.let {
            label = it
        }

        getUserModel()?.getDisplayName()?.let { userName ->
            label = PhotoLabelHelper.get(userName)
        }

        return label
    }
}