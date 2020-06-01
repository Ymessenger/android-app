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

import org.ymessenger.app.data.remote.Config.LICENSOR_BASE_URL
import org.ymessenger.app.data.remote.Config.LICENSOR_PORT

class UrlGenerator {
    companion object {
        private var baseUrl = ""
        private var port = 0

        fun setBaseUrlAndPort(baseUrl: String, port: Int) {
            this.baseUrl = baseUrl
            this.port = port
        }

        fun getWebSocketUrl() = "wss://$baseUrl:$port"

        fun getFileUrl(fileId: String?): String? {
            return if (fileId.isNullOrBlank()) {
                null
            } else {
                "https://$baseUrl:$port/api/Files/$fileId"
            }
        }

        fun getApiBaseUrl() = "https://$baseUrl:$port/"


        fun getLicensorWSUrl() = "wss://${LICENSOR_BASE_URL}:$LICENSOR_PORT"

        fun getLicensorFileUrl(fileId: String?): String? {
            return if (fileId.isNullOrBlank()) {
                null
            } else {
                "https://$LICENSOR_BASE_URL:$LICENSOR_PORT/api/Files/$fileId"
            }
        }
    }
}