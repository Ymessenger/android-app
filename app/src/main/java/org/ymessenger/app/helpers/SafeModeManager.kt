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

package org.ymessenger.app.helpers

import org.ymessenger.app.data.repositories.ChatPreviewRepository
import org.ymessenger.app.data.repositories.ContactGroupRepository
import org.ymessenger.app.data.repositories.ContactRepository

class SafeModeManager(
    private val contactRepository: ContactRepository,
    private val contactGroupRepository: ContactGroupRepository,
    private val chatPreviewRepository: ChatPreviewRepository
) {

    var isSafeMode = false
        private set

    fun enterSafeMode() {
        isSafeMode = true

        contactRepository.deleteAllContacts()
        contactGroupRepository.deleteAll()
        chatPreviewRepository.deleteAllChatPreviews()
    }

    fun exitSafeMode() {
        isSafeMode = false
    }
}