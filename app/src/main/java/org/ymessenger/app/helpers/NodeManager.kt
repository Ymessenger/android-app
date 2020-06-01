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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.ymessenger.app.data.remote.UrlGenerator
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.remote.requests.SetConnectionEncrypted
import org.ymessenger.app.data.remote.requests.VerifyNode
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.NodeRepository
import org.ymessenger.app.utils.AppExecutors
import y.encrypt.YEncrypt
import java.net.Socket
import java.security.SecureRandom

class NodeManager(
    private val settingsHelper: SettingsHelper,
    private val firebaseConfig: FirebaseConfig,
    private val executors: AppExecutors,
    private val nodeCallback: NodeCallback,
    private val nodeRepository: NodeRepository
) {
    private val node = MutableLiveData<Node>()

    init {
        settingsHelper.getNode()?.let {
            node.value = it
            if (it.clientsPort == 0) {
                nodeCallback.badNode(it)
            } else {
                UrlGenerator.setBaseUrlAndPort(getBaseUrl()!!, it.clientsPort)
                nodeCallback.nodeFound(it)
            }
        }
    }

    fun setCurrentNode(node: Node) {
        this.node.value = node
        if (node.clientsPort == 0) {
            nodeCallback.badNode(node)
        } else {
            settingsHelper.setNode(node)
            UrlGenerator.setBaseUrlAndPort(getBaseUrl()!!, node.clientsPort)
            nodeCallback.nodeFound(node)
        }
    }

    fun getCurrentNode() = node

    fun hasNode() = node.value != null

    private fun getBaseUrl() = node.value?.domains?.firstOrNull()

    fun autoSelectNode(nodes: List<Node>) {
        executors.networkIO.execute {
            val bestNode = getNodeWithSmallestPing(nodes)
            bestNode?.let {
                // We can't change LiveData from background thread
                Handler(Looper.getMainLooper()).post {
                    setCurrentNode(it)
                }
            }
        }
    }

    /**
     * This method returns node with smallest average ping to connect to
     *
     * @param nodes List of available nodes
     *
     * @return Best node to connect to. May be null if nothing found.
     */
    private fun getNodeWithSmallestPing(nodes: List<Node>): Node? {
        var smallestPing = Long.MAX_VALUE
        var bestNode: Node? = null

        for (node in nodes) {
            try {
                val hostAddress = node.domains!!.first()
                val port = node.clientsPort
                val ping = getAveragePing(hostAddress, port, 3)
                if (ping < smallestPing) {
                    smallestPing = ping
                    bestNode = node
                }
            } catch (e: Exception) {
//                e.printStackTrace()
                Log.e("Ping", "Failed to resolve server")
                continue
            }
        }

        return bestNode
    }

    /**
     * Returns the average ping of given server url
     *
     * @param hostname Host address
     * @param port Host port
     * @param repeat How many times server should be pinged
     *
     * @return Average time in milliseconds
     */
    private fun getAveragePing(hostname: String, port: Int, repeat: Int = 1): Long {
        var avgPing = 0L

        repeat(repeat) {
            val ping = getPing(hostname, port)
            avgPing += ping
        }

        avgPing /= repeat
        Log.d("Average Ping", "$hostname - $avgPing ms")

        return avgPing
    }

    /**
     * Simply returns ping of given server url
     *
     * @param hostname Host address
     * @param port Host port
     *
     * @return Time in milliseconds
     */
    private fun getPing(hostname: String, port: Int): Long {
        val start = System.currentTimeMillis()
        val socket = Socket(hostname, port)
        socket.close()
        val finish = System.currentTimeMillis()
        val ping = finish - start
        Log.d("Ping", "$hostname - $ping ms")

        return ping
    }

    interface NodeCallback {
        fun nodeFound(node: Node)
        fun badNode(node: Node)
        fun failedToVerifyNode()
        fun setEncryptedConnection(
            symmetricKey: ByteArray,
            myPrivateSignKey: ByteArray,
            nodePublicSignKey: ByteArray
        )
    }

    fun verifyNode(yEncrypt: YEncrypt, webSocketService: WebSocketService) {
        Log.d(TAG, "Start node verification")

        val randomBytes = ByteArray(128)
        SecureRandom().nextBytes(randomBytes)
        val sequence = EncryptHelper.bytesToBase64(randomBytes)
        // TODO: get random sequence from server and concatenate with randomBytes

        val lifeTime = 30L

        try {
            val licensorKeys = firebaseConfig.getLicensorKeys()

            val mySignKeys = yEncrypt.getShortAsymmetricKeys(1, 123, lifeTime, true)
            Log.d(TAG, "Generated short sign keys")

            val symmetricKey = yEncrypt.getSymmetricKey(1, 12345, lifeTime)
            Log.d(TAG, "Symmetric key generated: ${EncryptHelper.bytesToBase64(symmetricKey)}")

            yEncrypt.publicEncryptKeyToSend = EncryptHelper.base64ToBytes(licensorKeys.encryptKey)
            yEncrypt.privateSignKeyToSend = mySignKeys.privateKey

            val encryptedSymmetricKey = yEncrypt.encrypKeysMsg(1, lifeTime, symmetricKey)

            val verifyNode =
                VerifyNode(
                    sequence,
                    EncryptHelper.bytesToBase64(mySignKeys.publicKey),
                    EncryptHelper.bytesToBase64(encryptedSymmetricKey)
                )
            webSocketService.verifyNode(
                verifyNode,
                object :
                    WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Sequence> {
                    override fun onResponse(response: org.ymessenger.app.data.remote.responses.Sequence) {
                        try {
                            yEncrypt.publicSignKeyToReceive =
                                EncryptHelper.base64ToBytes(licensorKeys.signKey)
                            yEncrypt.setSymmetricEncryptKey(symmetricKey)
                            val data =
                                yEncrypt.decryptSecretMsg(EncryptHelper.base64ToBytes(response.sequence))
                            val decryptedSequence = EncryptHelper.bytesToBase64(data.msg)
                            if (decryptedSequence == sequence) {
                                Log.d(TAG, "Node is verified!")
                            } else {
                                Log.e(
                                    TAG,
                                    "Failed to verify node! Decrypted sequence doesn't match with original"
                                )
                                nodeCallback.failedToVerifyNode()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, "Failed to verify node! Failed to decrypt received sequence")
                            nodeCallback.failedToVerifyNode()
                        }
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to perform request")
                        nodeCallback.failedToVerifyNode()
                    }
                })

        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify node")
            e.printStackTrace()
            nodeCallback.failedToVerifyNode()
        }
    }

    fun setConnectionEncrypted(encryptionWrapper: EncryptionWrapper) {
        nodeRepository.getInformationCurrentNode({ node ->
            Log.d(TAG, "Got node \'${node.name}\'")

            executors.diskIO.execute {
                val keysGeneratorHelper = KeysGeneratorHelper(encryptionWrapper)
                val encryptKeys =
                    keysGeneratorHelper.getAsymmetricKeys(KeysGeneratorHelper.Length.SHORT, false)
                val signKeys =
                    keysGeneratorHelper.getAsymmetricKeys(KeysGeneratorHelper.Length.SHORT, true)

                val setConnectionEncrypted = SetConnectionEncrypted(
                    EncryptHelper.bytesToBase64(encryptKeys.keyPair.publicKey),
                    node.id,
                    EncryptHelper.bytesToBase64(signKeys.keyPair.publicKey)
                )
                nodeRepository.setConnectionEncrypted(setConnectionEncrypted, { encryptedData ->
                    Log.d(
                        TAG,
                        "We got encrypted symmetric key. Decrypt it and use to encrypt/decrypt all data in webSocketService"
                    )

                    val nodeSignPublicKey = EncryptHelper.base64ToBytes(node.nodeKey.signPublicKey)

                    try {
                        val yEncrypt = encryptionWrapper.getYEncrypt()

                        yEncrypt.privateEncryptKeyToReceive = encryptKeys.keyPair.privateKey
                        yEncrypt.publicSignKeyToReceive = nodeSignPublicKey

                        val decryptedKey =
                            yEncrypt.decrypKeysMsg(EncryptHelper.base64ToBytes(encryptedData))
                        val symmetricKey = decryptedKey.key

                        Log.d(
                            TAG,
                            "Symmetric key for encrypted connection: ${EncryptHelper.bytesToBase64(
                                symmetricKey
                            )}"
                        )

                        // Set all keys to webSocketService and set encryptedConnection flag
                        nodeCallback.setEncryptedConnection(
                            symmetricKey,
                            signKeys.keyPair.privateKey,
                            nodeSignPublicKey
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to decrypt symmetric key")
                        e.printStackTrace()
                    }
                }, {
                    Log.e(TAG, "Failed to set connection encrypted")
                })
            }
        }, {
            Log.e(TAG, "Error while getting current node information")
        })
    }

    fun reloadCurrentNode() {
        settingsHelper.getNode()?.let {
            Log.d(TAG, "Reloading current node")
            nodeRepository.getInformationNode(listOf(it.id), { nodes ->
                if (nodes.isNotEmpty()) {
                    Log.d(TAG, "Current node reloaded")
                    setCurrentNode(nodes.first())
                }
            }, {
                Log.e(TAG, "Failed to reload current node")
            })
        } ?: Log.w(TAG, "Current node is null")
    }

    fun switchToNode(nodeId: Long, callback: () -> Unit, error: () -> Unit) {
        nodeRepository.getInformationNode(listOf(nodeId), {
            // success
            if (it.isNotEmpty()) {
                val node = it.first()
                setCurrentNode(node)
                callback.invoke()
            } else {
                Log.e(TAG, "Node not found")
                error.invoke()
            }
        }, {
            // error
            Log.e(TAG, "Node not found")
            error.invoke()
        })
    }

    companion object {
        private const val TAG = "NodeManager"
    }
}