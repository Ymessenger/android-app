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
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.EditUser
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.utils.SingleLiveEvent

class ChangeNameViewModel(
    private val userId: Long,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val user = userRepository.getUser(userId)

    val firstName = MutableLiveData<String>()
    val lastName = MutableLiveData<String>()

    val saveButtonAvailable = Transformations.map(firstName) {
        it.isNotBlank()
    }

    val doneEvent = SingleLiveEvent<Void>()

    init {
        user.observeForever {
            if (firstName.value.isNullOrBlank()) {
                firstName.postValue(it.firstName)
            }

            if (lastName.value.isNullOrBlank() && it.lastName?.isNotBlank() == true) {
                lastName.postValue(it.lastName)
            }
        }
    }

    fun save() {
        val firstName = firstName.value.toString().trim()
        val lastName = lastName.value.toString().trim()

        if (firstName.isBlank()) {
            showError(R.string.first_name_must_be_specified)
            return
        }

        startLoading()
        val editUser = EditUser().apply {
            this.nameFirst = firstName
            this.nameSecond = lastName
        }
        userRepository.editUser(editUser, object : UserRepository.EditCallback {
            override fun success() {
                endLoading()
                doneEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    class Factory(
        private val userId: Long,
        private val userRepository: UserRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChangeNameViewModel(
                userId,
                userRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "ChangeAboutViewModel"
    }
}