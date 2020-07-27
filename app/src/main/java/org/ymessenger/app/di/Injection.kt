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

package org.ymessenger.app.di

import android.content.Context
import android.media.AudioManager
import org.ymessenger.app.AppBase
import org.ymessenger.app.data.local.db.AppDatabase
import org.ymessenger.app.data.mappers.*
import org.ymessenger.app.data.remote.*
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.helpers.*
import org.ymessenger.app.utils.AppExecutors
import org.ymessenger.app.viewmodels.*

object Injection {

    fun provideExecutors(): AppExecutors {
        return AppExecutors.getInstance()
    }

    fun provideDatabase(context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    fun provideWebSocketService(appBase: AppBase): WebSocketService {
        return appBase.getWebSocketService()
    }

    fun provideLicensorWSService(appBase: AppBase): LicensorWSService {
        return appBase.getLicensorWSService()
    }

    fun provideSettingsHelper(appBase: AppBase): SettingsHelper {
        return appBase.settingsHelper
    }

    fun provideAuthorizationManager(appBase: AppBase): AuthorizationManager {
        return appBase.authorizationManager
    }

    fun provideNodeManager(appBase: AppBase): NodeManager {
        return appBase.nodeManager
    }

    fun provideFileApi(appBase: AppBase): FileApi {
        return ServiceGenerator.createService(
            FileApi::class.java,
            provideAuthorizationManager(appBase).fileAccessTokenLiveData
        )
    }

    fun provideChangeNodeApi(): ChangeNodeApi {
        return ChangeNodeApi.create()
    }

    fun provideEncryptionWrapper(appBase: AppBase): EncryptionWrapper {
        return appBase.getEncryptionWrapper()
    }

    fun provideSafeModeManager(appBase: AppBase): SafeModeManager {
        return appBase.safeModeManager
    }

    fun provideStringHelper(appBase: AppBase): StringHelper {
        return StringHelper(appBase)
    }

    fun provideValuesHelper(appBase: AppBase): ValuesHelper {
        return ValuesHelper(appBase)
    }

    fun provideImageCompressor(appBase: AppBase): ImageCompressor {
        return ImageCompressor(appBase)
    }

    fun provideAudioRecorder(appBase: AppBase): AudioRecorder {
        return AudioRecorder(
            appBase.getExternalFilesDir(null)
        )
    }

    fun provideVoicePlayerHelper(appBase: AppBase): VoicePlayerHelper {
        return VoicePlayerHelper(
            appBase.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
    }

    // ............................... MAPPERS ..................................

    fun provideChannelMapper(): ChannelMapper {
        return ChannelMapper()
    }

    fun provideChannelUserMapper(): ChannelUserMapper {
        return ChannelUserMapper()
    }

    fun provideChatPreviewMapper(): ChatPreviewMapper {
        return ChatPreviewMapper()
    }

    fun provideChatMapper(): ChatMapper {
        return ChatMapper()
    }

    fun provideChatUserMapper(): ChatUserMapper {
        return ChatUserMapper()
    }

    fun provideGroupMapper(): GroupMapper {
        return GroupMapper()
    }

    fun provideContactMapper(): ContactMapper {
        return ContactMapper()
    }

    fun provideDialogMapper(): DialogMapper {
        return DialogMapper()
    }

    fun provideKeysMapper(): KeysMapper {
        return KeysMapper()
    }

    fun provideMessageMapper(): MessageMapper {
        return MessageMapper()
    }

    fun provideRepliedMessageMapper(): RepliedMessageMapper {
        return RepliedMessageMapper()
    }

    fun provideUserMapper(): UserMapper {
        return UserMapper()
    }

    // ............................. REPOSITORIES ...............................

    fun provideContactGroupRepository(appBase: AppBase): ContactGroupRepository {
        val database = provideDatabase(appBase)

        return ContactGroupRepository.getInstance(
            provideExecutors(),
            database.contactGroupDao(),
            database.contactGroupUserDao(),
            database.contactDao(),
            provideWebSocketService(appBase),
            provideSettingsHelper(appBase),
            provideGroupMapper()
        )
    }

    fun provideGroupContactRepository(appBase: AppBase): GroupContactRepository {
        val database = provideDatabase(appBase)

        return GroupContactRepository.getInstance(
            provideExecutors(),
            database.contactGroupUserDao(),
            database.contactDao(),
            provideWebSocketService(appBase),
            provideSettingsHelper(appBase),
            provideContactMapper()
        )
    }

    fun provideContactRepository(appBase: AppBase): ContactRepository {
        val database = provideDatabase(appBase)

        return ContactRepository.getInstance(
            provideExecutors(),
            database.contactDao(),
            database.contactGroupUserDao(),
            provideWebSocketService(appBase),
            provideSettingsHelper(appBase),
            provideContactMapper()
        )
    }

    fun provideUserRepository(appBase: AppBase): UserRepository {
        val database = provideDatabase(appBase)

        return UserRepository.getInstance(
            provideExecutors(),
            database.userDao(),
            database.contactDao(),
            database.contactGroupDao(),
            database.contactGroupUserDao(),
            provideWebSocketService(appBase),
            provideGroupMapper(),
            provideContactMapper(),
            provideUserMapper()
        )
    }

    fun provideDialogRepository(appBase: AppBase): DialogRepository {
        val database = provideDatabase(appBase)

        return DialogRepository.getInstance(
            provideExecutors(),
            database.dialogDao(),
            provideWebSocketService(appBase),
            provideDialogMapper()
        )
    }

    fun providePollRepository(appBase: AppBase): PollRepository {
        return PollRepository.getInstance(
            provideExecutors(),
            provideWebSocketService(appBase),
            provideUserMapper(),
            provideDatabase(appBase).contactDao()
        )
    }

    fun provideFileRepository(appBase: AppBase): FileRepository {
        return FileRepository.getInstance(
            provideExecutors(),
            provideFileApi(appBase),
            appBase.getExternalFilesDir(null)
        )
    }

    fun provideChatRepository(appBase: AppBase): ChatRepository {
        val database = provideDatabase(appBase)

        return ChatRepository.getInstance(
            provideExecutors(),
            database.chatDao(),
            provideWebSocketService(appBase),
            provideChatMapper()
        )
    }

    fun provideChatUserRepository(appBase: AppBase): ChatUserRepository {
        val database = provideDatabase(appBase)

        return ChatUserRepository.getInstance(
            provideExecutors(),
            database.chatUserDao(),
            provideWebSocketService(appBase),
            provideChatUserMapper()
        )
    }

    fun provideChatPreviewRepository(appBase: AppBase): ChatPreviewRepository {
        val database = provideDatabase(appBase)

        return ChatPreviewRepository.getInstance(
            provideExecutors(),
            database.chatPreviewDao(),
            provideWebSocketService(appBase),
            provideChatPreviewMapper()
        )
    }

    fun provideChannelRepository(appBase: AppBase): ChannelRepository {
        val database = provideDatabase(appBase)

        return ChannelRepository.getInstance(
            provideExecutors(),
            database.channelDao(),
            provideWebSocketService(appBase),
            provideChannelMapper()
        )
    }

    fun provideMessageRepository(appBase: AppBase): MessageRepository {
        val database = provideDatabase(appBase)

        return MessageRepository.getInstance(
            provideExecutors(),
            database.messageDao(),
            database.attachmentDao(),
            database.repliedMessageDao(),
            database.forwardedMessageInfoDao(),
            database.keysDao(),
            database.symmetricKeyDao(),
            database.userDao(),
            database.chatDao(),
            database.channelDao(),
            provideEncryptionWrapper(appBase),
            provideWebSocketService(appBase),
            provideMessageMapper(),
            provideRepliedMessageMapper(),
            database.lastLoadedMessageIdDao()
        )
    }

    fun provideRepliedMessageRepository(appBase: AppBase): RepliedMessageRepository {
        return RepliedMessageRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).repliedMessageDao(),
            provideWebSocketService(appBase),
            provideRepliedMessageMapper()
        )
    }

