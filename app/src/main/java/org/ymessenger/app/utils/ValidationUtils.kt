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

package org.ymessenger.app.utils

const val MIN_PHONE_NUMBER_LENGTH = 10

object ValidationUtils {
    fun validateFirstName(name: String?): Boolean {
        if (name.isNullOrEmpty()) {
            return false
        }

        return true
    }

    fun validateLastName(lastName: String?): Boolean {
        if (lastName.isNullOrEmpty()) {
            return false
        }

        return true
    }

    fun validatePhoneNumber(phone: String?): Boolean {
        if (phone.isNullOrEmpty()) {
            return false
        }

        if (phone.length < MIN_PHONE_NUMBER_LENGTH) {
            return false
        }

        return true
    }

    fun validateEmail(email: String?): Boolean {
        if (email.isNullOrEmpty()) {
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false
        }

        return true
    }
}