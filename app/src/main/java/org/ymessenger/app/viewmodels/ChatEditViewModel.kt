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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.EditChat
import org.ymessenger.app.data.remote.requests.EditChats
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ChatRepository
import org.ymessenger.app.data.repositories.FileRepository
import org.ymessenger.app.utils.SingleLiveEvent
import java.io.File

class ChatEditViewModel(
    private val chatId: Long,
    private val chatRepository: ChatRepository,
    private val fileRepository: FileRepository
) : BaseViewModel() {
    val chat = chatRepository.getChat(chatId)

    val editedEvent = SingleLiveEvent<Void>()

    private var connected: Boolean = false

    fun hasPhoto(): Boolean {
        return !chat.value?.photo.isNullOrEmpty()
    }

    fun saveChat(name: String, about: String) {
        if (!hasConnection()) return

        val editChat = EditChat(chatId).apply {
            this.name = name
            this.about = about
        }
        val editChats = EditChats(listOf(editChat))
        startLoading(R.string.editing_of_chat)
        chatRepository.editChats(editChats, object : ChatRepository.EditChatCallback {
            override fun edited() {
                endLoading()
                editedEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun removePhoto() {
        updatePhoto("")
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

    fun updatePhoto(fileId: String) {
        val editChat = EditChat(chatId).apply {
            this.photo = fileId
        }
        val editChats = EditChats(listOf(editChat))
        startLoading(R.string.editing_of_chat)
        chatRepository.editChats(editChats, object : ChatRepository.EditChatCallback {
            override fun edited() {
                endLoading()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
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
        private val chatId: Long,
        private val chatRepository: ChatRepository,
        private val fileRepository: FileRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChatEditViewModel(
                chatId,
                chatRepository,
                fileRepository
            ) as T
        }
    }
}