    fun provideAttachmentRepository(appBase: AppBase): AttachmentRepository {
        return AttachmentRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).attachmentDao()
        )
    }

    fun provideProtectedConversationRepository(appBase: AppBase): ProtectedConversationRepository {
        return ProtectedConversationRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).protectedConversationDao()
        )
    }

    fun provideKeysRepository(appBase: AppBase): KeysRepository {
        return KeysRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).keysDao(),
            provideWebSocketService(appBase),
            provideKeysMapper()
        )
    }

    fun provideSymmetricKeyRepository(appBase: AppBase): SymmetricKeyRepository {
        return SymmetricKeyRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).symmetricKeyDao()
        )
    }

    fun provideNodeRepository(appBase: AppBase): NodeRepository {
        return NodeRepository.getInstance(
            provideExecutors(),
            provideWebSocketService(appBase),
            provideLicensorWSService(appBase),
            provideChangeNodeApi()
        )
    }

    fun provideChannelUserRepository(appBase: AppBase): ChannelUserRepository {
        val database = provideDatabase(appBase)

        return ChannelUserRepository.getInstance(
            provideExecutors(),
            database.channelUserDao(),
            database.userDao(),
            provideWebSocketService(appBase),
            provideChannelUserMapper()
        )
    }

    fun provideFavoriteConversationRepository(appBase: AppBase): FavoriteConversationRepository {
        return FavoriteConversationRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).favoriteConversationDao()
        )
    }

    fun provideSessionRepository(appBase: AppBase): SessionRepository {
        return SessionRepository.getInstance(
            provideWebSocketService(appBase)
        )
    }

    fun provideDraftMessageRepository(appBase: AppBase): DraftMessageRepository {
        return DraftMessageRepository.getInstance(
            provideExecutors(),
            provideDatabase(appBase).draftMessageDao()
        )
    }

    fun provideRandomSequenceRepository(appBase: AppBase): RandomSequenceRepository {
        return RandomSequenceRepository.getInstance(
            provideWebSocketService(appBase)
        )
    }

    fun provideVerificationRepository(appBase: AppBase): VerificationRepository {
        return VerificationRepository.getInstance(
            provideWebSocketService(appBase)
        )
    }

    fun provideQRCodeRepository(appBase: AppBase): QRCodeRepository {
        return QRCodeRepository.getInstance(
            provideWebSocketService(appBase)
        )
    }

    fun provideUserActionRepository(appBase: AppBase): UserActionRepository {
        return UserActionRepository.getInstance(
            provideDatabase(appBase).userActionDao(),
            provideWebSocketService(appBase)
        )
    }

    // ........................... VIEW MODEL FACTORIES ............................

    fun provideContactListViewModelFactory(appBase: AppBase): ContactListViewModel.Factory {
        return ContactListViewModel.Factory(
            provideContactGroupRepository(appBase)
        )
    }

    fun provideContactGroupPageViewModelFactory(
        appBase: AppBase,
        contactGroupId: String?
    ): ContactGroupPageViewModel.Factory {
        return ContactGroupPageViewModel.Factory(
            contactGroupId,
            provideGroupContactRepository(appBase),
            provideContactRepository(appBase),
            provideUserRepository(appBase)
        )
    }

    fun provideContactGroupListViewModelFactory(appBase: AppBase): ContactGroupListViewModel.Factory {
        return ContactGroupListViewModel.Factory(
            provideContactGroupRepository(appBase),
            provideContactRepository(appBase)
        )
    }

    fun provideContactGroupEditViewModelFactory(
        appBase: AppBase,
        contactGroupId: String?
    ): ContactGroupEditViewModel.Factory {
        return ContactGroupEditViewModel.Factory(
            contactGroupId,
            provideContactGroupRepository(appBase),
            provideGroupContactRepository(appBase)
        )
    }

    fun provideUserProfileViewModelFactory(
        appBase: AppBase,
        userId: Long
    ): UserProfileViewModel.Factory {
        return UserProfileViewModel.Factory(
            userId,
            provideUserRepository(appBase),
            provideContactRepository(appBase),
            provideDialogRepository(appBase),
            provideSettingsHelper(appBase),
            provideFavoriteConversationRepository(appBase)
        )
    }

    fun provideVotedUserListViewModelFactory(
        appBase: AppBase,
        pollId: String,
        optionId: Int,
        conversationId: Long,
        conversationType: Int,
        signRequired: Boolean
    ): VotedUserListViewModel.Factory {
        return VotedUserListViewModel.Factory(
            pollId, optionId, conversationId, conversationType, signRequired,
            providePollRepository(appBase),
            provideEncryptionWrapper(appBase),
            provideKeysRepository(appBase)
        )
    }

    fun provideSignUpViewModelFactory(appBase: AppBase): SignUpViewModel.Factory {
        return SignUpViewModel.Factory(
            provideUserRepository(appBase),
            provideAuthorizationManager(appBase),
            provideVerificationRepository(appBase),
            provideStringHelper(appBase)
        )
    }

    fun provideSettingsViewModelFactory(userId: Long, appBase: AppBase): SettingsViewModel.Factory {
        return SettingsViewModel.Factory(
            userId,
            provideUserRepository(appBase),
            provideFileRepository(appBase)
        )
    }

    fun provideSecuritySettingsViewModelFactory(
        appBase: AppBase,
        userId: Long
    ): SecuritySettingsViewModel.Factory {
        return SecuritySettingsViewModel.Factory(
            userId,
            provideUserRepository(appBase),
            provideSettingsHelper(appBase)
        )
    }

    fun provideGroupInfoViewModelFactory(
        appBase: AppBase,
        chatId: Long,
        userId: Long
    ): GroupInfoViewModel.Factory {
        return GroupInfoViewModel.Factory(
            chatId,
            userId,
            provideChatRepository(appBase),
            provideChatUserRepository(appBase),
            provideContactRepository(appBase),
            provideChatPreviewRepository(appBase),
            provideFavoriteConversationRepository(appBase)
        )
    }

    fun provideGlobalSearchViewModelFactory(appBase: AppBase): GlobalSearchViewModel.Factory {
        return GlobalSearchViewModel.Factory(
            provideUserRepository(appBase),
            provideChatRepository(appBase),
            provideChannelRepository(appBase)
        )
    }

    fun provideDialogViewModelFactory(appBase: AppBase, userId: Long): DialogViewModel.Factory {
        return DialogViewModel.Factory(
            userId,
            provideUserRepository(appBase),
            provideDialogRepository(appBase),
            provideMessageRepository(appBase),
            provideFileRepository(appBase),
            provideAttachmentRepository(appBase),
            provideProtectedConversationRepository(appBase),
            provideKeysRepository(appBase),
            provideSymmetricKeyRepository(appBase),
            provideEncryptionWrapper(appBase),
            providePollRepository(appBase),
            provideSettingsHelper(appBase),
            provideAuthorizationManager(appBase),
            provideDraftMessageRepository(appBase),
            provideImageCompressor(appBase),
            provideSafeModeManager(appBase),
            provideAudioRecorder(appBase),
            provideVoicePlayerHelper(appBase),
            provideUserActionRepository(appBase)
        )
    }

    fun provideDeveloperOptionsViewModelFactory(appBase: AppBase): DeveloperOptionsViewModel.Factory {
        return DeveloperOptionsViewModel.Factory(
            provideKeysRepository(appBase),
            provideSymmetricKeyRepository(appBase),
            provideSettingsHelper(appBase),
            provideUserRepository(appBase),
            provideChatRepository(appBase),
            provideChannelRepository(appBase),
            provideRepliedMessageRepository(appBase),
            provideContactRepository(appBase),
            provideContactGroupRepository(appBase)
        )
    }

    fun provideCreateGroupViewModelFactory(appBase: AppBase): CreateGroupViewModel.Factory {
        return CreateGroupViewModel.Factory(
            provideChatRepository(appBase),
            provideChannelRepository(appBase),
            provideContactRepository(appBase),
            provideFileRepository(appBase)
        )
    }

    fun provideConversationGalleryViewModelFactory(
        appBase: AppBase,
        conversationId: Long,
        conversationType: Int
    ): ConversationGalleryViewModel.Factory {
        return ConversationGalleryViewModel.Factory(
            conversationId,
            conversationType,
            provideAttachmentRepository(appBase)
        )
    }

    fun provideServerListViewModelFactory(appBase: AppBase): ServerListViewModel.Factory {
        return ServerListViewModel.Factory(
            provideNodeRepository(appBase)
        )
    }

    fun provideChatEditViewModelFactory(appBase: AppBase, chatId: Long): ChatEditViewModel.Factory {
        return ChatEditViewModel.Factory(
            chatId,
            provideChatRepository(appBase),
            provideFileRepository(appBase)
        )
    }

    fun provideChatViewModelFactory(appBase: AppBase, chatId: Long): ChatViewModel.Factory {
        return ChatViewModel.Factory(
            chatId,
            provideChatRepository(appBase),
            provideChatUserRepository(appBase),
            provideChatPreviewRepository(appBase),
            provideUserRepository(appBase),
            provideMessageRepository(appBase),
            provideFileRepository(appBase),
            provideAttachmentRepository(appBase),
            provideProtectedConversationRepository(appBase),
            providePollRepository(appBase),
            provideAuthorizationManager(appBase),
            provideDraftMessageRepository(appBase),
            provideEncryptionWrapper(appBase),
            provideKeysRepository(appBase),
            provideImageCompressor(appBase),
            provideAudioRecorder(appBase),
            provideVoicePlayerHelper(appBase),
            provideUserActionRepository(appBase)
        )
    }

    fun provideChannelUsersViewModelFactory(
        appBase: AppBase,
        channelId: Long
    ): ChannelUsersViewModel.Factory {
        return ChannelUsersViewModel.Factory(
            channelId,
            provideChannelRepository(appBase),
            provideChannelUserRepository(appBase),
            provideUserRepository(appBase),
            provideContactRepository(appBase)
        )
    }

    fun provideChannelInfoViewModelFactory(
        appBase: AppBase,
        channelId: Long,
        userId: Long
    ): ChannelInfoViewModel.Factory {
        return ChannelInfoViewModel.Factory(
            channelId,
            userId,
            provideChannelRepository(appBase),
            provideChannelUserRepository(appBase),
            provideContactRepository(appBase),
            provideChatPreviewRepository(appBase),
            provideFavoriteConversationRepository(appBase)
        )
    }

    fun provideChannelEditViewModelFactory(
        appBase: AppBase,
        channelId: Long
    ): ChannelEditViewModel.Factory {
        return ChannelEditViewModel.Factory(
            channelId,
            provideChannelRepository(appBase),
            provideFileRepository(appBase)
        )
    }

    fun provideChannelViewModelFactory(
        appBase: AppBase,
        channelId: Long
    ): ChannelViewModel.Factory {
        return ChannelViewModel.Factory(
            channelId,
            provideChannelRepository(appBase),
            provideChannelUserRepository(appBase),
            provideChatPreviewRepository(appBase),
            provideMessageRepository(appBase),
            provideFileRepository(appBase),
            provideAttachmentRepository(appBase),
            provideProtectedConversationRepository(appBase),
            providePollRepository(appBase),
            provideAuthorizationManager(appBase),
            provideDraftMessageRepository(appBase),
            provideEncryptionWrapper(appBase),
            provideKeysRepository(appBase),
            provideImageCompressor(appBase),
            provideAudioRecorder(appBase),
            provideVoicePlayerHelper(appBase)
        )
    }

    fun provideChatListViewModelFactory(appBase: AppBase): ChatListViewModel.Factory {
        return ChatListViewModel.Factory(
            provideUserRepository(appBase),
            provideChatPreviewRepository(appBase),
            provideMessageRepository(appBase),
            provideWebSocketService(appBase),
            provideAuthorizationManager(appBase),
            provideKeysRepository(appBase),
            provideSettingsHelper(appBase),
            provideFavoriteConversationRepository(appBase),
            provideUserActionRepository(appBase),
            provideContactRepository(appBase),
            provideEncryptionWrapper(appBase)
        )
    }

    fun provideMessageDataSourceFactory(
        appBase: AppBase,
        conversationType: Int,
        conversationId: Long
    ): MessageDataSourceFactory {
        return MessageDataSourceFactory(
            conversationType,
            conversationId,
            provideMessageRepository(appBase),
            provideKeysRepository(appBase),
            provideSymmetricKeyRepository(appBase),
            provideAttachmentRepository(appBase),
            provideEncryptionWrapper(appBase),
            provideSettingsHelper(appBase).getFastSymmetricKey()!!
        )
    }

    fun provideMessagesViewModelFactory(
        appBase: AppBase,
        conversationId: Long,
        conversationType: Int
    ): MessagesViewModel.Factory {
        return MessagesViewModel.Factory(
            conversationId,
            conversationType,
            provideMessageDataSourceFactory(appBase, conversationType, conversationId),
            provideMessageRepository(appBase),
            provideUserRepository(appBase),
            provideChannelRepository(appBase),
            provideRepliedMessageRepository(appBase)
        )
    }

    fun provideEnterPinViewModelFactory(
        appBase: AppBase,
        mode: EnterPinViewModel.Mode
    ): EnterPinViewModel.Factory {
        return EnterPinViewModel.Factory(
            mode,
            provideSettingsHelper(appBase),
            provideSafeModeManager(appBase)
        )
    }

    fun provideEnterViewModelFactory(appBase: AppBase): EnterViewModel.Factory {
        return EnterViewModel.Factory(
            provideSettingsHelper(appBase),
            provideNodeRepository(appBase),
            provideNodeManager(appBase)
        )
    }

    fun provideEmailLoginViewModelFactory(
        appBase: AppBase
    ): EmailLoginViewModel.Factory {
        return EmailLoginViewModel.Factory(
            provideAuthorizationManager(appBase),
            provideVerificationRepository(appBase),
            provideValuesHelper(appBase)
        )
    }

    fun providePhoneLoginViewModelFactory(
        appBase: AppBase
    ): PhoneLoginViewModel.Factory {
        return PhoneLoginViewModel.Factory(
            provideAuthorizationManager(appBase),
            provideVerificationRepository(appBase),
            provideValuesHelper(appBase)
        )
    }

    fun provideMessagesSettingsViewModelFactory(appBase: AppBase): MessagesSettingsViewModel.Factory {
        return MessagesSettingsViewModel.Factory(
            provideSettingsHelper(appBase)
        )
    }

    fun provideSessionsViewModelFactory(appBase: AppBase): SessionsViewModel.Factory {
        return SessionsViewModel.Factory(
            provideSessionRepository(appBase),
            provideAuthorizationManager(appBase)
        )
    }

    fun provideCheckEncryptionKeyViewModelFactory(
        conversationId: Long,
        appBase: AppBase
    ): CheckEncryptionKeyViewModel.Factory {
        return CheckEncryptionKeyViewModel.Factory(
            conversationId,
            provideEncryptionWrapper(appBase),
            provideSymmetricKeyRepository(appBase)
        )
    }

    fun provideBaseSecureViewerViewModelFactory(appBase: AppBase): BaseSecureViewerViewModel.Factory {
        return BaseSecureViewerViewModel.Factory(
            provideKeysRepository(appBase),
            provideSymmetricKeyRepository(appBase),
            provideEncryptionWrapper(appBase)
        )
    }

    fun provideSearchMessagesViewModelFactory(appBase: AppBase): SearchMessagesViewModel.Factory {
        return SearchMessagesViewModel.Factory(
            provideMessageRepository(appBase),
            provideChatPreviewRepository(appBase)
        )
    }

    fun providePhoneContactViewModelFactory(appBase: AppBase): PhoneContactsViewModel.Factory {
        return PhoneContactsViewModel.Factory(
            appBase.contentResolver
        )
    }

    fun provideRegisteredViewModelFactory(appBase: AppBase): RegisteredViewModel.Factory {
        return RegisteredViewModel.Factory(
            appBase.contentResolver,
            provideUserRepository(appBase),
            provideContactRepository(appBase),
            provideSettingsHelper(appBase)
        )
    }

    fun provideChangeEmailViewModelFactory(
        appBase: AppBase,
        userId: Long
    ): ChangeEmailViewModel.Factory {
        return ChangeEmailViewModel.Factory(
            userId,
            provideUserRepository(appBase),
            provideVerificationRepository(appBase)
        )
    }

    fun provideChangePhoneViewModelFactory(
        appBase: AppBase,
        userId: Long
    ): ChangePhoneViewModel.Factory {
        return ChangePhoneViewModel.Factory(
            userId,
            provideUserRepository(appBase),
            provideVerificationRepository(appBase)
        )
    }

    fun provideChangeAboutViewModelFactory(
        appBase: AppBase,
        userId: Long
    ): ChangeAboutViewModel.Factory {
        return ChangeAboutViewModel.Factory(
            userId,
            provideUserRepository(appBase)
        )
    }

    fun provideChangeNameViewModelFactory(
        appBase: AppBase,
        userId: Long
    ): ChangeNameViewModel.Factory {
        return ChangeNameViewModel.Factory(
            userId,
            provideUserRepository(appBase)
        )
    }

    fun provideSelectChatViewModelFactory(appBase: AppBase): SelectChatViewModel.Factory {
        return SelectChatViewModel.Factory(
            provideChatPreviewRepository(appBase),
            provideFavoriteConversationRepository(appBase)
        )
    }

    fun provideNodeInfoViewModelFactory(appBase: AppBase): NodeInfoViewModel.Factory {
        return NodeInfoViewModel.Factory(
            provideNodeRepository(appBase),
            provideChatPreviewRepository(appBase)
        )
    }

    fun provideQRLoginViewModelFactory(appBase: AppBase): QRLoginViewModel.Factory {
        return QRLoginViewModel.Factory(
            provideAuthorizationManager(appBase),
            provideNodeManager(appBase)
        )
    }

    fun provideGetQRCodeForLoginViewModelFactory(appBase: AppBase): GetQRCodeForLoginViewModel.Factory {
        return GetQRCodeForLoginViewModel.Factory(
            provideQRCodeRepository(appBase)
        )
    }

    fun provideEditContactViewModelFactory(
        contactId: String,
        appBase: AppBase
    ): EditContactViewModel.Factory {
        return EditContactViewModel.Factory(
            contactId,
            provideContactRepository(appBase)
        )
    }

    fun provideNotificationsSettingsViewModelFactory(appBase: AppBase): NotificationsSettingsViewModel.Factory {
        return NotificationsSettingsViewModel.Factory(
            provideSettingsHelper(appBase)
        )
    }

    fun provideSetPassphraseViewModelFactory(appBase: AppBase): SetPassphraseViewModel.Factory {
        return SetPassphraseViewModel.Factory(
            provideSettingsHelper(appBase)
        )
    }

    fun provideEnterPassphraseViewModelFactory(): EnterPassphraseViewModel.Factory {
        return EnterPassphraseViewModel.Factory()
    }
}