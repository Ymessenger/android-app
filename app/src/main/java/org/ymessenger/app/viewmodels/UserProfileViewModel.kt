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
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.FavoriteConversation
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.entities.Contact
import org.ymessenger.app.data.remote.entities.UserPhone
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ContactRepository
import org.ymessenger.app.data.repositories.DialogRepository
import org.ymessenger.app.data.repositories.FavoriteConversationRepository
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import java.util.*

class UserProfileViewModel(
    private val userId: Long,
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val dialogRepository: DialogRepository,
    private val settingsHelper: SettingsHelper,
    private val favoriteConversationRepository: FavoriteConversationRepository
) : BaseViewModel() {

    private val userModel = userRepository.getUserModel(userId, settingsHelper.getSyncContacts())
    private val user = Transformations.map(userModel) {
        it?.let { it.user }
    }
    private val contact = contactRepository.getContactByUser(userId)

    val userName = Transformations.map(userModel) {
        it?.let { it.getDisplayName() }
    }

    val photoLabel = Transformations.map(userModel) {
        it?.let { it.getPhotoLabel() }
    }

    private val favoriteConversation =
        favoriteConversationRepository.getFavoriteConversation(userId, ConversationType.DIALOG)
    val isFavourite = Transformations.map(favoriteConversation) { it != null }

    val isContact = Transformations.map(contact) { it != null }

    val userPhone = Transformations.map(user) {
        it?.let {
            Gson().fromJson(it.phone, UserPhone::class.java)
        }
    }

    val openDialogEvent = SingleLiveEvent<Long>()
    val openPhotoEvent = SingleLiveEvent<String>()
    val openEditContactEvent = SingleLiveEvent<String>()
    val copyTagEvent = SingleLiveEvent<String>()
    val copyPhoneEvent = SingleLiveEvent<String>()
    val copyEmailEvent = SingleLiveEvent<String>()

    private lateinit var timer: Timer

    fun initTimer() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "Timer tick")
                userRepository.getUser(userId, settingsHelper.getSyncContacts())
            }
        }, Config.TIMER_UPDATE_PERIOD, Config.TIMER_UPDATE_PERIOD)
        Log.d(TAG, "Timer started")
    }

    fun cancelTimer() {
        timer.cancel()
        Log.d(TAG, "Timer was cancelled")
    }

    fun getUser() = user

    fun favouriteClick() {
        favoriteConversation.value?.let {
            favoriteConversationRepository.delete(it)
        } ?: favoriteConversationRepository.insert(
            FavoriteConversation(
                userId,
                ConversationType.DIALOG
            )
        )
    }

    fun addContact() {
        startLoading()
        val contact = Contact(userId, user.value?.fullName ?: userId.toString())
        contactRepository.addContact(contact, object : SuccessErrorCallback {
            override fun success() {
                endLoading()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun removeContact() {
        contact.value?.let {
            startLoading()
            contactRepository.deleteContacts(listOf(it.id), object : SuccessErrorCallback {
                override fun success() {
                    endLoading()
                }

                override fun error(error: ResultResponse) {
                    endLoading()
                    showErrorFromCode(error.errorCode)
                }
            })
        }
    }

    fun openDialog() {
        openDialogEvent.postValue(userId)
    }

    fun openPhoto() {
        user.value?.getPhotoUrl()?.let { photoUrl ->
            openPhotoEvent.postValue(photoUrl)
        }
    }

    fun openEditContact() {
        contact.value?.let {
            openEditContactEvent.postValue(it.id)
        }
    }

    fun copyTag() {
        user.value?.tag?.let {
            copyTagEvent.postValue(it)
        }
    }

    fun copyPhone() {
        userPhone.value?.fullNumber?.let {
            copyPhoneEvent.postValue(it)
        }
    }

    fun copyEmail() {
        user.value?.email?.let {
            copyEmailEvent.postValue(it)
        }
    }

    class Factory(
        private val userId: Long,
        private val userRepository: UserRepository,
        private val contactRepository: ContactRepository,
        private val dialogRepository: DialogRepository,
        private val settingsHelper: SettingsHelper,
        private val favoriteConversationRepository: FavoriteConversationRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return UserProfileViewModel(
                userId,
                userRepository,
                contactRepository,
                dialogRepository,
                settingsHelper,
                favoriteConversationRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "UserProfileViewModel"
    }

}