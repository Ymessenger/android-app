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
import org.ymessenger.app.data.local.db.entities.SymmetricKey
import org.ymessenger.app.data.repositories.SymmetricKeyRepository
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.EncryptionWrapper
import org.ymessenger.app.utils.SingleLiveEvent

class CheckEncryptionKeyViewModel(
    private val conversationId: Long,
    private val encryptionWrapper: EncryptionWrapper,
    private val symmetricKeyRepository: SymmetricKeyRepository
) : BaseViewModel() {

    private val myLastSymmetricKey = symmetricKeyRepository.getLastKeyForDialog(conversationId)

    val symmetricKeyHash = MutableLiveData<String>()

    val openScannerEvent = SingleLiveEvent<Void>()
    val openGalleryEvent = SingleLiveEvent<Void>()
    val openConversationGalleryEvent = SingleLiveEvent<Long>()
    val sendQRCodeEvent = SingleLiveEvent<String>()
    val encryptionKeyIsVerified = SingleLiveEvent<Void>()

    init {
        myLastSymmetricKey.observeForever {
            getHashKey(it)
        }
    }

    private fun getHashKey(symmetricKey: SymmetricKey) {
        try {
            val yEncrypt = encryptionWrapper.getYEncrypt()
            val hashBytes = yEncrypt.getHashKey(1, symmetricKey.data)
            val hashStr = EncryptHelper.bytesToBase64(hashBytes)
            symmetricKeyHash.postValue(hashStr)
        } catch (e: Exception) {
            e.printStackTrace()
            showError(R.string.unknown_error)
        }
    }

    fun sendQRCode() {
        symmetricKeyHash.value?.let {
            sendQRCodeEvent.postValue(it)
        } ?: showError(R.string.unknown_error)
    }

    fun scanQRCode() {
        openScannerEvent.call()
    }

    fun getQRCodeFromMessages() {
        openConversationGalleryEvent.postValue(conversationId)
    }

    fun getQRCodeFromGallery() {
        openGalleryEvent.call()
    }

    fun checkHash(hash: String) {
        val myHash = symmetricKeyHash.value ?: ""

        if (myHash == hash) {
            encryptionKeyIsVerified.call()
        } else {
            showError(R.string.wrong_encryption_key)
        }
    }

    class Factory(
        private val conversationId: Long,
        private val encryptionWrapper: EncryptionWrapper,
        private val symmetricKeyRepository: SymmetricKeyRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CheckEncryptionKeyViewModel(
                conversationId,
                encryptionWrapper,
                symmetricKeyRepository
            ) as T
        }
    }

}