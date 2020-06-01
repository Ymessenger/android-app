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

package org.ymessenger.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.data.local.db.entities.ContactGroup
import org.ymessenger.app.data.repositories.ContactGroupRepository
import org.ymessenger.app.data.repositories.ContactRepository

class ContactGroupListViewModel(
    private val contactGroupRepository: ContactGroupRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val contactGroupList = contactGroupRepository.getContactGroups()

    init {
        contactRepository.getContactModels()
    }

    fun updateSort(contactGroups: List<ContactGroup>) {
        for ((index, contactGroup) in contactGroups.withIndex()) {
            contactGroup.sort = index
        }
        contactGroupRepository.update(contactGroups)
    }

    class Factory(
        private val contactGroupRepository: ContactGroupRepository,
        private val contactRepository: ContactRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ContactGroupListViewModel(contactGroupRepository, contactRepository) as T
        }
    }

}