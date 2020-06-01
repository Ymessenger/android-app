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
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Channel
import org.ymessenger.app.data.remote.requests.EditChannel
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ChannelRepository
import org.ymessenger.app.data.repositories.FileRepository
import org.ymessenger.app.utils.SingleLiveEvent
import java.io.File

class ChannelEditViewModel(
    private val channelId: Long,
    private val channelRepository: ChannelRepository,
    private val fileRepository: FileRepository
) : BaseViewModel() {
    val channel = channelRepository.getChannel(channelId)

    val loading = MutableLiveData<Boolean>()

    val editedEvent = SingleLiveEvent<Void>()

    private var connected: Boolean = false

    fun hasPhoto(): Boolean {
        return !channel.value?.photo.isNullOrEmpty()
    }

    fun saveChannel(name: String, about: String) {
        if (!hasConnection()) return

        val channel = Channel(
            channelId,
            name,
            about,
            null,
            null,
            null,
            null
        )
        val editChannel = EditChannel(channel)
        startLoading(R.string.saving)
        channelRepository.editChannel(editChannel, object : ChannelRepository.EditChannelCallback {
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

    fun removePhoto() {
        updatePhoto("")
    }

    fun updatePhoto(fileId: String) {
        val channel = Channel(
            channelId,
            null,
            null,
            null,
            fileId,
            null,
            null
        )
        val editChannel = EditChannel(channel)
        startLoading(R.string.saving)
        channelRepository.editChannel(editChannel, object : ChannelRepository.EditChannelCallback {
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
        private val channelId: Long,
        private val channelRepository: ChannelRepository,
        private val fileRepository: FileRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChannelEditViewModel(
                channelId, channelRepository, fileRepository
            ) as T
        }
    }
}