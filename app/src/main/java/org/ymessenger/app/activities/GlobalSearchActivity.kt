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
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_global_search.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.SearchResultPagerAdapter
import org.ymessenger.app.data.local.qr.entities.QRUserProfile
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.AndroidUtils
import org.ymessenger.app.viewmodels.GlobalSearchViewModel

class GlobalSearchActivity : BaseActivity() {

    private lateinit var searchResultPagerAdapter: SearchResultPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_search)
        initToolbar()

        val factory = Injection.provideGlobalSearchViewModelFactory(appBase)
        val viewModel = ViewModelProviders.of(this, factory).get(GlobalSearchViewModel::class.java)

        searchResultPagerAdapter = SearchResultPagerAdapter(this, supportFragmentManager)

        viewPager.adapter = searchResultPagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(v.text.toString().trim())
                AndroidUtils.hideKeyboard(v)
                true
            } else false
        }

        etSearch.requestFocus()

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: GlobalSearchViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.openDialogEvent.observe(this, Observer {
            DialogActivity.start(this, it)
        })

        viewModel.openChatEvent.observe(this, Observer {
            ChatActivity.startActivity(this, it)
        })

        viewModel.openChannelEvent.observe(this, Observer {
            ChannelActivity.startActivity(this, it)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_scan_qr -> {
                openScanQR()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openScanQR() {
        IntentIntegrator(this)
            .setPrompt(getString(R.string.scan_user_profile_qr_code))
            .initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // nothing
            } else {
                checkQRCode(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkQRCode(data: String) {
        val gson = Gson()
        try {
            val qrUserProfile = gson.fromJson(data, QRUserProfile::class.java)
            UserProfileActivity.startActivity(this, qrUserProfile.userId)
        } catch (e: Exception) {
            showError(R.string.qr_code_wrong_format)
        }
    }
}