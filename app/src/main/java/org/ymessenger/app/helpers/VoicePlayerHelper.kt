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

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

class VoicePlayerHelper(
    private val audioManager: AudioManager
) {

    companion object {
        private const val TAG = "VoicePlayerHelper"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var sourceFileName: String? = null
    private var playbackCallback: PlaybackCallback? = null

    fun initMediaPlayer(playbackCallback: PlaybackCallback) {
        mediaPlayer = MediaPlayer()
        this.playbackCallback = playbackCallback
        mediaPlayer?.setOnCompletionListener {
            stop()
            playbackCallback.onComplete()
        }
    }

    fun setSource(filePath: String) {
        try {
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer?.prepare()
            sourceFileName = filePath
        } catch (e: Exception) {
            Log.e(TAG, "Error while set data source")
            e.printStackTrace()
            playbackCallback?.onError()
        }
    }

    fun setSource(decryptedBytes: ByteArray, filePath: String) {
        try {
            // It works for now, but maybe later it should be made with MediaDataSource
            val voiceBase64 = EncryptHelper.bytesToBase64(decryptedBytes)
            val url = "data:audio/mp3;base64,$voiceBase64"
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.prepare()
            sourceFileName = filePath
        } catch (e: Exception) {
            Log.e(TAG, "Error while set data source")
            e.printStackTrace()
            playbackCallback?.onError()
        }
    }

    fun isSourceSet(filePath: String): Boolean {
        sourceFileName?.let {
            return it == filePath
        } ?: return false
    }

    fun isSourceSet() = sourceFileName != null

    fun play(filePath: String, playingCallback: () -> Unit) {
        if (!audioManager.isMusicActive) {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } else {
            Log.w(TAG, "Music is active now. Skip")
            playingCallback.invoke()
        }
    }

    fun play() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playing")
            e.printStackTrace()
            playbackCallback?.onError()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause")
            e.printStackTrace()
            playbackCallback?.onError()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            sourceFileName = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop")
            e.printStackTrace()
            playbackCallback?.onError()
        }
    }

    interface PlaybackCallback {
        fun onComplete()
        fun onError()
    }

}