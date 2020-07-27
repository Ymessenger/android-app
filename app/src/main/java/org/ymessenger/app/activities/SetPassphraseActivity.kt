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
import kotlinx.android.synthetic.main.activity_set_pass_phrase.*
import org.ymessenger.app.R
import org.ymessenger.app.data.Limitations
import org.ymessenger.app.di.Injection
import org.ymessenger.app.helpers.PassphraseGenerator
import org.ymessenger.app.utils.SimpleTextWatcher
import org.ymessenger.app.viewmodels.SetPassphraseViewModel

class SetPassphraseActivity : BaseActivity() {

    private lateinit var viewModel: SetPassphraseViewModel

    private var modeCloseable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pass_phrase)

        initToolbar()

        modeCloseable = intent.getBooleanExtra(ARG_CLOSEABLE, true)

        if (!modeCloseable) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        val factory = Injection.provideSetPassphraseViewModelFactory(appBase)
        viewModel =
            ViewModelProviders.of(this, factory).get(SetPassphraseViewModel::class.java)

        etPassPhrase.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                viewModel.setPassphrase(text)
            }
        })

        btnGeneratePass.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            generatePassphrase()
        }

        btnSave.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            viewModel.savePassphrase()
        }

        etPassPhrase.imeOptions = EditorInfo.IME_ACTION_DONE
        etPassPhrase.setRawInputType(InputType.TYPE_CLASS_TEXT)
        etPassPhrase.setOnEditorActionListener { textView, i, keyEvent ->
            viewModel.savePassphrase()
            return@setOnEditorActionListener false
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: SetPassphraseViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.currentPassphrase.observe(this, Observer {
            etPassPhrase.setText(it)
        })

        viewModel.passphraseLength.observe(this, Observer {
            updateLength(it)
        })

        viewModel.setupYEncryptEvent.observe(this, Observer {
            appBase.setupYEncrypt(it, 0)
        })

        viewModel.doneEvent.observe(this, Observer {
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
        if (modeCloseable) {
            super.onBackPressed()
        } else {
            showToast(R.string.this_action_cannot_be_skipped)
        }
    }

    private fun generatePassphrase() {
        val passphrase = PassphraseGenerator.generate(this)
        etPassPhrase.setText(passphrase)
    }

    companion object {
        private const val ARG_CLOSEABLE = "ARG_CLOSEABLE"

        fun start(context: Context, closeable: Boolean) {
            context.startActivity(getIntent(context, closeable))
        }

        fun getIntent(context: Context, closeable: Boolean): Intent {
            val intent = Intent(context, SetPassphraseActivity::class.java)
            intent.putExtra(ARG_CLOSEABLE, closeable)

            return intent
        }
    }

}