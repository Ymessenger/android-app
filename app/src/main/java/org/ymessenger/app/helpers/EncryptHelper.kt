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

import android.util.Base64
import org.ymessenger.app.data.local.db.entities.Keys
import org.ymessenger.app.data.local.db.entities.SymmetricKey

object EncryptHelper {

    fun base64ToBytes(string: String): ByteArray {
        return Base64.decode(string, Base64.NO_WRAP)
    }

    fun bytesToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun isExpired(keys: Keys) =
        keys.generationTime + keys.lifetime <= System.currentTimeMillis() / 1000

    fun isExpired(symmetricKey: SymmetricKey) =
        symmetricKey.generationTime + symmetricKey.lifetime <= System.currentTimeMillis() / 1000
}