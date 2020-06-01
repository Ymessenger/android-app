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

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.helpers.SafeModeManager
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.utils.SingleLiveEvent

class EnterPinViewModel(
    private val _mode: Mode,
    private val settingsHelper: SettingsHelper,
    private val safeModeManager: SafeModeManager
) : BaseViewModel() {

    val mode = MutableLiveData<Mode>().apply { value = _mode }
    private val pinSB = StringBuilder()
    val pinLength = MutableLiveData<Int>()

    val success = SingleLiveEvent<Void>()
    val safePinEnteredEvent = SingleLiveEvent<Void>()
    val enteredPinError = MutableLiveData<Int?>()

    private var firstEnteredPin: String? = null
    val enterPinAgainEvent = SingleLiveEvent<Void>()

    fun numberClicked(num: Int) {
        if (pinSB.length < PIN_LENGTH) {
            pinSB.append(num)
            updateDots()
            if (pinSB.length == PIN_LENGTH) {
                pinEntered()
            }
        }
    }

    private fun pinEntered() {
        if (_mode == Mode.ENTER) {
            checkPin(pinSB.toString())
        } else {
            if (firstEnteredPin == null) {
                if (_mode == Mode.NEW_SAFE_PIN) {
                    val savedPin = settingsHelper.getPin()
                    if (TextUtils.equals(pinSB.toString(), savedPin)) {
                        enteredPinError.postValue(R.string.this_pin_is_the_same_as_the_main_pin)
                        return
                    }
                }

                firstEnteredPin = pinSB.toString()
                enterPinAgainEvent.call()
                clear()
            } else {
                if (firstEnteredPin != pinSB.toString()) {
                    enteredPinError.postValue(R.string.pins_do_not_match)
                } else {
                    savePin(pinSB.toString())
                }
            }
        }
    }

    private fun savePin(pin: String) {
        if (_mode == Mode.NEW_PIN) {
            settingsHelper.setPin(pin)
        } else if (_mode == Mode.NEW_SAFE_PIN) {
            settingsHelper.setSafePin(pin)
        }
        success.call()
    }

    private fun checkPin(pin: String) {
        val savedPin = settingsHelper.getPin()
        val savedSafePin = settingsHelper.getSafePin()

        val regularPinEntered = TextUtils.equals(pin, savedPin)
        val safePinEntered = TextUtils.equals(pin, savedSafePin)

        when {
            regularPinEntered -> {
                exitSafeMode()
                success.call()
            }
            safePinEntered -> {
                enterSafeMode()
                safePinEnteredEvent.call()
            }
            else -> enteredPinError.postValue(R.string.invalid_pin_entered)
        }
    }

    private fun enterSafeMode() {
        safeModeManager.enterSafeMode()
    }

    private fun exitSafeMode() {
        safeModeManager.exitSafeMode()
    }

    fun clear() {
        pinSB.clear()
        updateDots()
    }

    private fun updateDots() {
        pinLength.postValue(pinSB.length)
        enteredPinError.postValue(null)
    }

    companion object {
        private const val PIN_LENGTH = 4
        private const val TAG = "EnterPinViewModel"
    }

    enum class Mode {
        NEW_PIN,
        NEW_SAFE_PIN,
        ENTER
    }

    class Factory(
        private val mode: Mode,
        private val settingsHelper: SettingsHelper,
        private val safeModeManager: SafeModeManager
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EnterPinViewModel(mode, settingsHelper, safeModeManager) as T
        }
    }

}