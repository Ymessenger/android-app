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
import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.SymmetricKeyDao
import org.ymessenger.app.data.local.db.entities.SymmetricKey
import org.ymessenger.app.utils.AppExecutors

class SymmetricKeyRepository private constructor(
    private val executors: AppExecutors,
    private val symmetricKeyDao: SymmetricKeyDao
) {

    fun saveSymmetricKey(symmetricKey: SymmetricKey) {
        executors.diskIO.execute {
            symmetricKeyDao.insert(symmetricKey)
        }
    }

    fun getSymmetricKey(keyId: Long, getKeyCallback: GetKeyCallback) {
        executors.diskIO.execute {
            val key = symmetricKeyDao.getSymmetricKeySync(keyId)
            Handler(Looper.getMainLooper()).post {
                key?.let { getKeyCallback.result(it) } ?: getKeyCallback.keyNotFound()
            }
        }
    }

    fun getSymmetricKeySync(keyId: Long) = symmetricKeyDao.getSymmetricKeySync(keyId)

    fun getLastKeyForDialog(dialogId: Long): LiveData<SymmetricKey> {
        return symmetricKeyDao.getSymmetricKeyByDialog(dialogId)
    }

    interface GetKeyCallback {
        fun result(symmetricKey: SymmetricKey)
        fun keyNotFound()
    }

    // DEVELOPER OPTIONS

    fun deleteAllKeys() {
        executors.diskIO.execute {
            symmetricKeyDao.deleteAllKeys()
        }
    }

    companion object {
        private const val TAG = "SymmetricKeyRepository"

        private var instance: SymmetricKeyRepository? = null

        fun getInstance(
            executors: AppExecutors,
            symmetricKeyDao: SymmetricKeyDao
        ): SymmetricKeyRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: SymmetricKeyRepository(
                        executors,
                        symmetricKeyDao
                    ).also { instance = it }
            }
        }
    }

}