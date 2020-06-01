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

import android.os.Build
import com.google.gson.annotations.SerializedName
import org.ymessenger.app.BuildConfig
import org.ymessenger.app.data.remote.WSRequest
import org.ymessenger.app.data.remote.entities.Token

class Login : WSRequest {
    @SerializedName("Uid")
    var uid: String = ""
    @SerializedName("Token")
    var token: Token? = null
    @SerializedName("VCode")
    var vCode: Int? = null
    @SerializedName("Password")
    var password: String = ""
    @SerializedName("LoginType")
    var loginType: Int
    @SerializedName("UidType")
    var uidType: Int
    @SerializedName("DeviceTokenId")
    var deviceTokenId: String? = null

    @SerializedName("DeviceName")
    var deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"

    @SerializedName("OSName")
    var OSName: String = "Android ${Build.VERSION.RELEASE}"

    @SerializedName("AppName")
    var appName: String = "Y Messenger ${BuildConfig.VERSION_NAME}"

    constructor(token: Token) : super(RequestType.LOGIN) {
        this.token = token
        this.loginType = 0
        this.uidType = 1
    }

    constructor(uid: String, uidType: Int, vCode: Int) : super(RequestType.LOGIN) {
        this.uid = uid
        this.vCode = vCode
        this.loginType = LOGIN_TYPE_VERIFICATION_CODE
        this.uidType = uidType
    }

    companion object {
        const val LOGIN_TYPE_TOKEN = 0
        const val LOGIN_TYPE_VERIFICATION_CODE = 1
        const val LOGIN_TYPE_PASSWORD = 2

        const val UID_TYPE_PHONE = 0
        const val UID_TYPE_IDENTIFIER = 1
        const val UID_TYPE_EMAIL = 2
    }
}