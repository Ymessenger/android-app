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

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_notifications_settings.*
import org.ymessenger.app.R
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.NotificationsSettingsViewModel

class NotificationsSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications_settings)

        initToolbar()

        val factory = Injection.provideNotificationsSettingsViewModelFactory(appBase)
        val viewModel =
            ViewModelProviders.of(this, factory).get(NotificationsSettingsViewModel::class.java)

        switchHideNotificationContent.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateHideNotificationContent(isChecked)
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: NotificationsSettingsViewModel) {
        viewModel.hideNotificationContent.observe(this, Observer {
            switchHideNotificationContent.isChecked = it
        })
    }

}