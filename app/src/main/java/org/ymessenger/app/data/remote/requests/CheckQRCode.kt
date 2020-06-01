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
import org.ymessenger.app.data.remote.entities.QRCode

class CheckQRCode(
    @SerializedName("QR")
    val qrCode: QRCode,
    @SerializedName("DeviceTokenId")
    val deviceTokenId: String?
) : WSRequest(RequestType.CHECK_QR_CODE) {

    @SerializedName("DeviceName")
    var deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"

    @SerializedName("OSName")
    var OSName: String = "Android ${Build.VERSION.RELEASE}"

    @SerializedName("AppName")
    var appName: String = "Y Messenger ${BuildConfig.VERSION_NAME}"

}