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

object Config {
    const val LICENSOR_BASE_URL = "testlic-1.ymess.org"
    const val LICENSOR_PORT = 5005

    const val TIMER_UPDATE_PERIOD = 30000L
    const val WEB_SOCKET_REQUEST_TIMEOUT = 20000L

    const val USER_ACTION_SEND_PERIOD = 2
    const val USER_ACTION_CHECK_PERIOD = 3
    const val USER_ACTION_DISPLAY_PERIOD = 3
}