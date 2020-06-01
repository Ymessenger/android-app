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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_developer_options.*
import org.ymessenger.app.R
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.DeveloperOptionsViewModel

class DeveloperOptionsActivity : BaseActivity() {

    private lateinit var viewModel: DeveloperOptionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_options)

        initToolbar()

        initViewModel()

        btnDeleteAsymmetricKeys.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            deleteAsymmetricKeys()
        }

        btnDeleteSymmetricKeys.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            deleteSymmetricKeys()
        }

        tvFirebaseToken.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.copyFirebaseToken()
        }

        btnDeleteAllUsers.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.deleteAllUsers()
        }

        btnDeleteAllContacts.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.deleteAllContacts()
        }

        btnDeleteAllContactGroups.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.deleteAllContactGroups()
        }

        btnDeleteAllChats.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.deleteAllChats()
        }

        btnDeleteAllChannels.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.deleteAllChannels()
        }

        btnDeleteAllRepliedMessages.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.deleteAllRepliedMessages()
        }

        btnOpenPhoneContacts.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            startActivity(Intent(this, GetPhoneContactsActivity::class.java))
        }

        switchAlwaysOpenMainActivityAsAfterRegister.isChecked =
            viewModel.getAlwaysOpenMainActivityAsAfterRegister()
        switchAlwaysOpenMainActivityAsAfterRegister.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAlwaysOpenMainActivityAsAfterRegister(isChecked)
        }

        subscribeUi(viewModel)
    }

    private fun initViewModel() {
        val factory = Injection.provideDeveloperOptionsViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(DeveloperOptionsViewModel::class.java)
    }

    private fun subscribeUi(viewModel: DeveloperOptionsViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.firebaseToken.observe(this, Observer {
            tvFirebaseToken.text = it ?: "No data"
        })

        viewModel.copyFirebaseTokenEvent.observe(this, Observer {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.primaryClip =
                ClipData.newPlainText("Firebase token", it)
            showToast("Token is copied")
        })
    }

    private fun deleteAsymmetricKeys() {
        viewModel.deleteAsymmetricKeys()
    }

    private fun deleteSymmetricKeys() {
        viewModel.deleteSymmetricKeys()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dev_options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.mi_hide_dev_options -> {
                hideDevOptions()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideDevOptions() {
        appBase.settingsHelper.setDevOptionsUnlocked(false)
        finish()
    }
}