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

import android.app.ProgressDialog
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.ymessenger.app.AppBase
import org.ymessenger.app.R
import org.ymessenger.app.interfaces.IBaseScreenEvents

open class BaseFragment : Fragment(), IBaseScreenEvents {

    protected lateinit var appBase: AppBase

    private lateinit var progressDialog: ProgressDialog

    protected var mLastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appBase = activity!!.application as AppBase

        // Init progress dialog
        progressDialog = ProgressDialog(activity)
        progressDialog.setCancelable(false)
        progressDialog.setMessage(getString(R.string.loading))
    }

    override fun showError(@StringRes errorMessage: Int) {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.error)
            .setMessage(errorMessage)
            .setNeutralButton(R.string.ok, null)
            .show()
    }

    override fun showToast(message: String, long: Boolean) {
        Toast.makeText(activity!!, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
            .show()
    }

    override fun showToast(@StringRes message: Int) {
        showToast(getString(message))
    }

    protected fun canClick(): Boolean {
        val available = SystemClock.elapsedRealtime() - mLastClickTime > MIN_CLICK_TIME
        if (available) {
            mLastClickTime = SystemClock.elapsedRealtime()
        }
        return available
    }

    override fun showLoadingDialog(message: Int?) {
        if (!progressDialog.isShowing) {
            progressDialog.setMessage(getString(message ?: R.string.loading))

            progressDialog.show()
        }
    }

    override fun hideLoadingDialog() {
        progressDialog.dismiss()
    }

    companion object {
        private const val MIN_CLICK_TIME = 600
    }
}