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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_enter_passphrase.*
import org.ymessenger.app.R
import org.ymessenger.app.data.Limitations
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.SimpleTextWatcher
import org.ymessenger.app.viewmodels.EnterPassphraseViewModel

class EnterPassphraseActivity : BaseActivity() {

    private lateinit var viewModel: EnterPassphraseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_passphrase)

        initToolbar()

        val factory = Injection.provideEnterPassphraseViewModelFactory()
        viewModel =
            ViewModelProviders.of(this, factory).get(EnterPassphraseViewModel::class.java)

        etPassphrase.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                viewModel.setPassphrase(text)
            }
        })

        btnEnter.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            viewModel.enter()
        }

        etPassphrase.imeOptions = EditorInfo.IME_ACTION_GO
        etPassphrase.setRawInputType(InputType.TYPE_CLASS_TEXT)
        etPassphrase.setOnEditorActionListener { textView, i, keyEvent ->
            viewModel.enter()
            return@setOnEditorActionListener false
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: EnterPassphraseViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.passphraseLength.observe(this, Observer {
            updateLength(it)
        })

        viewModel.setupYEncryptEvent.observe(this, Observer {
            appBase.setupYEncrypt(it, 0)
            finish()
        })
    }

    private fun updateLength(length: Int) {
        tvPassphraseLength.text =
            getString(
                R.string.length_of_length_placeholder,
                length,
                Limitations.MIN_PASSPHRASE_LENGTH
            )

        val color = if (length < Limitations.MIN_PASSPHRASE_LENGTH) {
            ContextCompat.getColor(this, R.color.colorMaterialRed500)
        } else {
            ContextCompat.getColor(this, R.color.colorMaterialGreen500)
        }
        tvPassphraseLength.setTextColor(color)
    }

    override fun onBackPressed() {
        showToast(R.string.this_action_cannot_be_skipped)
    }

    override fun shouldShowEnterPassphraseActivity(): Boolean {
        return false
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        fun getIntent(context: Context): Intent {
            val intent = Intent(context, EnterPassphraseActivity::class.java)

            return intent
        }
    }

}