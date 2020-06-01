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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.remote.entities.EditUser
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.FileRepository
import org.ymessenger.app.data.repositories.UserRepository
import java.io.File

class SettingsViewModel(
    private val userId: Long,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository
) : BaseViewModel() {

    val user: LiveData<User> = userRepository.getUser(userId)

    private var connected: Boolean = false

    fun hasPhoto(): Boolean {
        return !user.value?.photo.isNullOrEmpty()
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
        updateUserPhoto("")
    }

    fun updateUserPhoto(fileId: String) {
        val editUser = EditUser().apply {
            this.photo = fileId
        }
        startLoading(R.string.updating)
        userRepository.editUser(editUser, object : UserRepository.EditCallback {
            override fun success() {
                endLoading()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showError(R.string.unknown_error)
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
        private val userId: Long,
        private val userRepository: UserRepository,
        private val fileRepository: FileRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SettingsViewModel(userId, userRepository, fileRepository) as T
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }

}