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
import org.ymessenger.app.data.mappers.PrivacyConverter
import org.ymessenger.app.data.remote.entities.EditUser
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.utils.SingleLiveEvent

class SecuritySettingsViewModel(
    private val userId: Long,
    private val userRepository: UserRepository,
    private val settingsHelper: SettingsHelper
) : BaseViewModel() {

    val user = userRepository.getUserLocal(userId)

    private val oldPrivacy = Transformations.map(user) {
        it?.let {
            PrivacyConverter.toBooleanArray(it.privacy)
        }
    }

    val loading = MutableLiveData<Boolean>()
    val editedEvent = SingleLiveEvent<Void>()

    fun saveUserPrivacy(
        nameAndTag: Boolean,
        online: Boolean,
        phone: Boolean,
        email: Boolean,
        photoAndAbout: Boolean
    ) {
        val userPrivacy = PrivacyConverter.toBooleanArray(user.value!!.privacy) ?: BooleanArray(22)
        userPrivacy[1] = nameAndTag
        userPrivacy[2] = photoAndAbout
        userPrivacy[14] = online
        userPrivacy[15] = phone
        userPrivacy[17] = email

        userRepository.editUserPrivacy(userId, userPrivacy, oldPrivacy.value, object : UserRepository.EditCallback {
            override fun success() {
                editedEvent.call()
            }

            override fun error(error: ResultResponse) {
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun setSyncContacts(syncContacts: Boolean) {
        settingsHelper.setSyncContacts(syncContacts)

        val editUser = EditUser()
        editUser.syncContacts = syncContacts
        userRepository.editUser(editUser, object : UserRepository.EditCallback {
            override fun success() {
                Log.d(TAG, "Sync contacts settings was updated")
            }

            override fun error(error: ResultResponse) {
                Log.e(TAG, "Failed to update sync contacts settings")
            }
        })
    }

    class Factory(
        private val userId: Long,
        private val userRepository: UserRepository,
        private val settingsHelper: SettingsHelper
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SecuritySettingsViewModel(userId, userRepository, settingsHelper) as T
        }
    }

    companion object {
        private const val TAG = "SecuritySettingsVM"
    }

}