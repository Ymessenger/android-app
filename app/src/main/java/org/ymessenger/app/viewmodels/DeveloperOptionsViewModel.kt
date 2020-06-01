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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.utils.SingleLiveEvent

class DeveloperOptionsViewModel(
    private val keysRepository: KeysRepository,
    private val symmetricKeyRepository: SymmetricKeyRepository,
    private val settingsHelper: SettingsHelper,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val repliedMessageRepository: RepliedMessageRepository,
    private val contactRepository: ContactRepository,
    private val contactGroupRepository: ContactGroupRepository
) : BaseViewModel() {

    val firebaseToken = MutableLiveData<String?>()

    val copyFirebaseTokenEvent = SingleLiveEvent<String>()

    init {
        firebaseToken.postValue(settingsHelper.getFirebaseToken())
    }

    fun deleteAsymmetricKeys() {
        Log.d(TAG, "Deleting all asymmetric keys...")
        keysRepository.deleteAllKeys()
        showToast(R.string.done)
    }

    fun deleteSymmetricKeys() {
        Log.d(TAG, "Deleting all symmetric keys...")
        symmetricKeyRepository.deleteAllKeys()
        showToast(R.string.done)
    }

    fun copyFirebaseToken() {
        firebaseToken.value?.let {
            copyFirebaseTokenEvent.postValue(it)
        }
    }

    fun deleteAllUsers() {
        Log.d(TAG, "Deleting all users")
        userRepository.deleteAllLocally()
        showToast(R.string.done)
    }

    fun deleteAllContacts() {
        Log.d(TAG, "Deleting all contacts")
        contactRepository.deleteAllContacts()
        showToast(R.string.done)
    }

    fun deleteAllContactGroups() {
        Log.d(TAG, "Deleting all contact groups")
        contactGroupRepository.deleteAll()
        showToast(R.string.done)
    }

    fun deleteAllChats() {
        Log.d(TAG, "Deleting all chats")
        chatRepository.deleteAllLocally()
        showToast(R.string.done)
    }

    fun deleteAllChannels() {
        Log.d(TAG, "Deleting all channels")
        channelRepository.deleteAllLocally()
        showToast(R.string.done)
    }

    fun deleteAllRepliedMessages() {
        Log.d(TAG, "Deleting all replied messages")
        repliedMessageRepository.deleteAllLocally()
        showToast(R.string.done)
    }

    fun setAlwaysOpenMainActivityAsAfterRegister(value: Boolean) {
        settingsHelper.setAlwaysOpenMainActivityAsAfterRegister(value)
    }

    fun getAlwaysOpenMainActivityAsAfterRegister(): Boolean {
        return settingsHelper.getAlwaysOpenMainActivityAsAfterRegister()
    }

    class Factory(
        private val keysRepository: KeysRepository,
        private val symmetricKeyRepository: SymmetricKeyRepository,
        private val settingsHelper: SettingsHelper,
        private val userRepository: UserRepository,
        private val chatRepository: ChatRepository,
        private val channelRepository: ChannelRepository,
        private val repliedMessageRepository: RepliedMessageRepository,
        private val contactRepository: ContactRepository,
        private val contactGroupRepository: ContactGroupRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DeveloperOptionsViewModel(
                keysRepository,
                symmetricKeyRepository,
                settingsHelper,
                userRepository,
                chatRepository,
                channelRepository,
                repliedMessageRepository,
                contactRepository,
                contactGroupRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "DeveloperOptionsVM"
    }
}