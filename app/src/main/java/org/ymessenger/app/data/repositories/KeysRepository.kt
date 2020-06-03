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

package org.ymessenger.app.data.repositories

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.KeysDao
import org.ymessenger.app.data.local.db.entities.Keys
import org.ymessenger.app.data.mappers.KeysMapper
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.clientResponses.EncryptedData
import org.ymessenger.app.data.remote.entities.Key
import org.ymessenger.app.data.remote.requests.AddNewKeys
import org.ymessenger.app.data.remote.requests.GetDevicesPrivateKeys
import org.ymessenger.app.data.remote.requests.GetUserKeys
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors

class KeysRepository private constructor(
    private val executors: AppExecutors,
    private val keysDao: KeysDao,
    private val webSocketService: WebSocketService,
    private val keysMapper: KeysMapper
) {

    fun getKeysByUser(userId: Long): List<Keys> {
        val getUserKeys = GetUserKeys(userId)
        webSocketService.getUserKeys(
            getUserKeys,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                    val dbKeys = response.keys.map { keysMapper.toDb(it) }

                    executors.diskIO.execute {
                        keysDao.insert(dbKeys)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get keys by user")
                }
            })
        return keysDao.getKeysByUser(userId)
    }

    fun getKeysByUser(userId: Long, keyId: Long, callback: ((Keys?) -> Unit)) {
        executors.diskIO.execute {
            val keys = keysDao.getKeys(userId, keyId)
            if (keys != null) {
                Handler(Looper.getMainLooper()).post {
                    callback(keys)
                }
            } else {
                val getUserKeys = GetUserKeys(userId, keysId = listOf(keyId))
                webSocketService.getUserKeys(
                    getUserKeys,
                    object :
                        WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                        override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                            val dbKeys = response.keys.map { keysMapper.toDb(it) }

                            executors.diskIO.execute {
                                keysDao.insert(dbKeys)
                            }

                            callback(dbKeys.firstOrNull())
                        }

                        override fun onError(error: ResultResponse) {
                            Log.e(TAG, "Failed to get keys by user")
                        }
                    })
            }
        }
    }

    fun loadKeysByUser(userId: Long, keyIds: List<Long>, callback: SuccessErrorCallback) {
        val getUserKeys = GetUserKeys(userId, keysId = keyIds)
        webSocketService.getUserKeys(
            getUserKeys,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                    val dbKeys = response.keys.map { keysMapper.toDb(it) }

                    executors.diskIO.execute {
                        keysDao.insert(dbKeys)
                    }

                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get keys by user")
                    callback.error(error)
                }
            })
    }

    fun getKeysSync(keyId: Long) = keysDao.getKeys(keyId)

    fun getKeysSync(userId: Long, keyId: Long) = keysDao.getKeys(userId, keyId)

    fun getLastUserKey(userId: Long, getKeyResult: GetKeyResult) {
        val getUserKeys = GetUserKeys(userId)
        webSocketService.getUserKeys(
            getUserKeys,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                    val dbKeys = response.keys.map { keysMapper.toDb(it) }

                    executors.diskIO.execute {
                        keysDao.insert(dbKeys)
                    }

                    if (dbKeys.isNotEmpty()) {
                        var found = false
                        for (dbKey in dbKeys) {
                            if (!dbKey.isSign) {
                                getKeyResult.result(dbKey)
                                found = true
                                break
                            }
                        }

                        if (!found) {
                            getKeyResult.error()
                        }
                    } else {
                        getKeyResult.error()
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get last user key")
                    getKeyResult.error()
                }
            })
    }

    fun getMyLastKey(userId: Long, isSign: Boolean, getKeyResult: GetKeyResult) {
        executors.diskIO.execute {
            val keys = keysDao.getMyLastKeys(userId, isSign)
            Handler(Looper.getMainLooper()).post {
                if (keys == null) {
                    getKeyResult.error()
                } else {
                    getKeyResult.result(keys)
                }
            }
        }
    }

    fun getMyLastKeys(isSign: Boolean): LiveData<Keys> {
        return keysDao.getMyLastKeys(isSign)
    }

    fun addNewKey(key: Key, privateKey: ByteArray) {
        val addNewKeys = AddNewKeys(listOf(key))
        webSocketService.addNewKeys(
            addNewKeys,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                    Log.d(TAG, "Added new key")
                    val dbKey = keysMapper.toDb(response.keys[0])
                    dbKey.privateKey = privateKey

                    executors.diskIO.execute {
                        keysDao.insert(dbKey)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to add new keys")
                }
            })
    }

    fun addNewKey(key: Key, privateKey: ByteArray, callback: (Keys?) -> Unit) {
        val addNewKeys = AddNewKeys(listOf(key))
        webSocketService.addNewKeys(
            addNewKeys,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                    Log.d(TAG, "Added new key")
                    val dbKey = keysMapper.toDb(response.keys[0])
                    dbKey.privateKey = privateKey

                    executors.diskIO.execute {
                        keysDao.insert(dbKey)
                    }

                    callback.invoke(dbKey)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to add new keys")
                    callback.invoke(null)
                }
            })
    }

    fun loadUserKeysFromServer(userId: Long, navigationTime: Long? = null) {
        val getUserKeys = GetUserKeys(userId, navigationTime = navigationTime)
        webSocketService.getUserKeys(
            getUserKeys,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Keys> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Keys) {
                    response.keys.lastOrNull()?.let {
                        Log.d(TAG, "Load next page")
                        loadUserKeysFromServer(userId, it.generationTime)
                    } ?: Log.d(TAG, "This is the end")

                    val dbKeys = response.keys.map { keysMapper.toDb(it) }

                    executors.diskIO.execute {
                        keysDao.insert(dbKeys)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get keys by user")
                }
            })
    }

    fun getKeysFromOtherDevices(publicKey: String, signKeyId: Long, sign: String, callback: SuccessErrorCallback) {
        val getDevicesPrivateKeys = GetDevicesPrivateKeys(publicKey, signKeyId, sign)
        webSocketService.getDevicesPrivateKeys(
            getDevicesPrivateKeys,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    Log.d(TAG, "Request for private keys is sent successfully")
                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get devices private keys")
                    callback.error(error)
                }
            })
    }

    fun sendKeysToAnotherDevices(encryptedData: EncryptedData) {
        webSocketService.sendEncryptedData(encryptedData)
    }

    fun getFullKeys(callback: (List<Keys>) -> Unit) {
        executors.diskIO.execute {
            val fullKeys = keysDao.getFullKeys()
            callback.invoke(fullKeys)
        }
    }

    fun savePrivateKeys(keysList: List<Keys>) {
        executors.diskIO.execute {
            keysDao.insertReplace(keysList)
        }
    }

    interface GetKeyResult {
        fun result(keys: Keys)
        fun error()
    }

    // DEVELOPER OPTIONS

    fun deleteAllKeys() {
        executors.diskIO.execute {
            keysDao.deleteAllKeys()
        }
    }

    companion object {
        private const val TAG = "KeysRepository"

        private var instance: KeysRepository? = null

        fun getInstance(
            executors: AppExecutors,
            keysDao: KeysDao,
            webSocketService: WebSocketService,
            keysMapper: KeysMapper
        ): KeysRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: KeysRepository(
                        executors,
                        keysDao,
                        webSocketService,
                        keysMapper
                    ).also { instance = it }
            }
        }
    }

}