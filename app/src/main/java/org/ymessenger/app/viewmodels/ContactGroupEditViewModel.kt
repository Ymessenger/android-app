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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.mappers.PrivacyConverter
import org.ymessenger.app.data.remote.entities.Group
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ContactGroupRepository
import org.ymessenger.app.data.repositories.GroupContactRepository
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent

class ContactGroupEditViewModel(
    private val contactGroupId: String?,
    private val contactGroupRepository: ContactGroupRepository,
    private val groupContactRepository: GroupContactRepository
) : BaseViewModel() {

    private var groupId = MutableLiveData(contactGroupId)

    val contactGroup = Transformations.switchMap(groupId) {
        it?.let {
            contactGroupRepository.getContactGroup(it)
        }
    }

    val name = MutableLiveData<String>()

    val groupContactList = Transformations.switchMap(contactGroup) {
        it?.let {
            name.postValue(it.name)
            setPrivacySettings(it.privacy)
            groupContactRepository.getContactsByContactGroup(it.id)
        }
    }

    private val selectedContacts = ArrayList<Contact>()
    val selectedContactsId = MutableLiveData<ArrayList<String>>()

    val savedEvent = SingleLiveEvent<Void>()

    val showPrivacySettings = MutableLiveData<Boolean>(false)

    val nameAndTagSwitch = MutableLiveData<Boolean>()
    val photoAndAboutSwitch = MutableLiveData<Boolean>()
    val onlineSwitch = MutableLiveData<Boolean>()
    val phoneSwitch = MutableLiveData<Boolean>()
    val emailSwitch = MutableLiveData<Boolean>()

    fun addUsers(usersId: List<Long>) {
        if (contactGroupId == null) {
            showError(R.string.create_group_first)
            return
        }

        startLoading(R.string.adding_users_to_contact_group)
        groupContactRepository.addUsers(contactGroupId, usersId, object : SuccessErrorCallback {
            override fun success() {
                Log.d(TAG, "Users added successfully")
                endLoading()
            }

            override fun error(error: ResultResponse) {
                Log.e(TAG, "Failed to add users")
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun removeUsers() {
        if (contactGroupId == null) {
            showError(R.string.create_group_first)
            return
        }

        val usersId = selectedContacts.map { it.userId }

        startLoading(R.string.removing_users_from_contact_group)
        groupContactRepository.removeUsers(contactGroupId, usersId, object : SuccessErrorCallback {
            override fun success() {
                Log.d(TAG, "Users removed successfully")
                endLoading()
                selectedContactsId.postValue(arrayListOf()) // Empty selected contacts to hide FAB delete
            }

            override fun error(error: ResultResponse) {
                Log.e(TAG, "Failed to remove users")
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun showPrivacySettingsClick() {
        showPrivacySettings.postValue(!(showPrivacySettings.value ?: false))
    }

    private fun setPrivacySettings(privacy: String?) {
        val privacyArray = PrivacyConverter.toBooleanArray(privacy) ?: return

        val nameTag = privacyArray[1]
        val photoAbout = privacyArray[2]
        val online = privacyArray[14]
        val phone = privacyArray[15]
        val email = privacyArray[17]

        nameAndTagSwitch.postValue(nameTag)
        onlineSwitch.postValue(online)
        phoneSwitch.postValue(phone)
        emailSwitch.postValue(email)
        photoAndAboutSwitch.postValue(photoAbout)
    }

    fun getPrivacySettings(): BooleanArray {
        val privacy = BooleanArray(22)

        privacy[1] = nameAndTagSwitch.value ?: false
        privacy[2] = photoAndAboutSwitch.value ?: false
        privacy[14] = onlineSwitch.value ?: false
        privacy[15] = phoneSwitch.value ?: false
        privacy[17] = emailSwitch.value ?: false

        return privacy
    }

    fun saveContactGroup() {
        val group = Group(name.value)
        group.privacySettings = getPrivacySettings()
        group.groupId = contactGroupId

        startLoading(if (contactGroupId == null) R.string.creating_of_contact_group else R.string.editing_of_contact_group)
        contactGroupRepository.createOrEditContactGroup(group, object : SuccessErrorCallback {
            override fun success() {
                endLoading()
                savedEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun removeContactGroup() {
        if (contactGroupId == null) {
            showError(R.string.create_group_first)
            return
        }

        startLoading(R.string.deleting)
        contactGroupRepository.deleteContactGroups(
            listOf(contactGroupId),
            object : SuccessErrorCallback {
                override fun success() {
                    endLoading()
                    savedEvent.call() // LOL. It should be deletedEvent, but it's fine
                }

                override fun error(error: ResultResponse) {
                    endLoading()
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    fun addRemoveSelectedContact(contact: Contact) {
        if (selectedContacts.contains(contact)) {
            selectedContacts.remove(contact)
        } else {
            selectedContacts.add(contact)
        }

        val selectedContactsIdTmp = selectedContacts.map { it.id } as ArrayList

        selectedContactsId.postValue(selectedContactsIdTmp)
    }

    companion object {
        private const val TAG = "ContactGroupEditVM"
    }

    class Factory(
        private val contactGroupId: String?,
        private val contactGroupRepository: ContactGroupRepository,
        private val groupContactRepository: GroupContactRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ContactGroupEditViewModel(
                contactGroupId,
                contactGroupRepository,
                groupContactRepository
            ) as T
        }
    }
}