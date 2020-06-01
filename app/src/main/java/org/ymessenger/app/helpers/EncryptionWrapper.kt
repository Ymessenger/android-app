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
import y.encrypt.YEncrypt

class EncryptionWrapper {

    private lateinit var yEncrypt: YEncrypt

    companion object {
        private const val TAG = "EncryptionWrapper"
    }

    fun init(passphrase: String, passId: Long) {
        yEncrypt = YEncrypt(passphrase.toByteArray(), passId)
        Log.d(TAG, "YEncrypt is initialized")
    }

    fun isInitialized() = this::yEncrypt.isInitialized

    fun getYEncrypt(): YEncrypt {
        return if (isInitialized()) yEncrypt else throw Exception("YEncrypt is not initialized")
    }

}