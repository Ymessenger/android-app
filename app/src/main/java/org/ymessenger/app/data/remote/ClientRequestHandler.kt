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

package org.ymessenger.app.data.remote

import android.util.Log
import com.google.gson.Gson
import org.ymessenger.app.data.local.db.entities.Keys
import org.ymessenger.app.data.mappers.KeysMapper
import org.ymessenger.app.data.remote.clientRequests.GetKeys
import org.ymessenger.app.data.remote.clientResponses.EncryptedData
import org.ymessenger.app.data.remote.clientResponses.Error
import org.ymessenger.app.data.repositories.KeysRepository
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.EncryptionWrapper
import org.ymessenger.app.helpers.KeysGeneratorHelper

class ClientRequestHandler(
    private val encryptionWrapper: EncryptionWrapper,
    private val keysRepository: KeysRepository,
    private val keysGeneratorHelper: KeysGeneratorHelper,
    private val webSocketService: WebSocketService,
    private val keysMapper: KeysMapper
) {

    fun getKeys(getKeys: GetKeys) {
        if (!encryptionWrapper.isInitialized()) {
            sendError("YEncrypt is not initialized", getKeys)
            return
        }

        val yEncrypt = try {
            encryptionWrapper.getYEncrypt()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // 1. Get all my private keys
        // 2. Generate symmetric key
        // 3. Encrypt PrivateKeys on my symmetric key
        // 4. Encrypt symmetric key on received public key and sign by mine
        // 5. Send all it back with received requestId

        keysRepository.getFullKeys { fullKeys ->
            if (fullKeys.isNotEmpty()) {
                Log.d(TAG, "${fullKeys.size} fullKeys found")
                val signKey = getSignKey(fullKeys) ?: return@getFullKeys

                val asymmetricKeys = fullKeys.map { keysMapper.fromDb(it) }

                val gson = Gson()
                val keysJson = gson.toJson(asymmetricKeys)
                val keysJsonBytes = keysJson.toByteArray()

                try {
                    val symmetricKeyWrapper = keysGeneratorHelper.getSymmetricKey()

                    yEncrypt.publicEncryptKeyToSend = EncryptHelper.base64ToBytes(getKeys.publicKey)
                    yEncrypt.privateSignKeyToSend = signKey.privateKey

                    val lifeTime = 1000L

                    val encryptedSymmetricKey =
                        yEncrypt.encrypKeysMsg(1, lifeTime, symmetricKeyWrapper.key)
                    val encryptedSymmetricKeyBase64 =
                        EncryptHelper.bytesToBase64(encryptedSymmetricKey)

                    // Encrypt private keys with symmetric key
                    yEncrypt.setSymmetricEncryptKey(symmetricKeyWrapper.key)
                    val encData = yEncrypt.encryptSecretMsg(1, 0, lifeTime, keysJsonBytes)
                    val encryptedDataBase64 = EncryptHelper.bytesToBase64(encData)


                    val encryptedData = EncryptedData(
                        encryptedSymmetricKeyBase64,
                        encryptedDataBase64,
                        EncryptHelper.bytesToBase64(signKey.publicKey),
                        getKeys.requestId
                    )

                    keysRepository.sendKeysToAnotherDevices(encryptedData)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send private keys to other devices")
                    e.printStackTrace()

                    sendError("Failed to send private keys", getKeys)
                }
            } else {
                sendError("There is no private keys on this device", getKeys)
            }
        }
    }

    private fun sendError(errorMessage: String, clientRequest: ClientRequest) {
        val error = Error(errorMessage, clientRequest.requestId)
        webSocketService.sendError(error)
    }

    private fun getSignKey(keys: List<Keys>): Keys? {
        for (key in keys) {
            if (key.isSign) return key
        }

        return null
    }

    companion object {
        private const val TAG = "ClientRequestHandler"
    }

}