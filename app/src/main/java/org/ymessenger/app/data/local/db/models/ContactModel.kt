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
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.helpers.PhotoLabelHelper

class ContactModel {
    @Embedded
    lateinit var contact: Contact

    @Relation(parentColumn = "user_id", entityColumn = "id")
    lateinit var users: List<User>

    fun getUser() = users.firstOrNull()

    fun getPhotoLabel(): String {
        return if (contact.name.isNotBlank()) {
            PhotoLabelHelper.get(contact.name)
        } else {
            getUser()?.getPhotoLabel() ?: contact.userId.toString()
        }
    }

    fun getDisplayName(): String? {
        return if (contact.name.isNotBlank()) {
            contact.name
        } else {
            getUser()?.fullName
        }
    }

}