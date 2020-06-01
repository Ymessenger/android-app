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

package org.ymessenger.app.helpers

import android.util.Log
import kotlin.random.Random

class KeysGeneratorHelper(private val encryptionWrapper: EncryptionWrapper) {

    companion object {
        private const val TAG = "KeysGeneratorHelper"

        private const val VERSION: Long = 1

        private const val DAY = 86400L
        private const val WEEK = DAY * 7

        private const val LIFETIME_ASYMMETRIC: Long = WEEK
        private const val LIFETIME_SYMMETRIC: Long = WEEK

//        private const val LIFETIME_ASYMMETRIC: Long = 180 // 3 min for test
//        private const val LIFETIME_SYMMETRIC: Long = 300 // 5 min for test
    }

    enum class Length {
        SHORT,
        MIDDLE,
        LONG
    }

    fun getAsymmetricKeys(length: Length, isSign: Boolean): KeyPairWrapper {
        val yEncrypt = encryptionWrapper.getYEncrypt()

        val keysId = Random.nextLong(Long.MAX_VALUE)
        val startGeneration = System.currentTimeMillis()
        val keyPair = when (length) {
            Length.SHORT -> yEncrypt.getShortAsymmetricKeys(
                VERSION,
                keysId,
                LIFETIME_ASYMMETRIC,
                isSign
            )
            Length.MIDDLE -> yEncrypt.getMidleAsymmetricKeys(
                VERSION,
                keysId,
                LIFETIME_ASYMMETRIC,
                isSign
            )
            Length.LONG -> yEncrypt.getLongAsymmetricKeys(
                VERSION,
                keysId,
                LIFETIME_ASYMMETRIC,
                isSign
            )
        }
        var generationTime = System.currentTimeMillis()
        val generatedFor = (generationTime - startGeneration) / 1000
        Log.d(
            TAG,
            "${length.name} ${if (isSign) "sign " else ""}keys generated for $generatedFor sec: public - ${EncryptHelper.bytesToBase64(
                keyPair.publicKey
            )}, private - ${EncryptHelper.bytesToBase64(keyPair.privateKey)}"
        )

        generationTime /= 1000

        return KeyPairWrapper(
            keyPair,
            keysId,
            generationTime,
            LIFETIME_ASYMMETRIC,
            VERSION,
            isSign
        )
    }

    fun getSymmetricKey(): SymmetricKeyWrapper {
        val yEncrypt = encryptionWrapper.getYEncrypt()

        val keyId = Random.nextLong(Long.MAX_VALUE)
        val startGeneration = System.currentTimeMillis()
        val key = yEncrypt.getSymmetricKey(VERSION, keyId, LIFETIME_SYMMETRIC)
        var generationTime = System.currentTimeMillis()
        val generatedFor = (generationTime - startGeneration) / 1000
        Log.d(
            TAG,
            "Symmetric key generated for $generatedFor sec: ${EncryptHelper.bytesToBase64(key)}"
        )

        generationTime /= 1000

        return SymmetricKeyWrapper(
            key,
            keyId,
            generationTime,
            LIFETIME_SYMMETRIC,
            VERSION
        )
    }

}