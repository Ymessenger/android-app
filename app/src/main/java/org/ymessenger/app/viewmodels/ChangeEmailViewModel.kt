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
import android.view.KeyEvent
import android.widget.TextView
import androidx.lifecycle.*
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.data.repositories.VerificationRepository
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import org.ymessenger.app.utils.ValidationUtils

class ChangeEmailViewModel(
    private val userId: Long,
    private val userRepository: UserRepository,
    private val verificationRepository: VerificationRepository
) : BaseViewModel() {

    private val user = userRepository.getUser(userId)

    val oldEmail = Transformations.map(user) {
        it.email
    }

    val newEmail = MutableLiveData<String>()
    val verificationCode = MutableLiveData<String>()

    val showOldEmail = Transformations.map(oldEmail) {
        !it.isNullOrBlank()
    }

    val showButtonSendVCode = MediatorLiveData<Boolean>()

    val codeWasSent = MutableLiveData<Boolean>(false)

    val showVerificationCode = Transformations.map(codeWasSent) {
        it
    }

    val verificationCodeRequestFocus = Transformations.map(codeWasSent) {
        it
    }

    private val emailCorrect = Transformations.map(newEmail) {
        ValidationUtils.validateEmail(it)
    }

    private val verificationCodeFilled = Transformations.map(verificationCode) {
        it.length == 4
    }

    val saveAvailable = Transformations.map(verificationCodeFilled) {
        it
    }

    val sendingVCode = MutableLiveData<Boolean>()

    val doneEvent = SingleLiveEvent<Void>()

    init {
        newEmail.observeForever {
            hideVCode()
        }

        showButtonSendVCode.addSource(emailCorrect) {
            showButtonSendVCode.postValue(it && verificationCodeFilled.value == false)
        }

        showButtonSendVCode.addSource(verificationCodeFilled) {
            showButtonSendVCode.postValue(emailCorrect.value == true && !it)
        }
    }

    private fun hideVCode() {
        verificationCode.postValue("")
        codeWasSent.postValue(false)
    }

    fun sendVerificationCode() {
        val email = newEmail.value ?: return

        sendingVCode.postValue(true)
        verificationRepository.sendVCodeToEmail(email, false, object : SuccessErrorCallback {
            override fun success() {
                // Code was sent
                sendingVCode.postValue(false)
                codeWasSent.postValue(true)
            }

            override fun error(error: ResultResponse) {
                Log.e(TAG, "Failed to send code")
                sendingVCode.postValue(false)
                showError(R.string.failed_to_send_verification_code)
            }
        })
    }

    fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
        save()
        return false
    }

    fun save() {
        val email = newEmail.value.toString().trim()
        val vCode = verificationCode.value?.toIntOrNull() ?: return

        startLoading()
        userRepository.editPhoneOrEmail(email, vCode, object : SuccessErrorCallback {
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
        private val userRepository: UserRepository,
        private val verificationRepository: VerificationRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChangeEmailViewModel(
                userId,
                userRepository,
                verificationRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "EditEmailViewModel"
    }
}