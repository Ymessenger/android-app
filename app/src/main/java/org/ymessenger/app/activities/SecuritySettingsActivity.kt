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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_security_settings.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.mappers.PrivacyConverter
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.SecuritySettingsViewModel

class SecuritySettingsActivity : BaseActivity() {

    private lateinit var viewModel: SecuritySettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_settings)
        initToolbar()

        val currentUserId =
            appBase.authorizationManager.getAuthorizedUserId()
                ?: throw NullPointerException("currentUserId is null")

        val factory = Injection.provideSecuritySettingsViewModelFactory(appBase, currentUserId)
        viewModel = ViewModelProviders.of(this, factory)
            .get(SecuritySettingsViewModel::class.java)

        btnPin.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            openPinSettings()
        }

        btnSessions.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            openSessionsSettings()
        }

        switchSyncContacts.isChecked = appBase.settingsHelper.getSyncContacts()
        switchEncryptedConnection.isChecked = appBase.settingsHelper.getUseEncryptedConnection()
        switchSavePassphrase.isChecked = appBase.settingsHelper.getSavePassphrase()

        switchEncryptedConnection.setOnCheckedChangeListener { buttonView, isChecked ->
            appBase.settingsHelper.setUseEncryptedConnection(isChecked)
            appBase.getWebSocketService().forceReconnect()
        }

        btnChangePassphrase.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            openChangePassphrase()
        }

        switchSavePassphrase.setOnCheckedChangeListener { buttonView, isChecked ->
            appBase.settingsHelper.setSavePassphrase(isChecked)
            if (isChecked) {
                SetPassphraseActivity.start(this, false)
            } else {
                appBase.settingsHelper.clearYEncryptPass()
            }
        }

        switchSyncContacts.setOnCheckedChangeListener { compoundButton, isChecked ->
            viewModel.setSyncContacts(isChecked)
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: SecuritySettingsViewModel) {
        viewModel.user.observe(this, Observer {
            updateUserPrivacy(it)
        })
    }

    private fun updateUserPrivacy(user: User) {
        val userPrivacy = PrivacyConverter.toBooleanArray(user.privacy) ?: return

        val nameTag = userPrivacy[1]
        val photoAbout = userPrivacy[2]
        val online = userPrivacy[14]
        val phone = userPrivacy[15]
        val email = userPrivacy[17]

        switchNameTag.isChecked = nameTag
        switchPhotoAbout.isChecked = photoAbout
        switchOnline.isChecked = online
        switchPhone.isChecked = phone
        switchEmail.isChecked = email
    }

    override fun onDestroy() {
        saveUserPrivacy()
        super.onDestroy()
    }

    private fun saveUserPrivacy() {
        val nameTag = switchNameTag.isChecked
        val online = switchOnline.isChecked
        val phone = switchPhone.isChecked
        val email = switchEmail.isChecked
        val photoAbout = switchPhotoAbout.isChecked

        viewModel.saveUserPrivacy(nameTag, online, phone, email, photoAbout)
    }

    private fun openPinSettings() {
        if (appBase.settingsHelper.hasPin()) {
            startActivityForResult(EnterPinActivity.enterModeIntent(this), REQUEST_CODE_ENTER_PIN)
        } else {
            startActivity(Intent(this, SettingsPinActivity::class.java))
        }
    }

    private fun openSessionsSettings() {
        startActivity(Intent(this, SessionsActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ENTER_PIN -> {
                if (resultCode == Activity.RESULT_OK) {
                    startActivity(Intent(this, SettingsPinActivity::class.java))
                }
            }
        }
    }

    private fun openChangePassphrase() {
        startActivity(Intent(this, SetPassphraseActivity::class.java))
    }

    companion object {
        private const val REQUEST_CODE_ENTER_PIN = 100
    }
}