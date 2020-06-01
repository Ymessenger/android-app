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

package org.ymessenger.app.data.repositories

import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.ContactDao
import org.ymessenger.app.data.local.db.dao.GroupContactDao
import org.ymessenger.app.data.local.db.entities.GroupContact
import org.ymessenger.app.data.local.db.models.ContactModel
import org.ymessenger.app.data.mappers.ContactMapper
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Contact
import org.ymessenger.app.data.remote.requests.AddUsersToGroup
import org.ymessenger.app.data.remote.requests.GetGroupContacts
import org.ymessenger.app.data.remote.requests.RemoveUsersFromGroup
import org.ymessenger.app.data.remote.responses.Contacts
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors

class GroupContactRepository private constructor(
    private val executors: AppExecutors,
    private val groupContactDao: GroupContactDao,
    private val contactDao: ContactDao,
    private val webSocketService: WebSocketService,
    private val settingsHelper: SettingsHelper,
    private val contactMapper: ContactMapper
) {

    fun getContactsByContactGroup(contactGroupId: String): LiveData<List<ContactModel>> {
        if (settingsHelper.getSyncContacts()) {
            loadAllGroupContactsFromServer(contactGroupId)
        }

        return groupContactDao.getContactsByContactGroup(contactGroupId)
    }

    private fun loadAllGroupContactsFromServer(
        contactGroupId: String,
        contacts: ArrayList<Contact> = arrayListOf(),
        navigationUserId: Long? = null
    ) {
        val getGroupContacts = GetGroupContacts(contactGroupId, navigationUserId)
        webSocketService.getGroupContacts(
            getGroupContacts,
            object : WebSocketService.ResponseCallback<Contacts> {
                override fun onResponse(response: Contacts) {
                    if (response.contacts.isEmpty()) {
                        Log.d(TAG, "All group contacts are loaded")
                        executors.diskIO.execute {
                            // Here we need to fetch group contacts from database
                            // Compare it to remote contacts and upload contacts who are not in remote database to server

                            val dbGroupContacts =
                                groupContactDao.getContactsByContactGroupSync(contactGroupId)
                            if (dbGroupContacts.isNotEmpty()) {
                                val usersIdToRemote = arrayListOf<Long>()

                                for (dbGroupContact in dbGroupContacts) {
                                    var found = false
                                    for (remoteContact in contacts) {
                                        if (dbGroupContact.contact.userId == remoteContact.contactUserId) {
                                            found = true
                                        }
                                    }

                                    if (!found) {
                                        usersIdToRemote.add(dbGroupContact.contact.userId)
                                    }
                                }

                                if (usersIdToRemote.isNotEmpty()) {
                                    uploadGroupContactsFromLocal(contactGroupId, usersIdToRemote)
                                }
                            }

                            insertResultIntoDb(contactGroupId, contacts)
                        }
                    } else {
                        Log.d(TAG, "Not end, loading next page")
                        contacts.addAll(response.contacts)
                        loadAllGroupContactsFromServer(
                            contactGroupId,
                            contacts,
                            response.contacts.last().contactUserId
                        )
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get group contacts")
                }
            })
    }

    fun uploadGroupContactsFromLocal(groupId: String, usersId: List<Long>, counter: Int = 1) {
        Log.d(TAG, "Uploading group contacts to server...")
        val addUsersToGroup = AddUsersToGroup(usersId, groupId)
        webSocketService.addUsersToGroup(
            addUsersToGroup,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    Log.d(TAG, "Group contacts are uploaded")
                    executors.diskIO.execute {
                        // 1. Get contacts from database by userId
                        // 2. Save group contacts

                        val dbGroupContacts = arrayListOf<GroupContact>()
                        val contacts = contactDao.getContactsByUsersId(usersId)
                        for (contact in contacts) {
                            dbGroupContacts.add(GroupContact(groupId, contact.id))
                        }
                        groupContactDao.insertGroupContacts(dbGroupContacts)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "$counter attempt: Failed to upload users to group")
                    if (counter < 5) {
                        Log.d(TAG, "Try again in a second...")
                        Handler().postDelayed({
                            uploadGroupContactsFromLocal(groupId, usersId, counter + 1)
                        }, 1000)
                    } else {
                        Log.e(TAG, "No more attempts. Abort")
                        Log.d(TAG, "Saving group contacts locally...")
                        executors.diskIO.execute {
                            // 1. Get contacts from database by userId
                            // 2. Save group contacts

                            val dbGroupContacts = arrayListOf<GroupContact>()
                            val contacts = contactDao.getContactsByUsersId(usersId)
                            if (contacts.isNotEmpty()) {
                                for (contact in contacts) {
                                    dbGroupContacts.add(GroupContact(groupId, contact.id))
                                }
                                groupContactDao.insertGroupContacts(dbGroupContacts)
                                Log.d(TAG, "Group contacts are saved locally")
                            } else {
                                Log.w(TAG, "Nothing to save")
                            }
                        }
                    }
                }
            })
    }

    fun getContactsByGroupAndName(groupId: String, name: String): LiveData<List<ContactModel>> {
        return groupContactDao.getContactsByGroupAndName(groupId, name)
    }

    private fun insertResultIntoDb(contactGroupId: String, contacts: List<Contact>) {
        val dbContacts = contacts.map { contactMapper.toDb(it) }

        contactDao.addAllContact(dbContacts)

        val dbContactGroup = dbContacts.map {
            GroupContact(contactGroupId, it.id)
        }

        groupContactDao.insertGroupContacts(dbContactGroup)
    }

    fun addContactGroupUser(groupContact: GroupContact) {
        executors.diskIO.execute {
            groupContactDao.insertGroupContact(groupContact)
        }
    }

    fun removeContactGroupUser(groupContact: GroupContact) {
        executors.diskIO.execute {
            groupContactDao.removeContactGroupUser(groupContact)
        }
    }

    fun addUsers(groupId: String, usersId: List<Long>, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val addUsersToGroup = AddUsersToGroup(usersId, groupId)
            webSocketService.addUsersToGroup(
                addUsersToGroup,
                object : WebSocketService.ResponseCallback<ResultResponse> {
                    override fun onResponse(response: ResultResponse) {
                        executors.diskIO.execute {
                            // save to DB but check server answer first
                            // 1. Get contacts from database by userId
                            // 2. Save group contacts

                            val dbGroupContacts = arrayListOf<GroupContact>()
                            val contacts = contactDao.getContactsByUsersId(usersId)
                            for (contact in contacts) {
                                dbGroupContacts.add(GroupContact(groupId, contact.id))
                            }
                            groupContactDao.insertGroupContacts(dbGroupContacts)
                        }
                        callback.success()
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to add users to group")
                        callback.error(error)
                    }
                })
        } else {
            executors.diskIO.execute {
                // save to DB but check server answer first
                // 1. Get contacts from database by userId
                // 2. Save group contacts

                val dbGroupContacts = arrayListOf<GroupContact>()
                val contacts = contactDao.getContactsByUsersId(usersId)
                for (contact in contacts) {
                    dbGroupContacts.add(GroupContact(groupId, contact.id))
                }
                groupContactDao.insertGroupContacts(dbGroupContacts)
            }
            callback.success()
        }
    }

    fun removeUsers(groupId: String, usersId: List<Long>, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val removeUsersFromGroup = RemoveUsersFromGroup(usersId, groupId)
            webSocketService.removeUsersFromGroup(
                removeUsersFromGroup,
                object : WebSocketService.ResponseCallback<ResultResponse> {
                    override fun onResponse(response: ResultResponse) {
                        executors.diskIO.execute {
                            // 1. Get contacts from database by userId
                            // 2. Remove group contacts

                            val contacts = contactDao.getContactsByUsersId(usersId)
                            val contactsId = contacts.map { it.id }
                            groupContactDao.removeGroupContacts(groupId, contactsId)
                        }
                        callback.success()
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to remove users from group")
                        callback.error(error)
                    }
                })
        } else {
            executors.diskIO.execute {
                // 1. Get contacts from database by userId
                // 2. Remove group contacts

                val contacts = contactDao.getContactsByUsersId(usersId)
                val contactsId = contacts.map { it.id }
                groupContactDao.removeGroupContacts(groupId, contactsId)
            }
            callback.success()
        }
    }

    companion object {
        private const val TAG = "GroupContactRepo"
        private var instance: GroupContactRepository? = null

        fun getInstance(
            executors: AppExecutors,
            groupContactDao: GroupContactDao,
            contactDao: ContactDao,
            webSocketService: WebSocketService,
            settingsHelper: SettingsHelper,
            contactMapper: ContactMapper
        ): GroupContactRepository {
            return instance ?: synchronized(this) {
                instance ?: GroupContactRepository(
                    executors,
                    groupContactDao,
                    contactDao,
                    webSocketService,
                    settingsHelper,
                    contactMapper
                ).also { instance = it }
            }
        }
    }
}