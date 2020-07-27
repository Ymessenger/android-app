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

package org.ymessenger.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.ymessenger.app.data.local.db.dao.*
import org.ymessenger.app.data.local.db.entities.*

private const val DATABASE_VERSION = 11

@Database(
    entities = [
        User::class,
        Contact::class,
        ContactGroup::class,
        GroupContact::class,
        Message::class,
        RepliedMessage::class,
        ForwardedMessageInfo::class,
        Attachment::class,
        Chat::class,
        Dialog::class,
        Channel::class,
        ChatUser::class,
        ChannelUser::class,
        ChatPreview::class,
        ProtectedConversation::class,
        Keys::class,
        SymmetricKey::class,
        FavoriteConversation::class,
        DraftMessage::class,
        UserAction::class,
        LastLoadedMessageId::class
    ],
    version = DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun contactGroupDao(): ContactGroupDao
    abstract fun contactGroupUserDao(): GroupContactDao
    abstract fun messageDao(): MessageDao
    abstract fun repliedMessageDao(): RepliedMessageDao
    abstract fun forwardedMessageInfoDao(): ForwardedMessageInfoDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun chatDao(): ChatDao
    abstract fun channelDao(): ChannelDao
    abstract fun dialogDao(): DialogDao
    abstract fun chatUserDao(): ChatUserDao
    abstract fun channelUserDao(): ChannelUserDao
    abstract fun chatPreviewDao(): ChatPreviewDao
    abstract fun protectedConversationDao(): ProtectedConversationDao
    abstract fun keysDao(): KeysDao
    abstract fun symmetricKeyDao(): SymmetricKeyDao
    abstract fun favoriteConversationDao(): FavoriteConversationDao
    abstract fun draftMessageDao(): DraftMessageDao
    abstract fun userActionDao(): UserActionDao
    abstract fun lastLoadedMessageIdDao(): LastLoadedMessageIdDao

    companion object {
        private const val DATABASE_NAME = "ymessenger_db"
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_9_10)
                .addMigrations(MIGRATION_10_11)
                .fallbackToDestructiveMigration() // Probably should be deleted
                .build()
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN node_id INTEGER")
                database.execSQL("ALTER TABLE symmetric_keys ADD COLUMN creator_user_id INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `last_loaded_message_id` (`conversation_id` INTEGER NOT NULL, `conversation_type` INTEGER NOT NULL, `global_id` TEXT NOT NULL, PRIMARY KEY(`conversation_id`, `conversation_type`))")
            }
        }
    }

}