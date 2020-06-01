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
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.requests.Login
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.VerificationRepository
import org.ymessenger.app.helpers.AuthorizationManager
import org.ymessenger.app.helpers.ValuesHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import org.ymessenger.app.utils.ValidationUtils

class EmailLoginViewModel(
    private val authorizationManager: AuthorizationManager,
    private val verificationRepository: VerificationRepository,
    private val valuesHelper: ValuesHelper
) : BaseViewModel() {

    private val status = SingleLiveEvent<Status>()
    private val state = MutableLiveData<State>()
    val loading = MutableLiveData<Boolean>()
    val emailEnabled = MutableLiveData<Boolean>()

    private var email: String? = null

    private val verificationCodeLength = valuesHelper.getVCodeLength()

    private var isConnected: Boolean = false

    init {
        state.postValue(State())
    }

    fun getStatus() = status

    fun getState() = state

    fun setCodeSent(codeSent: Boolean) {
        state.value?.codeSent = codeSent
    }

    fun sendVerificationCode(email: String) {
        if (!ValidationUtils.validateEmail(email)) {
            status.postValue(Status.WRONG_EMAIL_FORMAT)
            return
        }

        if (!hasConnection()) return

        emailEnabled.postValue(false)
        this.email = email
        loading.postValue(true)
        verificationRepository.sendVCodeToEmail(email, false, object : SuccessErrorCallback {
            override fun success() {
                loading.postValue(false)
                status.postValue(Status.CODE_SENT)
            }

            override fun error(error: ResultResponse) {
                emailEnabled.postValue(true)
                loading.postValue(false)
                if (error.errorCode == WSResponse.ErrorCode.PERMISSION_DENIED &&
                    error.message?.contains("not support") == true
                ) {
                    showError(R.string.server_does_not_support_speficied_type_of_verification)
                } else if (error.errorCode == WSResponse.ErrorCode.WRONG_ARGUMENT_ERROR &&
                    error.message?.contains("not exist") == true
                ) {
                    status.postValue(Status.EMAIL_NOT_FOUND)
                } else {
                    status.postValue(Status.ERROR)
                }
            }
        })

    }

    fun authorize(code: String) {
        if (code.length < verificationCodeLength) {
            status.postValue(Status.EMPTY_CODE)
            return
        }

        if (email == null) {
            status.postValue(Status.WRONG_EMAIL_FORMAT)
            return
        }

        if (!hasConnection()) return

        val loginRequest = Login(email!!, Login.UID_TYPE_EMAIL, code.toInt())
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
        ERROR,
        WRONG_EMAIL_FORMAT,
        EMPTY_CODE,
        EMAIL_NOT_FOUND
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
            return EmailLoginViewModel(
                authorizationManager,
                verificationRepository,
                valuesHelper
            ) as T
        }
    }

}