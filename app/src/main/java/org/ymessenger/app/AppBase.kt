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

package org.ymessenger.app

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.iid.FirebaseInstanceId
import org.ymessenger.app.data.local.db.AppDatabase
import org.ymessenger.app.data.remote.ClientRequestHandler
import org.ymessenger.app.data.remote.LicensorWSService
import org.ymessenger.app.data.remote.NotificationHandler
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.di.Injection
import org.ymessenger.app.helpers.*
import org.ymessenger.app.utils.AppExecutors
import y.encrypt.YEncrypt
import java.security.SecureRandom
import kotlin.random.Random

class AppBase : Application(), LifecycleObserver {

    private val licensorWSService = LicensorWSService()

    private val encryptionWrapper = EncryptionWrapper()

    private val webSocketService = WebSocketService(encryptionWrapper)

    lateinit var authorizationManager: AuthorizationManager
        private set

    lateinit var nodeManager: NodeManager
        private set

    lateinit var notificationHandler: NotificationHandler
        private set

    lateinit var clientRequestHandler: ClientRequestHandler
        private set

    lateinit var safeModeManager: SafeModeManager
        private set

    var appInBackground = false
        private set

    val settingsHelper by lazy { SettingsHelper(this) }

    /**
     * Show if app is locked when used PIN to lock
     */
    var isLocked = false

    companion object {
        private const val TAG = "AppBase"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        receiveToken()

        // TODO: THIS IS FOR OLD USERS ONLY. THIS SHOULD BE DELETED IN ONE OF NEXT UPDATES
        if (!settingsHelper.isFirstLaunch() && settingsHelper.getYEncryptPass() == null && !settingsHelper.getAskedOldUsersToSetPassphrase()) {
            settingsHelper.setYEncryptPass("qwerty")
        }

        // Initialize YEncrypt library
        initYEncrypt()

        licensorWSService.connect()

        isLocked = settingsHelper.hasPin()
        val executors = AppExecutors.getInstance()
        val firebaseConfig = FirebaseConfig()

        nodeManager = NodeManager(
            settingsHelper,
            firebaseConfig,
            executors,
            object : NodeManager.NodeCallback {
                override fun nodeFound(node: Node) {
                    webSocketService.connect()
                }

                override fun badNode(node: Node) {
                    Toast.makeText(applicationContext, R.string.this_node_is_bad, Toast.LENGTH_LONG)
                        .show()
                }

                override fun failedToVerifyNode() {
                    Toast.makeText(
                        applicationContext,
                        R.string.failed_to_verify_node,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun setEncryptedConnection(
                    symmetricKey: ByteArray,
                    myPrivateSignKey: ByteArray,
                    nodePublicSignKey: ByteArray
                ) {
                    webSocketService.startEncryptedConnection(
                        symmetricKey,
                        myPrivateSignKey,
                        nodePublicSignKey
                    )
                }
            }, Injection.provideNodeRepository(this)
        )

        safeModeManager = SafeModeManager(
            Injection.provideContactRepository(this),
            Injection.provideContactGroupRepository(this),
            Injection.provideChatPreviewRepository(this)
        )

        webSocketService.onConnected = {
            if (encryptionWrapper.isInitialized()) {
                val yEncrypt = encryptionWrapper.getYEncrypt()
                nodeManager.verifyNode(yEncrypt, webSocketService)
                if (settingsHelper.getUseEncryptedConnection()) {
                    nodeManager.setConnectionEncrypted(encryptionWrapper)
                }
            } else {
                Log.w(TAG, "Can't verify node. YEncrypt is not initialized yet.")
            }
        }

        webSocketService.onReconnect = {
            nodeManager.reloadCurrentNode()
        }

        val database = AppDatabase.getInstance(this)

        val messageRepository = Injection.provideMessageRepository(this)
        val chatPreviewRepository = Injection.provideChatPreviewRepository(this)

        notificationHandler = NotificationHandler(
            messageRepository,
            chatPreviewRepository,
            Injection.provideChatRepository(this),
            Injection.provideChatUserRepository(this),
            Injection.provideChatMapper(),
            Injection.provideChatUserMapper(),
            Injection.provideKeysMapper(),
            encryptionWrapper,
            Injection.provideKeysRepository(this),
            Injection.provideUserActionRepository(this)
        )

        clientRequestHandler = ClientRequestHandler(
            encryptionWrapper,
            Injection.provideKeysRepository(this),
            KeysGeneratorHelper(encryptionWrapper),
            webSocketService,
            Injection.provideKeysMapper()
        )

        webSocketService.setNotificationHandler(notificationHandler)
        webSocketService.setClientRequestHandler(clientRequestHandler)
        val messageUpdater = MessageUpdater(messageRepository, webSocketService, settingsHelper)
        val localDataManager = LocalDataManager(settingsHelper, database)
        authorizationManager = AuthorizationManager(
            webSocketService,
            messageUpdater,
            settingsHelper,
            localDataManager,
            Injection.provideUserRepository(this),
            Injection.provideUserMapper()
        )

        notificationHandler.onNewSession = {
            MyNotificationManager.showNewSessionNotification(this, it)
        }

        // This is for handling app state (background/foreground)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun initYEncrypt() {
        val seed = SecureRandom.getSeed(64)
        YEncrypt.init(seed)

        val passphrase = settingsHelper.getYEncryptPass()
        if (passphrase != null) {
            val passId = settingsHelper.getYEncryptMPID()
            setupYEncrypt(passphrase, passId)
        }

        checkFastSymmetricKeyIsCreated()
    }

    private fun checkFastSymmetricKeyIsCreated() {
        if (settingsHelper.getFastSymmetricKey() == null) {
            Log.d(TAG, "Fast symmetric key does not exist. Generating...")

            val password = Random.nextBytes(128)
            val salt = Random.nextBytes(16)

            val key = YEncrypt.GenerateKey(
                1L,
                password,
                salt,
                0L
            )

            Log.d(TAG, "Fast symmetric key is generated")
            settingsHelper.setFastSymmetricKey(key)
        }
    }

    fun setupYEncrypt(passphrase: String, passId: Long) {
        encryptionWrapper.init(passphrase, passId)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Log.d(TAG, "App runs background")
        isLocked = settingsHelper.hasPin()
        appInBackground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "App runs foreground")
        appInBackground = false
    }

    fun getWebSocketService() = webSocketService

    fun getLicensorWSService() = licensorWSService

    private fun receiveToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "getInstanceId failed", task.exception)
                return@addOnCompleteListener
            }

            task.result?.token?.let {
                settingsHelper.setFirebaseToken(it)
                Log.d(TAG, "Firebase token - $it")
            }
        }
    }

    fun getEncryptionWrapper() = encryptionWrapper

}