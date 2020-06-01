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

package org.ymessenger.app.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import org.ymessenger.app.AppBase
import org.ymessenger.app.data.local.db.AppDatabase
import org.ymessenger.app.data.remote.entities.Key
import org.ymessenger.app.data.repositories.KeysRepository
import org.ymessenger.app.di.Injection
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.KeyPairWrapper
import org.ymessenger.app.helpers.KeysGeneratorHelper
import org.ymessenger.app.utils.AppExecutors

class AsymmetricKeysGeneratorService : IntentService(TAG) {

    private lateinit var keysGeneratorHelper: KeysGeneratorHelper
    private lateinit var keysRepository: KeysRepository

    companion object {
        private const val TAG = "AKGeneratorService"

        const val ACTION_GET_SHORT_KEYS = "GetShortKeys"
        const val ACTION_GET_MIDDLE_KEYS = "GetMiddleKeys"
        const val ACTION_GET_LONG_KEYS = "GetLongKeys"

        fun getIntent(context: Context, action: String): Intent {
            val intent = Intent(context, AsymmetricKeysGeneratorService::class.java)
            intent.action = action

            return intent
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        val encryptionWrapper = (application as AppBase).getEncryptionWrapper()
        keysGeneratorHelper = KeysGeneratorHelper(encryptionWrapper)

        val database = AppDatabase.getInstance(applicationContext)
        val webSocketService = (application as AppBase).getWebSocketService()
        keysRepository = KeysRepository.getInstance(
            AppExecutors.getInstance(),
            database.keysDao(),
            webSocketService,
            Injection.provideKeysMapper()
        )
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent")

        when (intent?.action) {
            ACTION_GET_SHORT_KEYS -> generateShortAsymmetricKeys()
            ACTION_GET_MIDDLE_KEYS -> generateMiddleAsymmetricKeys()
            ACTION_GET_LONG_KEYS -> generateLongAsymmetricKeys()
            else -> throw IllegalArgumentException("Wrong action, can't generate keys")
        }
    }

    private fun generateShortAsymmetricKeys(isSign: Boolean = true) {
        val keyPairWrapper =
            keysGeneratorHelper.getAsymmetricKeys(KeysGeneratorHelper.Length.SHORT, isSign)

        addNewKey(keyPairWrapper)

        if (isSign) {
            generateShortAsymmetricKeys(false)
        } else {
            generateMiddleAsymmetricKeys()
        }
    }

    private fun generateMiddleAsymmetricKeys(isSign: Boolean = true) {
        val keyPairWrapper =
            keysGeneratorHelper.getAsymmetricKeys(KeysGeneratorHelper.Length.MIDDLE, isSign)

        addNewKey(keyPairWrapper)

        if (isSign) {
            generateMiddleAsymmetricKeys(false)
        } else {
            generateLongAsymmetricKeys()
        }
    }

    private fun generateLongAsymmetricKeys(isSign: Boolean = true) {
        val keyPairWrapper =
            keysGeneratorHelper.getAsymmetricKeys(KeysGeneratorHelper.Length.LONG, isSign)

        addNewKey(keyPairWrapper)

        if (isSign) {
            generateLongAsymmetricKeys(false)
        }
    }

    private fun addNewKey(keyPairWrapper: KeyPairWrapper) {
        val key = Key(
            keyPairWrapper.keysId,
            EncryptHelper.bytesToBase64(keyPairWrapper.keyPair.publicKey),
            keyPairWrapper.lifetime,
            keyPairWrapper.generationTime,
            keyPairWrapper.version.toInt(),
            null,
            null,
            if (keyPairWrapper.isSign) 0 else 1
        )
        keysRepository.addNewKey(key, keyPairWrapper.keyPair.privateKey)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}