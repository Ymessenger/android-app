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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.data.local.db.models.ContactModel
import org.ymessenger.app.data.repositories.ContactRepository
import org.ymessenger.app.data.repositories.GroupContactRepository
import org.ymessenger.app.data.repositories.UserRepository

class ContactGroupPageViewModel(
    private val contactGroupId: String?,
    private val groupContactRepository: GroupContactRepository,
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val filterQuery = MutableLiveData<String>()

    private val contactListResult = Transformations.switchMap(filterQuery) {
        if (contactGroupId == null) {
            if (it.isBlank()) {
                contactRepository.getContactModels()
            } else {
                contactRepository.getContactModelsByName(it)
            }
        } else {
            if (it.isBlank()) {
                groupContactRepository.getContactsByContactGroup(contactGroupId)
            } else {
                groupContactRepository.getContactsByGroupAndName(contactGroupId, it)
            }
        }
    }

    val contactList = Transformations.map(contactListResult) {
        checkForNullUsers(it)
        it
    }

    private fun checkForNullUsers(contactModels: List<ContactModel>) {
        val usersId = hashSetOf<Long>()

        for (contactModel in contactModels) {
            if (contactModel.getUser() == null) {
                usersId.add(contactModel.contact.userId)
            }
        }

        if (usersId.isNotEmpty()) {
            userRepository.getUsers(usersId.toList())
        }
    }

    fun setFilter(query: String) {
        if (filterQuery.value != query) {
            filterQuery.postValue(query)
        }
    }

    class Factory(
        private val contactGroupId: String?,
        private val groupContactRepository: GroupContactRepository,
        private val contactRepository: ContactRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ContactGroupPageViewModel(
                contactGroupId,
                groupContactRepository,
                contactRepository,
                userRepository
            ) as T
        }
    }

}