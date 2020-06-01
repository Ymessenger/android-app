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

package org.ymessenger.app.data.remote.requests

import com.google.gson.annotations.SerializedName
import org.ymessenger.app.data.remote.WSRequest

class SetConnectionEncrypted private constructor(
    @SerializedName("PublicKey")
    val publicKey: String?,
    @SerializedName("NodeId")
    val nodeId: Long?,
    @SerializedName("SignPublicKey")
    val signPublicKey: String?,

    @SerializedName("KeyId")
    val keyId: Long?,
    @SerializedName("UserId")
    val userId: Long?,
    @SerializedName("SignKeyId")
    val signKeyId: Long?
) : WSRequest(RequestType.SET_CONNECTION_ENCRYPTED) {

    constructor(publicKey: String, nodeId: Long, signPublicKey: String) : this(
        publicKey,
        nodeId,
        signPublicKey,
        null,
        null,
        null
    )

    constructor(keyId: Long, userId: Long, signKeyId: Long) : this(
        null,
        null,
        null,
        keyId,
        userId,
        signKeyId
    )
}