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

package org.ymessenger.app.data.remote.entities

import android.content.Context
import com.google.gson.annotations.SerializedName
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.UrlGenerator
import org.ymessenger.app.helpers.PhotoLabelHelper

data class Node(
    @SerializedName("Id")
    val id: Long,
    @SerializedName("Name")
    val name: String?,
    @SerializedName("Tag")
    val tag: String,
    @SerializedName("About")
    val about: String?,
    @SerializedName("Photo")
    val photo: String?,
    @SerializedName("Country")
    val country: String?,
    @SerializedName("City")
    val city: String?,
    @SerializedName("StartDay")
    val startDay: String?,
    @SerializedName("Languages")
    val languages: List<String>,
    @SerializedName("Domains")
    val domains: List<String>?,
    @SerializedName("IpAddresses")
    val ipAddresses: List<String>?,
    @SerializedName("NodesId")
    val nodesId: List<Long>?,
    @SerializedName("Visible")
    val visible: Boolean,
    @SerializedName("Storage")
    val storage: Boolean,
    @SerializedName("Routing")
    val routing: Boolean,
    @SerializedName("ClientsPort")
    val clientsPort: Int,
    @SerializedName("NodesPort")
    val nodesPort: Int,
    @SerializedName("NodeKey")
    val nodeKey: NodeKey,
    @SerializedName("EncryptionType")
    val encryptionType: Int,
    @SerializedName("PermanentlyDeleting")
    val permanentlyDeleting: Boolean,
    @SerializedName("RegistrationMethod")
    val registrationMethod: Int,
    @SerializedName("SupportEmail")
    val supportEmail: String?,
    @SerializedName("AdminEmail")
    val adminEmail: String?,
    @SerializedName("UserRegistrationAllowed")
    val userRegistrationAllowed: Boolean
) {
    fun getPhotoLabel(): String {
        return PhotoLabelHelper.get(getDisplayName())
    }

    fun getPhotoUrl() = UrlGenerator.getLicensorFileUrl(photo)

    fun getDisplayName(): String {
        return name ?: tag
    }

    class NodeKey(
        @SerializedName("KeyId")
        val keyId: Long,
        @SerializedName("EncPublicKey")
        val encPublicKey: String,
        @SerializedName("SignPublicKey")
        val signPublicKey: String,
        @SerializedName("ExpiredAt")
        val expiredAt: Long
    )

    object EncryptionType {
        const val FORBIDDEN = 0
        const val FORBIDDEN_ON_THIS_NODE = 1
        const val FULL = 2
    }

    object RegistrationMethod {
        const val NOTHING_REQUIRED = 0
        const val PHONE_REQUIRED = 1
        const val EMAIL_REQUIRED = 2
    }

    fun getEncryptionTypeLabel(context: Context): String {
        val resId = when (encryptionType) {
            EncryptionType.FORBIDDEN -> R.string.forbidden
            EncryptionType.FORBIDDEN_ON_THIS_NODE -> R.string.forbidden_on_this_server
            EncryptionType.FULL -> R.string.full
            else -> R.string.unknown_type
        }

        return context.getString(resId)
    }

    fun getPermanentlyDeletingLabel(context: Context): String {
        return context.getString(
            if (permanentlyDeleting) {
                R.string.full_deletion
            } else {
                R.string.mark_as_deleted
            }
        )
    }

    fun getRegistrationLabel(context: Context): String {
        return context.getString(
            if (userRegistrationAllowed) {
                R.string.yes
            } else {
                R.string.no
            }
        )
    }

    fun getRegistrationMethodLabel(context: Context): String {
        val resId = when (registrationMethod) {
            RegistrationMethod.NOTHING_REQUIRED -> R.string.nothing_required
            RegistrationMethod.PHONE_REQUIRED -> R.string.phone_required
            RegistrationMethod.EMAIL_REQUIRED -> R.string.email_required
            else -> R.string.unknown_type
        }

        return context.getString(resId)
    }
}