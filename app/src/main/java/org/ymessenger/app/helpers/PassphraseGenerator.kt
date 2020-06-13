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

package org.ymessenger.app.helpers

import android.content.Context
import android.util.Log
import org.ymessenger.app.data.Limitations

object PassphraseGenerator {

    private const val TAG = "PassphraseGenerator"

    fun generate(context: Context): String {
        val fileName = "passphrase_words.txt"
        val wordsString = context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }

        if (wordsString.isNotBlank()) {
            // Remove " symbols
            val cleanWordsString = wordsString.replace("\"", "")
            val wordsArray = cleanWordsString.split(",")
            Log.d(TAG, "Passphrase words - ${wordsArray.size}, random word: ${wordsArray.random()}")

            // Generate passphrase
            val sb = StringBuilder()
            while (getLengthWithoutSpaces(sb.toString()) < Limitations.MIN_PASSPHRASE_LENGTH) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(wordsArray.random())
            }
            val passphrase = sb.toString()
            Log.d(TAG, "Generated passphrase: $passphrase")

            return passphrase
        } else {
            return ""
        }
    }

    private fun getLengthWithoutSpaces(text: String): Int {
        return text.replace(" +".toRegex(), "").length
    }
}