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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.models.PhoneContact

class PhoneContactsViewModel(
    private val contentResolver: ContentResolver
) : BaseViewModel() {

    val contactList = MutableLiveData<List<PhoneContact>>()

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

    class Factory(
        private val contentResolver: ContentResolver
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PhoneContactsViewModel(contentResolver) as T
        }
    }

    companion object {
        private const val TAG = "PhoneContactsViewModel"

        private val PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
    }
}