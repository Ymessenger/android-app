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
import org.ymessenger.app.data.Limitations
import org.ymessenger.app.utils.SingleLiveEvent

class EnterPassphraseViewModel : BaseViewModel() {

    val passphraseLength = MutableLiveData<Int>()

    val setupYEncryptEvent = SingleLiveEvent<String>()

    private var passphrase: String = ""

    fun setPassphrase(passphrase: String) {
        this.passphrase = passphrase

        val passphraseWithoutSpaces = passphrase.replace(" +".toRegex(), "")
        passphraseLength.value = passphraseWithoutSpaces.length
    }

    fun enter() {
        if (passphraseLength.value ?: 0 < Limitations.MIN_PASSPHRASE_LENGTH) {
            showError(R.string.passphrase_is_too_short)
            return
        }

        setupYEncryptEvent.postValue(passphrase)
    }

    class Factory() : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EnterPassphraseViewModel() as T
        }
    }

    companion object {
        private const val TAG = "EnterPassphraseViewModel"
    }

}