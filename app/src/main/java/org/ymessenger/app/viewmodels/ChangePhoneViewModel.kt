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
import com.google.gson.Gson
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.UserPhone
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.data.repositories.VerificationRepository
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import org.ymessenger.app.utils.ValidationUtils

class ChangePhoneViewModel(
    private val userId: Long,
    private val userRepository: UserRepository,
    private val verificationRepository: VerificationRepository
) : BaseViewModel() {

    private val user = userRepository.getUser(userId)

    val oldPhone = Transformations.map(user) {
        it?.let {
            val userPhone = Gson().fromJson(it.phone, UserPhone::class.java)
            userPhone?.fullNumber
        }
    }

    val newPhone = MutableLiveData<String>()
    val verificationCode = MutableLiveData<String>()

    val showOldPhone = Transformations.map(oldPhone) {
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

    private val phoneCorrect = Transformations.map(newPhone) {
        ValidationUtils.validatePhoneNumber(it)
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
        newPhone.observeForever {
            hideVCode()
        }

        showButtonSendVCode.addSource(phoneCorrect) {
            showButtonSendVCode.postValue(it && verificationCodeFilled.value == false)
        }

        showButtonSendVCode.addSource(verificationCodeFilled) {
            showButtonSendVCode.postValue(phoneCorrect.value == true && !it)
        }
    }

    private fun hideVCode() {
        verificationCode.postValue("")
        codeWasSent.postValue(false)
    }

    fun sendVerificationCode() {
        val phone = newPhone.value ?: return

        sendingVCode.postValue(true)
        verificationRepository.sendVCodeToPhone(phone, false, object : SuccessErrorCallback {
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
        val phone = newPhone.value.toString().trim()
        val vCode = verificationCode.value?.toIntOrNull() ?: return

        startLoading()
        userRepository.editPhoneOrEmail(phone, vCode, object : SuccessErrorCallback {
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
            return ChangePhoneViewModel(
                userId,
                userRepository,
                verificationRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "ChangePhoneViewModel"
    }
}