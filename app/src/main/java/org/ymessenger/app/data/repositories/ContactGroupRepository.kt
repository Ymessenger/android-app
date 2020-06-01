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
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.ContactDao
import org.ymessenger.app.data.local.db.dao.ContactGroupDao
import org.ymessenger.app.data.local.db.dao.GroupContactDao
import org.ymessenger.app.data.local.db.entities.ContactGroup
import org.ymessenger.app.data.local.db.entities.GroupContact
import org.ymessenger.app.data.mappers.GroupMapper
import org.ymessenger.app.data.mappers.PrivacyConverter
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Group
import org.ymessenger.app.data.remote.requests.AddUsersToGroup
import org.ymessenger.app.data.remote.requests.CreateOrEditGroup
import org.ymessenger.app.data.remote.requests.DeleteGroups
import org.ymessenger.app.data.remote.requests.GetUserGroups
import org.ymessenger.app.data.remote.responses.Groups
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors
import java.util.*

class ContactGroupRepository private constructor(
    private val executors: AppExecutors,
    private val contactGroupDao: ContactGroupDao,
    private val groupContactDao: GroupContactDao,
    private val contactDao: ContactDao,
    private val webSocketService: WebSocketService,
    private val settingsHelper: SettingsHelper,
    private val groupMapper: GroupMapper
) {

    fun getContactGroups(): LiveData<List<ContactGroup>> {
        if (settingsHelper.getSyncContacts()) {
            val getUserGroups = GetUserGroups()
            webSocketService.getUserGroups(
                getUserGroups,
                object : WebSocketService.ResponseCallback<Groups> {
                    override fun onResponse(response: Groups) {
                        executors.diskIO.execute {
                            // We have to add synchronization here.
                            // Here we've got all groups from server.
                            // Now we need to fetch all groups from database and compare to list from server.
                            // If there are local groups that not in remote list, we should add them to server and delete locally.

                            val dbGroups = contactGroupDao.getContactGroupsSync()

                            if (dbGroups.isNotEmpty()) {
                                val dbGroupsToAddRemotely = arrayListOf<ContactGroup>()

                                for (dbGroup in dbGroups) {
                                    var found = false
                                    for (remoteGroup in response.groups) {
                                        if (dbGroup.id == remoteGroup.groupId) {
                                            found = true
                                        }
                                    }

                                    if (!found) {
                                        dbGroupsToAddRemotely.add(dbGroup)
                                    }
                                }

                                for (dbGroup in dbGroupsToAddRemotely) {
                                    createOrEditGroupFromLocal(dbGroup)
                                }
                            }

                            insertResultIntoDb(response.groups)
                        }
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to get user groups")
                    }
                })
        }

        return contactGroupDao.getContactGroups()
    }

    private fun insertResultIntoDb(groups: List<Group>) {
        val dbGroups = groups.map { groupMapper.toDb(it) }

        contactGroupDao.upsert(dbGroups)
    }

    fun getContactGroup(contactGroupId: String) = contactGroupDao.getContactGroup(contactGroupId)

    fun createOrEditContactGroup(group: Group, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val createOrEditGroup = CreateOrEditGroup(group)
            webSocketService.createOrEditGroup(
                createOrEditGroup,
                object : WebSocketService.ResponseCallback<Groups> {
                    override fun onResponse(response: Groups) {
                        executors.diskIO.execute {
                            insertResultIntoDb(response.groups)

                            Handler(Looper.getMainLooper()).post {
                                callback.success()
                            }
                        }
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to create or edit contact group")
                        callback.error(error)
                    }
                })
        } else {
            if (group.groupId == null) {
                group.groupId = UUID.randomUUID().toString()
            }
            executors.diskIO.execute {
                insertResultIntoDb(listOf(group))

                Handler(Looper.getMainLooper()).post {
                    callback.success()
                }
            }
        }
    }

    fun createOrEditGroupFromLocal(contactGroup: ContactGroup) {
        val privacy = PrivacyConverter.toBooleanArray(contactGroup.privacy) ?: booleanArrayOf()
        val group = Group(contactGroup.name)
        group.privacySettings = privacy
        group.groupId = contactGroup.id

        Log.d(TAG, "Creating contact group from local one...")
        val createOrEditGroup = CreateOrEditGroup(group)
        webSocketService.createOrEditGroup(
            createOrEditGroup,
            object : WebSocketService.ResponseCallback<Groups> {
                override fun onResponse(response: Groups) {
                    Log.d(TAG, "Contact group is uploaded")
                    executors.diskIO.execute {
                        // Upload group contacts to server
                        val dbGroupContacts =
                            groupContactDao.getContactsByContactGroupSync(contactGroup.id)
                        if (dbGroupContacts.isNotEmpty()) {
                            val usersId = dbGroupContacts.map { it.contact.userId }
                            uploadGroupContactsFromLocal(response.groups.first().groupId!!, usersId)
                        }

                        contactGroupDao.delete(contactGroup)
                        Log.d(TAG, "Local contact group is deleted")
                        insertResultIntoDb(response.groups)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to create or edit contact group from local")
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

    fun deleteContactGroups(groupsId: List<String>, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val deleteGroups = DeleteGroups(groupsId)
            webSocketService.deleteGroups(
                deleteGroups,
                object : WebSocketService.ResponseCallback<ResultResponse> {
                    override fun onResponse(response: ResultResponse) {
                        Log.d(TAG, "Contact groups deleted")
                        executors.diskIO.execute {
                            contactGroupDao.deleteContactGroups(groupsId)
                        }
                        callback.success()
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to delete contact groups")
                        callback.error(error)
                    }
                })
        } else {
            executors.diskIO.execute {
                contactGroupDao.deleteContactGroups(groupsId)
            }
            callback.success()
        }
    }

    fun updateContactGroup(contactGroup: ContactGroup, groupContactsId: List<String>) {
        executors.diskIO.execute {
            contactGroupDao.update(contactGroup)
            groupContactDao.removeAllUsersFromContactGroup(contactGroup.id)
            val groupContacts = arrayListOf<GroupContact>()
            for (contactId in groupContactsId) {
                groupContacts.add(GroupContact(contactGroup.id, contactId))
            }
            groupContactDao.insertGroupContacts(groupContacts)
        }
    }

    fun deleteAll() {
        executors.diskIO.execute {
            contactGroupDao.deleteAll()
        }
    }

    fun update(contactGroups: List<ContactGroup>) {
        executors.diskIO.execute {
            contactGroupDao.update(contactGroups)
        }
    }

    companion object {
        private const val TAG = "ContactGroupRepo"
        private var instance: ContactGroupRepository? = null

        fun getInstance(
            executors: AppExecutors,
            contactGroupDao: ContactGroupDao,
            groupContactDao: GroupContactDao,
            contactDao: ContactDao,
            webSocketService: WebSocketService,
            settingsHelper: SettingsHelper,
            groupMapper: GroupMapper
        ): ContactGroupRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: ContactGroupRepository(
                        executors,
                        contactGroupDao,
                        groupContactDao,
                        contactDao,
                        webSocketService,
                        settingsHelper,
                        groupMapper
                    ).also { instance = it }
            }
        }
    }
}