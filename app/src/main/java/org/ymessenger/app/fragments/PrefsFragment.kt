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

package org.ymessenger.app.fragments

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.ymessenger.app.AppBase
import org.ymessenger.app.R
import org.ymessenger.app.activities.*


class PrefsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private var mLastClickTime: Long = 0

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // We can handle click and set intents in code, but also we can set intent in XML
        initClickListeners()
        initIntents()
    }

    override fun onResume() {
        super.onResume()
        val appBase = activity?.application as AppBase
        showDeveloperOptions(appBase.settingsHelper.isDevOptionsUnlocked())
    }

    private fun initClickListeners() {
        findPreference<Preference>("pref_messages")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_security")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_server")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_notifications")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_user_interface")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_data")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_support")?.onPreferenceClickListener = this
        findPreference<Preference>("pref_developer_options")?.onPreferenceClickListener = this
    }

    private fun initIntents() {
//        findPreference<Preference>("pref_user_interface").intent = Intent(activity, IntroActivity::class.java)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        if (!canClick()) return false
        when (preference?.key) {
            "pref_messages" -> {
                startActivity(Intent(context, MessagesSettingsActivity::class.java))
            }

            "pref_notifications" -> {
                startActivity(Intent(context, NotificationsSettingsActivity::class.java))
            }

            "pref_security" -> {
                val appBase = activity?.application as AppBase
                if (appBase.safeModeManager.isSafeMode) {
                    Toast.makeText(
                        activity,
                        R.string.access_to_security_settings_temporarily_denied,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startActivity(Intent(context, SecuritySettingsActivity::class.java))
                }
            }

            "pref_server" -> {
                startActivity(Intent(context, NodeInfoActivity::class.java))
            }

            "pref_developer_options" -> {
                startActivity(Intent(context, DeveloperOptionsActivity::class.java))
            }

            else -> Toast.makeText(
                activity,
                R.string.this_feature_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    private fun canClick(): Boolean {
        val available = SystemClock.elapsedRealtime() - mLastClickTime > MIN_CLICK_TIME
        if (available) {
            mLastClickTime = SystemClock.elapsedRealtime()
        }
        return available
    }

    fun showDeveloperOptions(show: Boolean) {
        findPreference<Preference>("pref_developer_options")?.isVisible = show
    }

    companion object {
        private const val MIN_CLICK_TIME = 600
    }
}