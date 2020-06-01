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

import android.telephony.PhoneNumberUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.requests.Login
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.VerificationRepository
import org.ymessenger.app.helpers.AuthorizationManager
import org.ymessenger.app.helpers.ValuesHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import org.ymessenger.app.utils.ValidationUtils

class PhoneLoginViewModel(
    private val authorizationManager: AuthorizationManager,
    private val verificationRepository: VerificationRepository,
    private val valuesHelper: ValuesHelper
) : BaseViewModel() {

    private val status = SingleLiveEvent<Status>()
    private val state = MutableLiveData<State>()
    val loading = MutableLiveData<Boolean>()

    private val verificationCodeLength = valuesHelper.getVCodeLength()

    private var isConnected: Boolean = false
    val phoneEnabled = MutableLiveData<Boolean>()

    private var phone: String? = null

    init {
        state.postValue(State())
    }

    fun getStatus() = status

    fun getState() = state

    fun setCodeSent(codeSent: Boolean) {
        state.value?.codeSent = codeSent
    }

    fun sendVerificationCode(phoneNumber: String) {
        if (!ValidationUtils.validatePhoneNumber(phoneNumber)) {
            status.postValue(Status.WRONG_PHONE_NUMBER)
            return
        }

        if (!hasConnection()) return

        phoneEnabled.postValue(false)
        val normalizedPhone = PhoneNumberUtils.normalizeNumber(phoneNumber)
        this.phone = normalizedPhone
        loading.postValue(true)

        verificationRepository.sendVCodeToPhone(
            normalizedPhone,
            false,
            object : SuccessErrorCallback {
                override fun success() {
                    loading.postValue(false)
                    status.postValue(Status.CODE_SENT)
                }

                override fun error(error: ResultResponse) {
                    phoneEnabled.postValue(true)
                    loading.postValue(false)
                    when (error.errorCode) {
                        WSResponse.ErrorCode.SEND_VERIFICATION_CODE_ERROR -> {
                            status.postValue(Status.SEND_VERIFICATION_CODE_ERROR)
                        }
                        WSResponse.ErrorCode.WRONG_ARGUMENT_ERROR -> {
                            if (error.message?.contains("not exist") == true) {
                                status.postValue(Status.PHONE_NOT_FOUND)
                            } else {
                                status.postValue(Status.ERROR)
                            }
                        }
                        WSResponse.ErrorCode.PERMISSION_DENIED -> {
                            // FIXME: do it in better way
                            if (error.message?.contains("not support") == true) {
                                showError(R.string.server_does_not_support_speficied_type_of_verification)
                            } else {
                                status.postValue(Status.ERROR)
                            }
                        }
                        else -> {
                            status.postValue(Status.ERROR)
                        }
                    }
                }
            })
    }

    fun authorize(code: String) {
        if (code.length < verificationCodeLength) {
            status.postValue(Status.EMPTY_CODE)
            return
        }

        if (phone == null) {
            status.postValue(Status.WRONG_PHONE_NUMBER)
            return
        }

        if (!hasConnection()) return

        val loginRequest = Login(phone!!, Login.UID_TYPE_PHONE, code.toInt())
        loading.postValue(true)
        authorizationManager.authorize(loginRequest, object : AuthorizationManager.Callback {
            override fun authorized() {
                loading.postValue(false)
                status.postValue(Status.AUTHORIZED)
            }

            override fun error(error: ResultResponse) {
                loading.postValue(false)
                status.postValue(Status.AUTHORIZATION_ERROR)
            }

        })
    }

    fun setConnectionStatus(isConnected: Boolean) {
        this.isConnected = isConnected
    }

    private fun hasConnection(): Boolean {
        if (!isConnected) {
            showError(R.string.connection_is_lost_try_later)
        }

        return isConnected
    }

    enum class Status {
        CODE_SENT,
        AUTHORIZED,
        AUTHORIZATION_ERROR,
        SEND_VERIFICATION_CODE_ERROR,
        ERROR,
        WRONG_PHONE_NUMBER,
        EMPTY_CODE,
        PHONE_NOT_FOUND
    }

    data class State(
        var codeSent: Boolean = false
    )

    class Factory(
        private val authorizationManager: AuthorizationManager,
        private val verificationRepository: VerificationRepository,
        private val valuesHelper: ValuesHelper
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PhoneLoginViewModel(
                authorizationManager,
                verificationRepository,
                valuesHelper
            ) as T
        }
    }

}