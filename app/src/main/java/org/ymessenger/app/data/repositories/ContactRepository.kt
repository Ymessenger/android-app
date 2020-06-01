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

import android.util.Log
import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.ContactDao
import org.ymessenger.app.data.local.db.dao.GroupContactDao
import org.ymessenger.app.data.local.db.entities.GroupContact
import org.ymessenger.app.data.local.db.models.ContactModel
import org.ymessenger.app.data.mappers.ContactMapper
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Contact
import org.ymessenger.app.data.remote.requests.CreateOrEditContact
import org.ymessenger.app.data.remote.requests.DeleteContacts
import org.ymessenger.app.data.remote.requests.GetUserContacts
import org.ymessenger.app.data.remote.responses.Contacts
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors
import java.util.*

class ContactRepository private constructor(
    private val executors: AppExecutors,
    private val contactDao: ContactDao,
    private val groupContactDao: GroupContactDao,
    private val webSocketService: WebSocketService,
    private val settingsHelper: SettingsHelper,
    private val contactMapper: ContactMapper
) {

    fun getContacts() = contactDao.getContacts()

    fun getContactModels(): LiveData<List<ContactModel>> {
        if (settingsHelper.getSyncContacts()) {
            loadAllContactsFromServer()
        }

        return contactDao.getContactModels()
    }

    fun loadAllContactsFromServer(
        contacts: ArrayList<Contact> = arrayListOf(),
        navigationUserId: Long? = null
    ) {
        val getUserContacts = GetUserContacts(navigationUserId)

        webSocketService.getUserContacts(
            getUserContacts,
            object : WebSocketService.ResponseCallback<Contacts> {
                override fun onResponse(response: Contacts) {
                    if (response.contacts.isEmpty()) {
                        Log.d(TAG, "All contacts are loaded")
                        // Here should be logic to sync contacts
                        // First, we need to get all contacts from server
                        // Then get all contacts from database and compare
                        // Find all local contacts who are not in remote contacts and save them to server, then remove local
                        // Also save contacts from remote to local database

                        executors.diskIO.execute {
                            val dbContacts = contactDao.getContactsSync()

                            if (dbContacts.isNotEmpty()) {
                                val contactsToSaveRemotely =
                                    arrayListOf<org.ymessenger.app.data.local.db.entities.Contact>()

                                for (dbContact in dbContacts) {
                                    var found = false
                                    for (contact in contacts) {
                                        if (dbContact.userId == contact.contactUserId) {
                                            found = true
                                        }
                                    }

                                    if (!found) {
                                        contactsToSaveRemotely.add(dbContact)
                                    }
                                }

                                for (contactToRemote in contactsToSaveRemotely) {
                                    addContactFromLocalToRemote(contactToRemote)
                                }
                            }

                            insertIntoDb(contacts)
                        }
                    } else {
                        Log.d(TAG, "Not end, loading next page")
                        contacts.addAll(response.contacts)
                        loadAllContactsFromServer(contacts, response.contacts.last().contactUserId)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get contacts")
                }
            })
    }

    fun getContactModelById(contactId: String) = contactDao.getContactModelById(contactId)

    fun getContactModelsById(contactsId: List<String>) = contactDao.getContactModelsById(contactsId)

    fun getContactModelsByName(name: String) = contactDao.getContactModelsByName(name)

    fun getContact(contactId: Long) = contactDao.getContact(contactId)

    fun getContactByUser(userId: Long) = contactDao.getContactByUser(userId)

    fun addContact(contact: Contact, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val createOrEditContact = CreateOrEditContact(contact)
            webSocketService.createOrEditContact(
                createOrEditContact,
                object : WebSocketService.ResponseCallback<Contacts> {
                    override fun onResponse(response: Contacts) {
                        Log.d(TAG, "Contact is created")

                        executors.diskIO.execute {
                            insertIntoDb(response.contacts)
                        }

                        callback.success()
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to add contact")
                        callback.error(error)
                    }
                })
        } else {
            contact.contactId = UUID.randomUUID().toString()
            executors.diskIO.execute {
                insertIntoDb(listOf(contact))
            }

            callback.success()
        }
    }

    fun editContact(contact: Contact, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val createOrEditContact = CreateOrEditContact(contact)
            webSocketService.createOrEditContact(
                createOrEditContact,
                object : WebSocketService.ResponseCallback<Contacts> {
                    override fun onResponse(response: Contacts) {
                        if (response.contacts.isNotEmpty()) {
                            val editedContact = response.contacts.first()
                            Log.d(TAG, "Contact was edited")

                            executors.diskIO.execute {
                                updateInDb(editedContact)
                            }

                            callback.success()
                        } else {
                            Log.e(TAG, "Response is empty")
                            callback.error(ResultResponse("Response is empty"))
                        }
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to edit contact")
                        callback.error(error)
                    }
                })
        } else {
            executors.diskIO.execute {
                updateInDb(contact)
            }

            callback.success()
        }
    }

    private fun updateInDb(contact: Contact) {
        val dbContact = contactMapper.toDb(contact)

        contactDao.updateContact(dbContact)
    }

    private fun addContactFromLocalToRemote(dbContact: org.ymessenger.app.data.local.db.entities.Contact) {
        Log.d(TAG, "Uploading local contact to remote server...")
        val contact = Contact(dbContact.userId, dbContact.name)

        val createOrEditContact = CreateOrEditContact(contact)
        webSocketService.createOrEditContact(
            createOrEditContact,
            object : WebSocketService.ResponseCallback<Contacts> {
                override fun onResponse(response: Contacts) {
                    Log.d(TAG, "Contact is uploaded")

                    executors.diskIO.execute {
                        contactDao.removeContact(dbContact)
                        Log.d(TAG, "Local contact is removed")
                        insertIntoDb(response.contacts)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to upload local contact")
                }
            })
    }

    fun deleteContacts(contactsId: List<String>, callback: SuccessErrorCallback) {
        if (settingsHelper.getSyncContacts()) {
            val deleteContacts = DeleteContacts(contactsId)
            webSocketService.deleteContacts(
                deleteContacts,
                object : WebSocketService.ResponseCallback<ResultResponse> {
                    override fun onResponse(response: ResultResponse) {
                        Log.d(TAG, "Contacts deleted [${contactsId.size}]")

                        executors.diskIO.execute {
                            contactDao.deleteContacts(contactsId)
                        }

                        callback.success()
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Failed to delete contacts")
                        callback.error(error)
                    }
                })
        } else {
            executors.diskIO.execute {
                contactDao.deleteContacts(contactsId)
            }
            callback.success()
        }

    }

    private fun insertIntoDb(contacts: List<Contact>) {
        val dbContacts = contacts.map { contactMapper.toDb(it) }

        contactDao.addAllContact(dbContacts)

        val dbGroupContacts = arrayListOf<GroupContact>()
        for (contact in contacts) {
            contact.groupsId?.let { groupsId ->
                for (groupId in groupsId) {
                    dbGroupContacts.add(GroupContact(groupId, contact.contactId!!))
                }
            }
        }

        if (dbGroupContacts.isNotEmpty()) {
            try {
                groupContactDao.insertGroupContacts(dbGroupContacts)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert group contacts")
                e.printStackTrace()
            }
        }
    }

    fun deleteAllContacts() {
        executors.diskIO.execute {
            contactDao.deleteAllContacts()
        }
    }

    companion object {
        private const val TAG = "ContactRepository"

        private var instance: ContactRepository? = null

        fun getInstance(
            executors: AppExecutors,
            contactDao: ContactDao,
            groupContactDao: GroupContactDao,
            webSocketService: WebSocketService,
            settingsHelper: SettingsHelper,
            contactMapper: ContactMapper
        ): ContactRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: ContactRepository(
                        executors,
                        contactDao,
                        groupContactDao,
                        webSocketService,
                        settingsHelper,
                        contactMapper
                    ).also { instance = it }
            }
        }
    }

}