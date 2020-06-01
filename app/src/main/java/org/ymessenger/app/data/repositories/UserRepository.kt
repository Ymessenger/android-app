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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.ymessenger.app.data.local.db.dao.ContactDao
import org.ymessenger.app.data.local.db.dao.ContactGroupDao
import org.ymessenger.app.data.local.db.dao.GroupContactDao
import org.ymessenger.app.data.local.db.dao.UserDao
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.local.db.entities.ContactGroup
import org.ymessenger.app.data.local.db.entities.GroupContact
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.local.db.models.UserModel
import org.ymessenger.app.data.mappers.ContactMapper
import org.ymessenger.app.data.mappers.GroupMapper
import org.ymessenger.app.data.mappers.UserMapper
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.*
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.Tokens
import org.ymessenger.app.data.remote.responses.Users
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors

class UserRepository private constructor(
    private val executors: AppExecutors,
    private val userDao: UserDao,
    private val contactDao: ContactDao,
    private val contactGroupDao: ContactGroupDao,
    private val groupContactDao: GroupContactDao,
    private val webSocketService: WebSocketService,
    private val groupMapper: GroupMapper,
    private val contactMapper: ContactMapper,
    private val userMapper: UserMapper
) {

    fun getUsers(): Listing<User> {
        val boundaryCallback = UserBoundaryCallback(
            executors,
            webSocketService,
            this::insertResultIntoDb
        )

        val factory = userDao.getUsers()

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(PAGE_SIZE)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Int, User>(factory, config)
            .setBoundaryCallback(boundaryCallback)
            .setFetchExecutor(executors.networkIO)
            .build()

        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) { refresh() }

        return Listing(
            livePagedListBuilder,
            refresh = {
                refreshTrigger.value = null
            },
            refreshState = refreshState
        )
    }

    private fun insertResultIntoDb(items: List<org.ymessenger.app.data.remote.entities.User>) {
        val dbItems = items.map { userMapper.toDb(it) }
        userDao.insertAll(dbItems)

        val dbContacts = arrayListOf<Contact>()
        val dbGroups = arrayListOf<ContactGroup>()
        val dbGroupContacts = arrayListOf<GroupContact>()

        for (user in items) {
            user.contact?.let {
                dbContacts.add(contactMapper.toDb(it))
                it.groupsId?.let { groupsId ->
                    for (groupId in groupsId) {
                        dbGroupContacts.add(GroupContact(groupId, it.contactId!!))
                    }
                }
            }

            user.groups?.let {
                // Inserting new group removes all group contacts.
                // We need to check for this group and if it exist - do not add it
                for (group in it) {
                    if (contactGroupDao.getContactGroupSync(group.groupId!!) == null) {
                        dbGroups.add(groupMapper.toDb(group))
                    }
                }
            }
        }

        if (dbContacts.isNotEmpty()) {
            contactDao.addAllContact(dbContacts)
        }

        if (dbGroups.isNotEmpty()) {
            contactGroupDao.insert(dbGroups)
        }

        if (dbGroupContacts.isNotEmpty()) {
            groupContactDao.insertGroupContacts(dbGroupContacts)
        }

        // TODO: delete contact and group contacts if [what?]
    }

    fun getUser(userId: Long, includeContact: Boolean = false): LiveData<User> {
        getUsersFromServer(listOf(userId), includeContact)
        return userDao.getUser(userId)
    }

    fun getUserModel(userId: Long, includeContact: Boolean = false): LiveData<UserModel> {
        getUsersFromServer(listOf(userId), includeContact)
        return userDao.getUserModel(userId)
    }

    fun getUsers(usersId: List<Long>): LiveData<List<User>> {
        getUsersFromServer(usersId)
        return userDao.getUsers(usersId)
    }

    fun getUsersFromServer(
        usersId: List<Long>,
        includeContact: Boolean = false
    ) {
        val getUsers = GetUsers(usersId, includeContact)
        webSocketService.getUsers(getUsers, object : WebSocketService.ResponseCallback<Users> {
            override fun onResponse(response: Users) {
                executors.diskIO.execute {
                    insertResultIntoDb(response.users)
                }
            }

            override fun onError(error: ResultResponse) {
                // nothing?
            }

        })
    }

    /**
     * Refresh all users. Firstly it sends request for first page. When result is come it clears database and insert
     * new users.
     */
    private fun refresh(): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>().apply {
            postValue(NetworkState.LOADING)
        }
        val getAllUsersInformationNode = GetAllUsersInformationNode()
        webSocketService.getAllUsersInformationNode(
            getAllUsersInformationNode,
            object : WebSocketService.ResponseCallback<Users> {
                override fun onResponse(response: Users) {
                    executors.diskIO.execute {
                        userDao.deleteAllUsers()
                        insertResultIntoDb(response.users)
                        networkState.postValue(NetworkState.LOADED)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get data")
                    networkState.postValue(NetworkState.error(error.message))
                }
            })
        return networkState
    }

    fun getSelf(userId: Long): LiveData<User> {
        val getSelf = GetSelf()
        webSocketService.getSelf(
            getSelf,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.User> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.User) {
                    val dbUser = userMapper.toDb(response.user)
                    executors.diskIO.execute {
                        userDao.insert(dbUser)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "GetSelf errors with code ${error.errorCode}")
                }
            })
        return userDao.getUser(userId)
    }

    fun getSelf(callback: (User) -> Unit, errorCallback: (ResultResponse) -> Unit) {
        val getSelf = GetSelf()
        webSocketService.getSelf(
            getSelf,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.User> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.User) {
                    val selfUser = userMapper.toDb(response.user)

                    callback.invoke(selfUser)
                }

                override fun onError(error: ResultResponse) {
                    // nothing
                }
            })
    }

    fun getUserSync(userId: Long) = userDao.getUserSync(userId)

    fun insert(remoteUser: org.ymessenger.app.data.remote.entities.User) {
        executors.diskIO.execute {
            insertResultIntoDb(listOf(remoteUser))
        }

    }

    fun updateUser(user: User) {
        executors.diskIO.execute {
            userDao.updateUser(user)
        }
    }

    fun searchUsers(searchQuery: String): Listing<User> {
        val sourceFactory = SearchUsersDataSourceFactory(searchQuery, webSocketService, userMapper)

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(PAGE_SIZE)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Long, User>(sourceFactory, config)
            .setFetchExecutor(executors.networkIO)
            .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }

        return Listing(
            livePagedListBuilder,
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    fun getUsersByPhones(phones: List<String>, callback: GetUsers) {
        val getUsersByPhones = GetUsersByPhones(phones)
        webSocketService.getUsersByPhones(
            getUsersByPhones,
            object : WebSocketService.ResponseCallback<Users> {
                override fun onResponse(response: Users) {
                    callback.result(response.users.map { userMapper.toDb(it) })
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get users by phones")
                    callback.error()
                }
            })
    }

    fun editUser(
        editUser: org.ymessenger.app.data.remote.entities.EditUser,
        callback: EditCallback
    ) {
        val editUserRequest = EditUser(editUser)
        webSocketService.editUser(
            editUserRequest,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.User> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.User) {
                    val dbUser = userMapper.toDb(response.user)
                    executors.diskIO.execute {
                        userDao.updateUser(dbUser)
                    }
                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "error while edit user.")
                    callback.error(error)
                }

            })
    }

    fun signUp(newUser: NewUser, callback: RegisterCallback) {
        webSocketService.newUser(newUser, object : WebSocketService.ResponseCallback<Tokens> {
            override fun onResponse(response: Tokens) {
                callback.registered(response)
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to register new user")
                callback.error(error)
            }
        })
    }

    fun editPhoneOrEmail(newValue: String, vCode: Int, callback: SuccessErrorCallback) {
        val editPhoneOrEmail = EditPhoneOrEmail(newValue, vCode)
        webSocketService.editPhoneOrEmail(
            editPhoneOrEmail,
            object : WebSocketService.ResponseCallback<Users> {
                override fun onResponse(response: Users) {
                    Log.d(TAG, "Phone or email was updated successfully")
                    executors.diskIO.execute {
                        insertResultIntoDb(response.users)
                    }
                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to update phone or email")
                    callback.error(error)
                }
            })
    }

    interface RegisterCallback {
        fun registered(tokens: Tokens)
        fun error(error: ResultResponse)
    }

    interface EditCallback {
        fun success()
        fun error(error: ResultResponse)
    }

    interface GetUsers {
        fun result(users: List<User>)
        fun error()
    }

    fun deleteAllLocally() {
        executors.diskIO.execute {
            userDao.deleteAllUsers()
        }
    }

    companion object {
        private const val TAG = "UserRepository"
        private var instance: UserRepository? = null

        private const val PAGE_SIZE = 100

        fun getInstance(
            executors: AppExecutors,
            userDao: UserDao,
            contactDao: ContactDao,
            contactGroupDao: ContactGroupDao,
            groupContactDao: GroupContactDao,
            webSocketService: WebSocketService,
            groupMapper: GroupMapper,
            contactMapper: ContactMapper,
            userMapper: UserMapper
        ): UserRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: UserRepository(
                        executors,
                        userDao,
                        contactDao,
                        contactGroupDao,
                        groupContactDao,
                        webSocketService,
                        groupMapper,
                        contactMapper,
                        userMapper
                    ).also { instance = it }
            }
        }
    }

}