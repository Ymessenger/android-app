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
import org.ymessenger.app.data.remote.entities.Contact
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ContactRepository
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent

class EditContactViewModel(
    private val contactId: String,
    private val contactRepository: ContactRepository
) : BaseViewModel() {

    private val contactModel = contactRepository.getContactModelById(contactId)

    val contactName = MutableLiveData<String>()

    val doneEvent = SingleLiveEvent<Void>()

    init {
        contactModel.observeForever {
            if (contactName.value.isNullOrBlank()) {
                contactName.postValue(it.contact.name)
            }
        }
    }

    fun save() {
        val contact = contactModel.value?.contact ?: return
        val newContactName = contactName.value.toString().trim()

        startLoading()
        val remoteContact = Contact(contact.userId, newContactName)
        remoteContact.contactId = contactId

        contactRepository.editContact(remoteContact, object : SuccessErrorCallback {
            override fun success() {
                endLoading()
                doneEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    class Factory(
        private val contactId: String,
        private val contactRepository: ContactRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EditContactViewModel(
                contactId,
                contactRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "EditContactViewModel"
    }
}