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

import android.media.MediaRecorder
import android.util.Log
import java.io.File

class AudioRecorder(
    private val externalPath: File
) {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val AUDIO_DIR_NAME = "audio"
    }

    private var mediaRecorder: MediaRecorder? = null

    private val outputDir = File(externalPath, AUDIO_DIR_NAME)

    private var output: String? = null

    private fun initRecorder() {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        output = getFilepath()

        mediaRecorder = MediaRecorder()

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)
    }

    private fun generateFilename(): String {
        return "audio_${System.currentTimeMillis()}.mp3"
    }

    private fun getFilepath(): String {
        return "${outputDir.absolutePath}/${generateFilename()}"
    }

    fun startRecording() {
        Log.d(TAG, "Starting recording!")
        initRecorder()
        mediaRecorder?.prepare()
        mediaRecorder?.start()
    }

    fun stopRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        Log.d(TAG, "Stop recording!")
    }

    fun getRecordingFilename() = output

}