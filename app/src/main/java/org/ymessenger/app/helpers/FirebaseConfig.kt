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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.ymessenger.app.R

class FirebaseConfig {

    companion object {
        private const val TAG = "FirebaseConfig"
        private const val KEY_LICENSOR_KEYS = "licensor_keys"
    }

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        update()
    }

    private fun update() {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FirebaseRemoteConfig is updated")
                val licensorKeys = remoteConfig.getString(KEY_LICENSOR_KEYS)
                Log.d(TAG, "licensor_keys: $licensorKeys")
            } else {
                Log.e(TAG, "Fetch failed")
            }
        }
    }

    fun getLicensorKeys(): LicensorKeys {
        val licensorKeysStr = remoteConfig.getString(KEY_LICENSOR_KEYS)
        val gson = Gson()

        val licensorKeysList = gson.fromJson<List<LicensorKeys>>(
            licensorKeysStr,
            object : TypeToken<List<LicensorKeys>>() {}.type
        )

        val now = System.currentTimeMillis() / 1000

        for (licensorKey in licensorKeysList) {
            if (licensorKey.expiredAt != null && licensorKey.expiredAt < now) continue

            if (licensorKey.createdAt < now && licensorKey.expiredAt == null) {
                return licensorKey
            }
        }

        return licensorKeysList.last()
    }

    data class LicensorKeys(
        @SerializedName("created_at")
        val createdAt: Long,
        @SerializedName("expired_at")
        val expiredAt: Long?,
        @SerializedName("encrypt_key")
        val encryptKey: String,
        @SerializedName("sign_key")
        val signKey: String
    )

}