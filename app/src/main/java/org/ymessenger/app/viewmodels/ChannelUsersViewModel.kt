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
import androidx.paging.PagedList
import org.ymessenger.app.data.local.db.models.ChannelUserModel
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.ChannelUser
import org.ymessenger.app.data.remote.responses.ChannelUsers
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ChannelRepository
import org.ymessenger.app.data.repositories.ChannelUserRepository
import org.ymessenger.app.data.repositories.ContactRepository
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.utils.SingleLiveEvent

class ChannelUsersViewModel(
    private val channelId: Long,
    private val channelRepository: ChannelRepository,
    private val channelUserRepository: ChannelUserRepository,
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository
) : BaseViewModel() {

    private val updateChannelEvent = SingleLiveEvent<Void>()
    val channel = Transformations.switchMap(updateChannelEvent) {
        channelRepository.getChannel(channelId)
    }

    private val repoResult = MutableLiveData<Listing<ChannelUserModel>>().apply {
        postValue(channelUserRepository.getChannelUsers(channelId))
    }

    private val channelUsersResult = Transformations.switchMap(repoResult) {
        it.pagedList
    }

    val channelUsers = Transformations.map(channelUsersResult) {
        checkForNullUsers(it)
        it
    }

    val refreshState = Transformations.switchMap(repoResult) { it.refreshState }

    private val contactsIdToAdd = MutableLiveData<List<String>>()
    val contactsToAdd = Transformations.switchMap(contactsIdToAdd) {
        contactRepository.getContactModelsById(it)
    }

    init {
        updateChannelEvent.call()
        channelUserRepository.deleteChannelUsersFrom(channelId)
    }

    private fun checkForNullUsers(channelUsers: PagedList<ChannelUserModel>) {
        val usersId = hashSetOf<Long>()
        channelUsers.forEach {
            if (it.getUser() == null) {
                usersId.add(it.channelUser.userId)
            }
        }
        if (usersId.isNotEmpty()) {
            userRepository.getUsers(usersId.toList())
        }
    }

    fun refresh() {
        updateChannelEvent.call()
        repoResult.value?.refresh?.invoke()
    }

    fun promoteToAdmin(channelUserModel: ChannelUserModel) {
        val channelUser = ChannelUser(
            channelUserModel.channelUser.channelId,
            channelUserModel.channelUser.userId,
            ChannelUser.ChannelUserRole.ADMINISTRATOR,
            false,
            null
        )

        channelUserRepository.editChannelUsers(
            listOf(channelUser),
            channelId,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    Log.d(TAG, "Admin rights was granted")
                }

                override fun onError(error: ResultResponse) {
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    fun dismissAdminRights(channelUserModel: ChannelUserModel) {
        val channelUser = ChannelUser(
            channelUserModel.channelUser.channelId,
            channelUserModel.channelUser.userId,
            ChannelUser.ChannelUserRole.SUBSCRIBER,
            false,
            null
        )

        channelUserRepository.editChannelUsers(
            listOf(channelUser),
            channelId,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    Log.d(TAG, "Admin rights was dismissed")
                }

                override fun onError(error: ResultResponse) {
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    fun removeUserFromChannel(channelUserModel: ChannelUserModel) {
        val channelUser = ChannelUser(
            channelUserModel.channelUser.channelId,
            channelUserModel.channelUser.userId,
            null,
            true,
            null
        )

        channelUserRepository.editChannelUsers(
            listOf(channelUser),
            channelId,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    Log.d(TAG, "User was removed from channel")
                    channelUserRepository.deleteChannelUser(channelUserModel.channelUser)
                    updateChannelEvent.call()
                }

                override fun onError(error: ResultResponse) {
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    fun addContactsToChannel(contactsId: List<String>) {
        contactsIdToAdd.postValue(contactsId)
    }

    fun addUsersToChannel(usersId: List<Long>) {
        channelUserRepository.addUsersToChannels(
            usersId,
            listOf(channelId),
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    Log.d(TAG, "Users added to channel")
                    refresh()
                }

                override fun onError(error: ResultResponse) {
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val channelId: Long,
        private val channelRepository: ChannelRepository,
        private val channelUserRepository: ChannelUserRepository,
        private val userRepository: UserRepository,
        private val contactRepository: ContactRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChannelUsersViewModel(
                channelId,
                channelRepository,
                channelUserRepository,
                userRepository,
                contactRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "ChannelUsersVM"
    }

}