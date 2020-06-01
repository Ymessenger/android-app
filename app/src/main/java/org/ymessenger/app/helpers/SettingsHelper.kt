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

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.remote.entities.Token

class SettingsHelper(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val TAG = "SettingsHelper"

        private const val KEY_FIRST_LAUNCH = "FIRST_LAUNCH"
        private const val KEY_TOKEN = "TOKEN"
        private const val KEY_LAST_UPDATE_TIME = "LAST_UPDATE_TIME"
        private const val KEY_NODE = "NODE"

        private const val KEY_PIN = "PIN"
        private const val KEY_SAFE_PIN = "SAFE_PIN"

        private const val KEY_YENCRYPT_PASS = "YENCRYPT_PASS"
        private const val KEY_YENCRYPT_PASS_ID = "YENCRYPT_PASS_ID"

        private const val KEY_SAVE_PASSPHRASE = "SAVE_PASSPHRASE"
        // TODO: DELETE THIS IN ONE OF NEXT UPDATES
        private const val KEY_ASKED_OLD_USERS_TO_SET_PASSPHRASE =
            "ASKED_OLD_USERS_TO_SET_PASSPHRASE"

        private const val KEY_SYNC_CONTACTS = "SYNC_CONTACTS"

        private const val KEY_HIDE_NOTIFICATION_CONTENT = "HIDE_NOTIFICATION_CONTENT"

        private const val KEY_SAVE_ENCRYPTED_MESSAGES_ON_SERVER =
            "SAVE_ENCRYPTED_MESSAGES_ON_SERVER"

        private const val KEY_FIREBASE_TOKEN = "FIREBASE_TOKEN"

        private const val KEY_IS_DEV_OPTIONS_UNLOCKED = "IS_DEV_OPTIONS_UNLOCKED"

        private const val KEY_ALWAYS_OPEN_MAIN_ACTIVITY_AS_AFTER_REGISTER =
            "ALWAYS_OPEN_MAIN_ACTIVITY_AS_AFTER_REGISTER"

        private const val KEY_FAST_SYMMETRIC_KEY = "FAST_SYMMETRIC_KEY"

        private const val KEY_USE_ENCRYPTED_CONNECTION = "USE_ENCRYPTED_CONNECTION"
    }

    fun isFirstLaunch(): Boolean {
        return preferences
            .getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunch() {
        preferences
            .edit().putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }

    fun getToken(): Token? {
        var token: Token? = null

        if (preferences.contains(KEY_TOKEN)) {
            val tokenJson = preferences.getString(KEY_TOKEN, "")
            val gson = Gson()
            try {
                token = gson.fromJson(tokenJson, Token::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return token
    }

    fun setToken(token: Token) {
        val tokenJson = Gson().toJson(token)
        preferences.edit()
            .putString(KEY_TOKEN, tokenJson)
            .apply()
    }

    private fun clearToken() {
        preferences
            .edit().remove(KEY_TOKEN)
            .commit() // There is not apply because we need to delete this value immediately, not in background
    }

    fun getLastUpdateTime(): Long {
        return preferences
            .getLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis() / 1000)
    }

    fun setLastUpdateTime(lastUpdateTime: Long) {
        preferences.edit()
            .putLong(KEY_LAST_UPDATE_TIME, lastUpdateTime)
            .apply()
    }

    fun clearLastUpdateTime() {
        preferences
            .edit().remove(KEY_LAST_UPDATE_TIME)
            .apply()
    }

    fun getNode(): Node? {
        var node: Node? = null

        if (preferences.contains(KEY_NODE)) {
            val json = preferences.getString(KEY_NODE, "")
            val gson = Gson()
            try {
                node = gson.fromJson(json, Node::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse node")
                e.printStackTrace()
            }
        }

        return node
    }

    fun setNode(node: Node) {
        val json = Gson().toJson(node)
        preferences.edit()
            .putString(KEY_NODE, json)
            .apply()
    }

    fun clearNode() {
        preferences
            .edit().remove(KEY_NODE)
            .apply()
    }

    fun hasPin(): Boolean {
        return preferences
            .contains(KEY_PIN)
    }

    fun getPin(): String? {
        return preferences
            .getString(KEY_PIN, null)
    }

    fun setPin(pin: String) {
        preferences.edit()
            .putString(KEY_PIN, pin)
            .apply()
    }

    fun clearPin() {
        preferences
            .edit().remove(KEY_PIN)
            .apply()
    }

    fun hasSafePin(): Boolean {
        return preferences
            .contains(KEY_SAFE_PIN)
    }

    fun getSafePin(): String? {
        return preferences
            .getString(KEY_SAFE_PIN, null)
    }

    fun setSafePin(pin: String) {
        preferences.edit()
            .putString(KEY_SAFE_PIN, pin)
            .apply()
    }

    fun clearSafePin() {
        preferences
            .edit().remove(KEY_SAFE_PIN)
            .apply()
    }

    fun getYEncryptPass(): String? {
        return preferences
            .getString(KEY_YENCRYPT_PASS, null)
    }

    fun getYEncryptMPID(): Long {
        return preferences
            .getLong(KEY_YENCRYPT_PASS_ID, 0)
    }

    fun setYEncryptPass(pass: String) {
        preferences.edit()
            .putString(KEY_YENCRYPT_PASS, pass)
            .apply()
    }

    fun clearYEncryptPass() {
        preferences
            .edit().remove(KEY_YENCRYPT_PASS)
            .apply()
    }

    fun getSyncContacts(): Boolean {
        return preferences
            .getBoolean(KEY_SYNC_CONTACTS, false)
    }

    fun setSyncContacts(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SYNC_CONTACTS, value)
            .apply()
    }

    fun getSaveEncryptedMessagesOnServer(): Boolean {
        return preferences
            .getBoolean(KEY_SAVE_ENCRYPTED_MESSAGES_ON_SERVER, false)
    }

    fun setSaveEncryptedMessagesOnServer(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SAVE_ENCRYPTED_MESSAGES_ON_SERVER, value)
            .apply()
    }

    fun getFirebaseToken(): String? {
        return preferences
            .getString(KEY_FIREBASE_TOKEN, null)
    }

    fun setFirebaseToken(token: String) {
        preferences.edit()
            .putString(KEY_FIREBASE_TOKEN, token)
            .apply()
    }

    fun isDevOptionsUnlocked(): Boolean {
        return preferences
            .getBoolean(KEY_IS_DEV_OPTIONS_UNLOCKED, false)
    }

    fun setDevOptionsUnlocked(unlocked: Boolean) {
        preferences.edit()
            .putBoolean(KEY_IS_DEV_OPTIONS_UNLOCKED, unlocked)
            .apply()
    }

    fun setAlwaysOpenMainActivityAsAfterRegister(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ALWAYS_OPEN_MAIN_ACTIVITY_AS_AFTER_REGISTER, value)
            .apply()
    }

    fun getAlwaysOpenMainActivityAsAfterRegister(): Boolean {
        return preferences.getBoolean(KEY_ALWAYS_OPEN_MAIN_ACTIVITY_AS_AFTER_REGISTER, false)
    }

    fun clearAfterLogout() {
        clearToken()
        clearLastUpdateTime()
//        clearNode() // FIXME: when clear node it stores in NodeManager's LiveData object and connects to it BUT not saves it in Preferences and app crashes
        clearPin()
        clearSafePin()
    }

    fun getFastSymmetricKey(): ByteArray? {
        val key = preferences.getString(KEY_FAST_SYMMETRIC_KEY, null)
        return if (key == null) {
            null
        } else {
            EncryptHelper.base64ToBytes(key)
        }
    }

    fun setFastSymmetricKey(key: ByteArray) {
        val sKey = EncryptHelper.bytesToBase64(key)
        preferences.edit()
            .putString(KEY_FAST_SYMMETRIC_KEY, sKey)
            .apply()
    }

    fun clearFastSymmetricKey() {
        preferences.edit()
            .remove(KEY_FAST_SYMMETRIC_KEY)
            .apply()
    }

    fun getUseEncryptedConnection(): Boolean {
        return preferences
            .getBoolean(KEY_USE_ENCRYPTED_CONNECTION, true)
    }

    fun setUseEncryptedConnection(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_USE_ENCRYPTED_CONNECTION, value)
            .apply()
    }

    fun getHideNotificationContent(): Boolean {
        return preferences
            .getBoolean(KEY_HIDE_NOTIFICATION_CONTENT, false)
    }

    fun setHideNotificationContent(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_HIDE_NOTIFICATION_CONTENT, value)
            .apply()
    }

    fun getAskedOldUsersToSetPassphrase(): Boolean {
        return preferences.getBoolean(KEY_ASKED_OLD_USERS_TO_SET_PASSPHRASE, false)
    }

    fun setAskedOldUsersToSetPassphrase() {
        preferences.edit()
            .putBoolean(KEY_ASKED_OLD_USERS_TO_SET_PASSPHRASE, true)
            .apply()
    }

    fun clearAskedOldUsersToSetPassphrase() {
        preferences.edit()
            .remove(KEY_ASKED_OLD_USERS_TO_SET_PASSPHRASE)
            .apply()
    }

    fun getSavePassphrase(): Boolean {
        return preferences.getBoolean(KEY_SAVE_PASSPHRASE, true)
    }

    fun setSavePassphrase(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SAVE_PASSPHRASE, value)
            .apply()
    }

}