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
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.remote.entities.Channel
import org.ymessenger.app.data.remote.entities.ChannelUser
import org.ymessenger.app.data.remote.entities.Chat
import org.ymessenger.app.data.remote.entities.ChatUser
import org.ymessenger.app.data.remote.requests.CreateChannel
import org.ymessenger.app.data.remote.requests.NewChats
import org.ymessenger.app.data.repositories.ChannelRepository
import org.ymessenger.app.data.repositories.ChatRepository
import org.ymessenger.app.data.repositories.ContactRepository
import org.ymessenger.app.data.repositories.FileRepository
import org.ymessenger.app.utils.SingleLiveEvent
import java.io.File

class CreateGroupViewModel(
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileRepository
) : BaseViewModel() {
    val name = MutableLiveData<String>()
    val privateChat = MutableLiveData<Boolean>()
    val createChannel = MutableLiveData<Boolean>()

    private val contactsId = MutableLiveData<ArrayList<String>>()
    val contactList = Transformations.switchMap(contactsId) {
        contactRepository.getContactModelsById(it)
    }

    val selectedContactsId = MutableLiveData<ArrayList<String>>()

    val createdEvent = SingleLiveEvent<Pair<Long, Int>>()

    private var photoId: String? = null

    private var connected: Boolean = false

    init {
        privateChat.postValue(false)
        createChannel.postValue(false)
    }

    fun submitGroupContacts(newContactsId: List<String>) {
        val tmpContactsId = contactsId.value ?: arrayListOf()
        for (contactId in newContactsId) {
            if (!tmpContactsId.contains(contactId)) {
                tmpContactsId.add(contactId)
            }
        }

        contactsId.postValue(tmpContactsId)
    }

    fun addRemoveSelectedContact(contactId: String) {
        val selectedContacts = selectedContactsId.value ?: arrayListOf()
        if (selectedContacts.contains(contactId)) {
            selectedContacts.remove(contactId)
        } else {
            selectedContacts.add(contactId)
        }
        selectedContactsId.postValue(selectedContacts)
    }

    fun uploadFile(file: File, success: (fileId: String) -> Unit) {
        fileRepository.uploadFile(file, false, object : FileRepository.UploadFileCallback {
            override fun uploaded(file: org.ymessenger.app.data.remote.responses.File) {
                success(file.fileInfo.fileId)
            }

            override fun errorLargeSize() {
                showError(R.string.file_size_is_too_large_error)
            }

            override fun error() {
                showError(R.string.file_upload_error)
            }
        })
    }

    fun setPhotoId(fileId: String?) {
        photoId = fileId
    }

    fun saveGroup() {
        if (!hasConnection()) return

        when {
            privateChat.value!! && createChannel.value!! -> showError(R.string.operation_is_not_supported) //Chat.Type.PRIVATE_CHANNEL
            createChannel.value!! -> createChannel() //Chat.Type.CHANNEL
            privateChat.value!! -> createChat(Chat.Type.PRIVATE)
            else -> createChat(Chat.Type.PUBLIC)
        }
    }

    private fun createChat(chatType: Int) {
        val chatUsers = arrayListOf<ChatUser>()
        for (contactModel in contactList.value ?: listOf()) {
            chatUsers.add(
                ChatUser(
                    null,
                    contactModel.contact.userId,
                    false,
                    false,
                    0,
                    null
                )
            )
        }

        val chat = Chat(
            null,
            name.value!!.trim(),
            null,
            photoId,
            null,
            null,
            chatType,
            chatUsers
        )

        val newChats = NewChats(listOf(chat))

        startLoading(R.string.creating_of_chat)
        chatRepository.createChat(newChats, object : ChatRepository.CreateChatCallback {
            override fun created(chatId: Long) {
                Log.d(TAG, "Created chat with id $chatId")
                endLoading()
                createdEvent.postValue(chatId to ConversationType.CHAT)
            }

            override fun error() {
                Log.d(TAG, "Error while creating chat")
                endLoading()
                showError(R.string.failed_to_create_chat)
            }

        })
    }

    private fun createChannel() {
        val channelUsers = arrayListOf<ChannelUser>()
        for (contactModel in contactList.value ?: listOf()) {
            channelUsers.add(
                ChannelUser(
                    null,
                    contactModel.contact.userId,
                    ChannelUser.ChannelUserRole.SUBSCRIBER,
                    false,
                    false
                )
            )
        }

        val channel = Channel(
            null,
            name.value!!.trim(),
            null,
            null,
            photoId,
            null,
            null
        )

        val createChannel = CreateChannel(channel, channelUsers)

        startLoading(R.string.creating_of_chat)
        channelRepository.createChannel(
            createChannel,
            object : ChannelRepository.CreateChannelCallback {
                override fun created(channelId: Long) {
                    Log.d(TAG, "Created channel with id $channelId")
                    endLoading()
                    createdEvent.postValue(channelId to ConversationType.CHANNEL)
                }

                override fun error() {
                    Log.d(TAG, "Error while creating channel")
                    endLoading()
                    showError(R.string.failed_to_create_channel)
                }

            })
    }

    fun deleteMembers() {
        val _selectedContactsId = selectedContactsId.value ?: return
        val _contactsId = contactsId.value ?: return
        val contactsIdToPost = arrayListOf<String>().apply { addAll(_contactsId) }
        for (contactId in _contactsId) {
            if (_selectedContactsId.contains(contactId)) {
                contactsIdToPost.remove(contactId)
            }
        }
        contactsId.postValue(contactsIdToPost)
        selectedContactsId.postValue(arrayListOf())
    }

    fun setConnected(status: Boolean) {
        connected = status
    }

    private fun hasConnection(): Boolean {
        if (!connected) {
            showError(R.string.connection_is_lost)
        }

        return connected
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val channelRepository: ChannelRepository,
        private val contactRepository: ContactRepository,
        private val fileRepository: FileRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CreateGroupViewModel(
                chatRepository,
                channelRepository,
                contactRepository,
                fileRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "CreateGroupViewModel"
    }
}