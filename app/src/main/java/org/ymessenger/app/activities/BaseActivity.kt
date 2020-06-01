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
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.ymessenger.app.AppBase
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.NotificationHandler
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.helpers.MyNotificationManager
import org.ymessenger.app.interfaces.IBaseScreenEvents

open class BaseActivity : AppCompatActivity(), NotificationHandler.NewMessageListener,
    IBaseScreenEvents, NotificationHandler.NeedLoginListener {

    protected lateinit var appBase: AppBase
    private var afterPinInput = false

    private lateinit var progressDialog: ProgressDialog

    protected var mLastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appBase = application as AppBase
        initProgressDialog()

        appBase.authorizationManager.onNeedLoginEvent = {
            goToEnterActivity()
        }

        appBase.authorizationManager.userIsBannedEvent.observe(this, Observer {
            showError(R.string.you_are_banned)
        })
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        appBase.notificationHandler.setNewMessageListener(this)
        appBase.notificationHandler.setNeedLoginListener(this)

        if (shouldShowEnterPassphraseActivity()) {
            showEnterPassphraseActivity()
        }

        if (afterPinInput) return
        if (appBase.isLocked) {
            startActivityForResult(EnterPinActivity.enterModeIntent(this), REQUEST_CODE_ENTER_PIN)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ENTER_PIN -> {
                afterPinInput = true
                if (resultCode == Activity.RESULT_OK) {
                    appBase.isLocked = false
                } else {
                    // minimize app
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        afterPinInput = false
    }

    protected fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected fun initToolbar(_toolbar: Toolbar?) {
        setSupportActionBar(_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun showError(@StringRes errorMessage: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(errorMessage)
            .setNeutralButton(R.string.ok, null)
            .show()
    }

    override fun showToast(message: String, long: Boolean) {
        Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    override fun showToast(@StringRes message: Int) {
        showToast(getString(message))
    }

    private fun initProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setMessage(getString(R.string.loading))
    }

    override fun showLoadingDialog(message: Int?) {
        if (!progressDialog.isShowing) {
            progressDialog.setMessage(getString(message ?: R.string.loading))

            progressDialog.show()
        } else {
            progressDialog.setMessage(getString(message ?: R.string.loading))
        }
    }

    override fun hideLoadingDialog() {
        progressDialog.dismiss()
    }

    protected fun askToOpenSettings() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_is_required_for_this_feature_to_work)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                openApplicationSettings()
            }
            .show()
    }

    private fun openApplicationSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    override fun onMessageReceived(remoteMessage: Message) {
        if (remoteMessage.senderId != appBase.authorizationManager.getAuthorizedUserId()) {
            MyNotificationManager.showNewMessageNotification(appBase, remoteMessage)
        }
    }

    override fun onNeedLogin() {
        appBase.authorizationManager.forceLogout()
        goToEnterActivity()
    }

    protected fun canClick(): Boolean {
        val available = SystemClock.elapsedRealtime() - mLastClickTime > MIN_CLICK_TIME
        if (available) {
            mLastClickTime = SystemClock.elapsedRealtime()
        }
        return available
    }

    protected fun goToEnterActivity() {
        val intent = Intent(this, EnterActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    protected fun unsupportedOperation() {
        showToast(R.string.operation_is_not_supported)
    }

    protected fun setSecureFlag() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    protected fun clearSecureFlag() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    protected open fun shouldShowEnterPassphraseActivity(): Boolean {
        return !appBase.settingsHelper.getSavePassphrase() &&
                !appBase.getEncryptionWrapper().isInitialized()
    }

    private fun showEnterPassphraseActivity() {
        EnterPassphraseActivity.start(this)
    }

    companion object {
        private const val TAG = "BaseActivity"
        private const val REQUEST_CODE_ENTER_PIN = 500

        private const val MIN_CLICK_TIME = 600
    }

}