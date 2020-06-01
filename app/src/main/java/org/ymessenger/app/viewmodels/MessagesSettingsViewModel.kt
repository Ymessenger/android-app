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
import org.ymessenger.app.helpers.SettingsHelper

class MessagesSettingsViewModel(
    private val settingsHelper: SettingsHelper
) : BaseViewModel() {

    val saveEncryptedMessagesOnServer =
        MutableLiveData<Boolean>(settingsHelper.getSaveEncryptedMessagesOnServer())

    fun updateSaveEncryptedMessagesOnServer(save: Boolean) {
        settingsHelper.setSaveEncryptedMessagesOnServer(save)
    }

    class Factory(
        private val settingsHelper: SettingsHelper
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MessagesSettingsViewModel(settingsHelper) as T
        }
    }

    companion object {
        private const val TAG = "MessagesSettingsViewModel"
    }

}