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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.SymmetricKey
import org.ymessenger.app.data.repositories.KeysRepository
import org.ymessenger.app.data.repositories.SymmetricKeyRepository
import org.ymessenger.app.helpers.EncryptionWrapper
import java.io.File

class BaseSecureViewerViewModel(
    private val keysRepository: KeysRepository,
    private val symmetricKeyRepository: SymmetricKeyRepository,
    private val encryptionWrapper: EncryptionWrapper
) : BaseViewModel() {

    val decryptedBytes = MutableLiveData<ByteArray>()

    fun decryptFile(
        file: File,
        senderId: Long,
        keyId: Long,
        signKeyId: Long
    ) {
        // 1. Get symmetric key
        // 2. Get sign key
        // 3. Decrypt file

        startLoading(R.string.file_decryption)
        symmetricKeyRepository.getSymmetricKey(
            keyId,
            object : SymmetricKeyRepository.GetKeyCallback {
                override fun result(symmetricKey: SymmetricKey) {
                    keysRepository.getKeysByUser(senderId, signKeyId) {
                        if (it != null) {
                            try {
                                val yEncrypt = encryptionWrapper.getYEncrypt()

                                yEncrypt.setSymmetricEncryptKey(symmetricKey.data)
                                yEncrypt.publicSignKeyToReceive = it.publicKey
                                val data = yEncrypt.decryptSecretMsg(file.readBytes())
                                endLoading()
                                decryptedBytes.postValue(data.msg)
                            } catch (e: Exception) {
                                endLoading()
                                showError(R.string.unknown_error)
                            }
                        } else {
                            Log.e(TAG, "There is no sign key")
                            endLoading()
                            showError(R.string.there_is_no_sign_key_to_decrypt)
                        }
                    }
                }

                override fun keyNotFound() {
                    Log.e(TAG, "There is no symmetric key")
                    endLoading()
                    showError(R.string.there_is_no_symmetric_key_to_decrypt)
                }
            })
    }

    class Factory(
        private val keysRepository: KeysRepository,
        private val symmetricKeyRepository: SymmetricKeyRepository,
        private val encryptionWrapper: EncryptionWrapper
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BaseSecureViewerViewModel(
                keysRepository,
                symmetricKeyRepository,
                encryptionWrapper
            ) as T
        }
    }

    companion object {
        private const val TAG = "BaseSecureViewerVM"
    }
}