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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.activities.ContactsActivity
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.repositories.ContactGroupRepository

class ContactListViewModel(
    private val contactGroupRepository: ContactGroupRepository
) : ViewModel() {

    // TODO: add selected contacts OBJECTS (not ids)

    val contactGroupList = contactGroupRepository.getContactGroups()
    val searchQuery = MutableLiveData<String>("")

    // TODO: Should replace selectedContacts with selectedContactsId?
    val selectedContactsId = MutableLiveData<List<String>>()
    val selectedContacts = ArrayList<Contact>()
    private val _selectedContactsId = ArrayList<String>()
    var requestCode: Int = ContactsActivity.NO_REQUEST_CODE

    init {
        selectedContactsId.postValue(_selectedContactsId)
    }

    fun addSelectedContact(contact: Contact) {
//        if (_selectedContactsId.contains(contactId)) {
//            _selectedContactsId.remove(contactId)
//        } else {
//            _selectedContactsId.add(contactId)
//        }

        if (selectedContacts.contains(contact)) {
            selectedContacts.remove(contact)
        } else {
            selectedContacts.add(contact)
        }

        _selectedContactsId.clear()
        for (selectedContact in selectedContacts) {
            _selectedContactsId.add(selectedContact.id)
        }

        selectedContactsId.postValue(_selectedContactsId)
    }

    fun setFilter(filter: String) {
        searchQuery.postValue(filter)
    }

    class Factory(
        private val contactGroupRepository: ContactGroupRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ContactListViewModel(
                contactGroupRepository
            ) as T
        }
    }

}