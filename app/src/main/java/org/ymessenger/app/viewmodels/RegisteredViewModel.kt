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

import android.content.ContentResolver
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.remote.entities.Contact
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ContactRepository
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.models.PhoneContact
import org.ymessenger.app.utils.SingleLiveEvent

class RegisteredViewModel(
    private val contentResolver: ContentResolver,
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val settingsHelper: SettingsHelper
) : BaseViewModel() {

    private val contactList = MutableLiveData<List<PhoneContact>>()

    val uniquePhones = Transformations.map(contactList) {
        getUniqueNumbers(it)
    }

    val closeActivityEvent = SingleLiveEvent<Void>()

    fun loadContacts() {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PROJECTION,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )

        cursor.moveToFirst()
        val contacts: MutableList<PhoneContact> = mutableListOf()
        while (!cursor.isAfterLast) {
            val id = cursor.getLong(cursor.getColumnIndex(PROJECTION[0]))
            val lookupKey = cursor.getString(cursor.getColumnIndex(PROJECTION[1]))
            val name = cursor.getString(cursor.getColumnIndex(PROJECTION[2]))
            var number: String? = try {
                cursor.getString(cursor.getColumnIndex(PROJECTION[3]))
            } catch (e: Exception) {
                null
            }
            contacts.add(PhoneContact(id, lookupKey, name, number))
            cursor.moveToNext()
        }
        cursor.close()

        contactList.postValue(contacts)
    }

    private fun getUniqueNumbers(phoneContacts: List<PhoneContact>): List<String> {
        val numbers = hashSetOf<String>()

        for (phoneContact in phoneContacts) {
            val number = phoneContact.number ?: continue

            // Skip phones that doesn't start with +
            if (!number.startsWith("+")) continue

            // Remove unwished symbols
            val cleanedNumber = number.replace("[()\\s-]+".toRegex(), "")

            numbers.add(cleanedNumber)
        }

        return numbers.toList()
    }

    fun searchUsersByPhones(phones: List<String>) {
        if (phones.isEmpty()) {
            done()
            return
        }

        startLoading(R.string.looking_for_users)
        userRepository.getUsersByPhones(phones, object : UserRepository.GetUsers {
            override fun result(users: List<User>) {
                endLoading()
                if (users.isNotEmpty()) {
                    addUsersToContacts(users)
                } else {
                    showToast(R.string.none_of_your_contacts_use_messenger)
                    closeActivityEvent.call()
                }
            }

            override fun error() {
                endLoading()
                Log.e(TAG, "Failed to get users by phones")
                closeActivityEvent.call()
            }
        })
    }

    fun addUsersToContacts(users: List<User>) {
        startLoading(R.string.adding_to_contacts)
        for (user in users) {
            val contact = Contact(user.id, user.fullName!!)
            contactRepository.addContact(contact, object : SuccessErrorCallback {
                override fun success() {
                    if (user == users.last()) {
                        endLoading()
                        done()
                    }
                    // nothing
                }

                override fun error(error: ResultResponse) {
                    Log.e(TAG, "Failed to add user with id ${user.id} to contacts")
                    if (user == users.last()) {
                        endLoading()
                        done()
                    }
                }
            })
        }
    }

    private fun done() {
        showToast(R.string.done)
        closeActivityEvent.call()
    }

    fun getSyncContacts(): Boolean {
        return settingsHelper.getSyncContacts()
    }

    fun setSyncContacts(sync: Boolean) {
        settingsHelper.setSyncContacts(sync)
    }

    class Factory(
        private val contentResolver: ContentResolver,
        private val userRepository: UserRepository,
        private val contactRepository: ContactRepository,
        private val settingsHelper: SettingsHelper
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return RegisteredViewModel(
                contentResolver,
                userRepository,
                contactRepository,
                settingsHelper
            ) as T
        }
    }

    companion object {
        private const val TAG = "RegisteredViewModel"

        private val PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
    }
}