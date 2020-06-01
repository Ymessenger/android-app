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
import androidx.lifecycle.*
import org.json.JSONObject
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.remote.entities.User
import org.ymessenger.app.data.remote.entities.UserPhone
import org.ymessenger.app.data.remote.requests.Login
import org.ymessenger.app.data.remote.requests.NewUser
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.Tokens
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.data.repositories.VerificationRepository
import org.ymessenger.app.helpers.AuthorizationManager
import org.ymessenger.app.helpers.StringHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import org.ymessenger.app.utils.ValidationUtils

class SignUpViewModel(
    private val userRepository: UserRepository,
    private val authorizationManager: AuthorizationManager,
    private val verificationRepository: VerificationRepository,
    private val stringHelper: StringHelper
) : BaseViewModel() {

    val registerDoneEvent = SingleLiveEvent<Void>()

    private var connected: Boolean = false

    private var currentNode: Node? = null
    // FIXME: This is shit, think how it can be done in better way
    private var fixedRegisterMethod = false

    val firstName = MutableLiveData<String>()
    val firstNameInputted = Transformations.map(firstName) {
        it.isNotBlank()
    }

    val lastName = MutableLiveData<String>()
    val phoneCode = MutableLiveData<String>()
    val phoneNumber = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val verificationCode = MutableLiveData<String>()

    val isUserRegistrationAllowed = MutableLiveData<Boolean>()

    private var registerInfo = MutableLiveData(RegisterInfo.None)

    val showPhone = Transformations.map(registerInfo) {
        it != RegisterInfo.Email
    }
    val showEmail = Transformations.map(registerInfo) {
        it != RegisterInfo.Phone
    }
    val showOrLabel = Transformations.map(registerInfo) {
        it == RegisterInfo.None
    }

    val showButtonSendVCode = MediatorLiveData<Boolean>()

    val showVerificationCode = MediatorLiveData<Boolean>()

    private val verificationCodeWasSent = MutableLiveData(false)

    val registerAvailable = MediatorLiveData<Boolean>()
    private val requiredFieldsAreFilled = MutableLiveData<Boolean>().apply { value = false }

    val inputErrorList = MutableLiveData<List<InputError>>()

    enum class RegisterInfo {
        None,
        Phone,
        Email
    }

    init {
        registerAvailable.addSource(requiredFieldsAreFilled) {
            registerAvailable.postValue(it)
        }

        firstName.observeForever {
            checkForRequiredFields()
        }

        phoneCode.observeForever {
            if (it.isNotBlank()) {
                setRegisterInfo(RegisterInfo.Phone)
            } else if (!fixedRegisterMethod) {
                setRegisterInfo(RegisterInfo.None)
            }
            checkForRequiredFields()
        }

        phoneNumber.observeForever {
            if (it.isNotBlank()) {
                setRegisterInfo(RegisterInfo.Phone)
            } else if (!fixedRegisterMethod) {
                setRegisterInfo(RegisterInfo.None)
            }
            checkForRequiredFields()
        }

        email.observeForever {
            if (it.isNotBlank()) {
                setRegisterInfo(RegisterInfo.Email)
            } else if (!fixedRegisterMethod) {
                setRegisterInfo(RegisterInfo.None)
            }
            checkForRequiredFields()
        }

        verificationCode.observeForever {
            checkForRequiredFields()
        }

        showButtonSendVCode.addSource(email) {
            showButtonSendVCode.postValue(verifyEmail() && !verifyCode())
        }

        showButtonSendVCode.addSource(phoneCode) {
            showButtonSendVCode.postValue(verifyPhone() && !verifyCode())
        }

        showButtonSendVCode.addSource(phoneNumber) {
            showButtonSendVCode.postValue(verifyPhone() && !verifyCode())
        }

        showButtonSendVCode.addSource(verificationCode) {
            when (registerInfo.value) {
                RegisterInfo.Phone -> {
                    showButtonSendVCode.postValue(verifyPhone() && !verifyCode())
                }
                RegisterInfo.Email -> {
                    showButtonSendVCode.postValue(verifyEmail() && !verifyCode())
                }
                else -> {
                    showButtonSendVCode.postValue(false)
                }
            }
        }

        showVerificationCode.addSource(verificationCodeWasSent) {
            showVerificationCode.postValue(it && registerInfo.value != RegisterInfo.None)
        }

        showVerificationCode.addSource(registerInfo) {
            showVerificationCode.postValue(verificationCodeWasSent.value == true && it != RegisterInfo.None)
        }
    }

    private fun setRegisterInfo(registerInfo: RegisterInfo) {
        hideVCode()

        if (this.registerInfo.value == registerInfo) return

        this.registerInfo.postValue(registerInfo)
        showInputErrors(listOf())
    }

    private fun hideVCode() {
        verificationCode.postValue("")
        verificationCodeWasSent.postValue(false)
    }

    private fun checkForRequiredFields() {
        var allIsFilled = false

        if (registerInfo.value == RegisterInfo.Phone) {
            if (verifyFirstName() && verifyPhone() && verifyCode()) {
                allIsFilled = true
            }
        } else if (registerInfo.value == RegisterInfo.Email) {
            if (verifyFirstName() && verifyEmail() && verifyCode()) {
                allIsFilled = true
            }
        }

        requiredFieldsAreFilled.postValue(allIsFilled)
    }

    private fun verifyFirstName(): Boolean {
        var verified = false

        if (firstName.value?.isNotBlank() == true) {
            verified = true
        }

        return verified
    }

    private fun verifyPhone(): Boolean {
        var verified = false

        if (phoneCode.value?.isNotBlank() == true && phoneNumber.value?.isNotBlank() == true) {
            verified = true
        }

        return verified
    }

    private fun verifyEmail(): Boolean {
        var verified = false

        verified = ValidationUtils.validateEmail(email.value)

        return verified
    }

    private fun verifyCode(): Boolean {
        var verified = false

        if (verificationCode.value?.length == 4) {
            verified = true
        }

        return verified
    }

    fun sendVerificationCode() {
        val registerInfo = registerInfo.value!!

        if (registerInfo == RegisterInfo.None) {
            showError(R.string.unknown_error)
            return
        }

        if (registerInfo == RegisterInfo.Phone) {
            if (!verifyPhone()) {
                showError(R.string.wrong_phone_format)
                return
            }

            val phone = phoneCode.value.toString() + phoneNumber.value.toString()

            startLoading(R.string.sending_verification_code)
            verificationRepository.sendVCodeToPhone(phone, true, object : SuccessErrorCallback {
                override fun success() {
                    endLoading()
                    verificationCodeWasSent.postValue(true)
                    showToast(R.string.verification_code_was_sent_to_your_phone)
                }

                override fun error(error: ResultResponse) {
                    endLoading()
                    errorSendVCode(error)
                }
            })

        } else {
            if (!verifyEmail()) {
                showError(R.string.email_is_incorrect)
                return
            }

            val email = email.value.toString().trim()

            startLoading(R.string.sending_verification_code)
            verificationRepository.sendVCodeToEmail(email, true, object : SuccessErrorCallback {
                override fun success() {
                    endLoading()
                    verificationCodeWasSent.postValue(true)
                    showToast(R.string.verification_code_was_sent_to_your_email)
                }

                override fun error(error: ResultResponse) {
                    endLoading()
                    errorSendVCode(error)
                }
            })
        }
    }

    private fun errorSendVCode(error: ResultResponse) {
        try {
            val jsonError = JSONObject(error.message)
            when {
                jsonError.has("Email") -> {
                    // This email already exists
                    showError(R.string.email_already_exists)
                }
                jsonError.has("Phone") -> {
                    // This phone already exists
                    showError(R.string.phone_already_exists)
                }
                else -> {
                    throw Exception()
                }
            }
        } catch (e: Exception) {
            showError(R.string.failed_to_send_verification_code)
        }
    }

    fun signUp() {
        if (!hasConnection()) return

        if (registerInfo.value == RegisterInfo.None) {
            showError(R.string.unknown_error)
            return
        }

        val firstName = firstName.value
        val secondName = lastName.value
        val countryCode = phoneCode.value?.let { it.toIntOrNull() }
        val phone = phoneNumber.value?.let { it.toLongOrNull() }
        val email = email.value

        var phones: List<UserPhone>? = null

        if (registerInfo.value == RegisterInfo.Phone && countryCode != null && phone != null) {
            val userPhone = UserPhone(countryCode, phone)
            phones = listOf(userPhone)
        }

        var emails: List<String>? = null
        if (registerInfo.value == RegisterInfo.Email) {
            email?.let {
                emails = listOf(it)
            }
        }

        val vcode = verificationCode.value?.let { it.toIntOrNull() }

        // Open all user privacy
        val userPrivacy = BooleanArray(22)
        userPrivacy[1] = true
        userPrivacy[2] = true
        userPrivacy[14] = true
        userPrivacy[15] = true
        userPrivacy[17] = true

        val user = User(
            null,
            firstName,
            secondName,
            null,
            null,
            null,
            null,
            phones,
            emails,
            null,
            null,
            userPrivacy,
            null,
            null,
            false,
            false,
            false
        )
        val newUser = NewUser(user, vcode)
        startLoading(R.string.signing_up)

        userRepository.signUp(newUser, object : UserRepository.RegisterCallback {
            override fun registered(tokens: Tokens) {
                showInputErrors(listOf())
                authorize(tokens)
            }

            override fun error(error: ResultResponse) {
                endLoading()
                handleInputErrors(error)
                showError(R.string.failed_to_sign_up)
            }
        })
    }

    private fun handleInputErrors(error: ResultResponse) {
        Log.d(TAG, "Error message: ${error.message}")
        val inputErrors = mutableListOf<InputError>()

        try {
            val jsonObject = JSONObject(error.message)
            for (key in jsonObject.keys()) {
                val value = jsonObject.getString(key)
                Log.e(TAG, "$key - $value")
                val input = getInputByName(key)
                if (input == null) {
                    Log.e(TAG, "No such input '$key'")
                } else {
                    val description = stringHelper.getStringByName(value)
                    inputErrors.add(InputError(input, description))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error message is not a JSON")
            e.printStackTrace()
        }

        showInputErrors(inputErrors)
    }

    private fun showInputErrors(inputErrors: List<InputError>) {
        inputErrorList.postValue(inputErrors)
    }

    private fun authorize(tokens: Tokens) {
        val loginRequest = Login(tokens.token)
        authorizationManager.authorize(loginRequest, object : AuthorizationManager.Callback {
            override fun authorized() {
                endLoading()
                registerDoneEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showError(R.string.failed_to_sign_in)
            }
        })
    }

    class InputError(
        val input: Input,
        val errorDescription: String
    )

    enum class Input {
        Phone,
        Email,
        VCode
    }

    private fun getInputByName(name: String): Input? {
        return when (name) {
            "Phone" -> Input.Phone
            "Email" -> Input.Email
            "VCode" -> Input.VCode
            else -> null
        }
    }

    fun setCurrentNode(node: Node?) {
        this.currentNode = node

        isUserRegistrationAllowed.postValue(node?.userRegistrationAllowed)

        node?.let {
            when (it.registrationMethod) {
                Node.RegistrationMethod.NOTHING_REQUIRED -> {
                    fixedRegisterMethod = false
                    setRegisterInfo(RegisterInfo.None)
                }
                Node.RegistrationMethod.PHONE_REQUIRED -> {
                    fixedRegisterMethod = true
                    setRegisterInfo(RegisterInfo.Phone)
                }
                Node.RegistrationMethod.EMAIL_REQUIRED -> {
                    fixedRegisterMethod = true
                    setRegisterInfo(RegisterInfo.Email)
                }
            }
        }
    }

    fun setConnected(status: Boolean) {
        connected = status
    }

    private fun hasConnection(): Boolean {
        if (!connected) {
            showError(R.string.connection_is_lost_try_later)
        }

        return connected
    }

    class Factory(
        private val userRepository: UserRepository,
        private val authorizationManager: AuthorizationManager,
        private val verificationRepository: VerificationRepository,
        private val stringHelper: StringHelper
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SignUpViewModel(
                userRepository,
                authorizationManager,
                verificationRepository,
                stringHelper
            ) as T
        }
    }

    companion object {
        private const val TAG = "SignUpViewModel"
    }

}