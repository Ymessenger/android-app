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

package org.ymessenger.app.activities

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings_pin.*
import org.ymessenger.app.R

class SettingsPinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_pin)
        initToolbar()

        switchUsePin.isChecked = appBase.settingsHelper.hasPin()
        switchUseSafePin.isChecked = appBase.settingsHelper.hasSafePin()
        switchUseSafePin.isEnabled = switchUsePin.isChecked

        switchUsePin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openEnterPinActivity()
            } else {
                appBase.settingsHelper.clearPin()
                switchUseSafePin.isChecked = false
            }

            switchUseSafePin.isEnabled = isChecked
        }

        switchUseSafePin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openEnterSafePinActivity()
            } else {
                appBase.settingsHelper.clearSafePin()
            }
        }
    }

    private fun openEnterPinActivity() {
        startActivityForResult(EnterPinActivity.setPinModeIntent(this), REQUEST_CODE_SET_PIN)
    }

    private fun openEnterSafePinActivity() {
        startActivityForResult(
            EnterPinActivity.setSafePinModeIntent(this),
            REQUEST_CODE_SET_SAFE_PIN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SET_PIN -> {
                if (!appBase.settingsHelper.hasPin()) {
                    switchUsePin.isChecked = false
                }
            }

            REQUEST_CODE_SET_SAFE_PIN -> {
                if (!appBase.settingsHelper.hasSafePin()) {
                    switchUseSafePin.isChecked = false
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SET_PIN = 100
        private const val REQUEST_CODE_SET_SAFE_PIN = 101
    }
}