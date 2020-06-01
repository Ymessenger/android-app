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

package org.ymessenger.app.models

import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.helpers.PhotoLabelHelper

class PollVotedUser(
    val user: User,
    val sign: String?,
    val signRequired: Boolean,
    var signVerified: Boolean
) {
    var contact: Contact? = null

    fun getDisplayName(): String? {
        return contact?.name ?: user.fullName
    }

    fun getPhotoLabel(): String {
        val contactName = contact?.name
        return if (contactName.isNullOrBlank()) {
            user.getPhotoLabel()
        } else {
            PhotoLabelHelper.get(contactName)
        }
    }
}