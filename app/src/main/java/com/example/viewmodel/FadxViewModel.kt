package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FadxRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Main : Screen("main")
    object Search : Screen("search")
    object Messages : Screen("messages")
    data class ChatConversation(val chatId: String) : Screen("chat/$chatId")
    object EditProfile : Screen("edit_profile")
    object CreatePost : Screen("create_post")
    data class UserProfile(val userId: String) : Screen("profile/$userId")
    data class GroupDetail(val groupId: String) : Screen("group/$groupId")
    data class PageDetail(val pageId: String) : Screen("page/$pageId")
    object CreateGroup : Screen("create_group")
    object CreatePage : Screen("create_page")
    object CreateProduct : Screen("create_product")
    object CreateEvent : Screen("create_event")
    object AdminPanel : Screen("admin_panel")
    object SavedItems : Screen("saved_items")
    object Settings : Screen("settings")
    object Friends : Screen("friends")
    object Groups : Screen("groups")
    object Pages : Screen("pages")
    object Marketplace : Screen("marketplace")
    object Events : Screen("events")
}

class FadxViewModel(
    val repository: FadxRepository = FadxRepository()
) : ViewModel() {

    // Current Screen / Route Navigation Stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenBackStack = mutableListOf<Screen>()

    // Main Bottom Navigation Tab (0: Home, 1: Videos, 2: Create, 3: Notifications, 4: Menu)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Video Section Filter
    private val _videoTab = MutableStateFlow(0) // 0: Shorts/Reels, 1: Feed
    val videoTab: StateFlow<Int> = _videoTab.asStateFlow()

    private val _videoFeedCategory = MutableStateFlow("Recommended")
    val videoFeedCategory: StateFlow<String> = _videoFeedCategory.asStateFlow()

    // Active Story Viewer State
    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    // Comments Sheet State
    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    val activeCommentPost: StateFlow<Post?> = _activeCommentPost.asStateFlow()

    // Report Dialog State
    private val _reportingItem = MutableStateFlow<Triple<String, String, String>?>(null) // (type, id, summary)
    val reportingItem: StateFlow<Triple<String, String, String>?> = _reportingItem.asStateFlow()

    // Search Query & Category
    val searchQuery = MutableStateFlow("")
    val searchCategory = MutableStateFlow("All") // All, People, Posts, Videos, Groups, Pages, Hashtags

    // Global Snackbar / Toast messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Flow bindings from Repository
    val currentUser = repository.currentUser
    val isAuthenticated = repository.isAuthenticated
    val themeMode = repository.themeMode
    val stories = repository.stories
    val posts = repository.posts
    val videos = repository.videos
    val friendRequests = repository.friendRequests
    val friendsList = repository.friendsList
    val chatThreads = repository.chatThreads
    val messagesMap = repository.messagesMap
    val groups = repository.groups
    val pages = repository.pages
    val products = repository.products
    val events = repository.events
    val notifications = repository.notifications
    val savedItems = repository.savedItems
    val reports = repository.reports
    val privacySettings = repository.privacySettings
    val notificationSettings = repository.notificationSettings
    val twoFactorEnabled = repository.twoFactorEnabled

    // Search filtered results
    val searchResults: StateFlow<Map<String, List<Any>>> = combine(
        searchQuery,
        searchCategory,
        posts,
        videos
    ) { query, category, allPosts, allVideos ->
        if (query.isBlank()) {
            emptyMap<String, List<Any>>()
        } else {
            val q = query.trim().lowercase()
            val users = repository.sampleUsers
            val allGroups = repository.groups.value
            val allPages = repository.pages.value
            mapOf<String, List<Any>>(
                "people" to (users + currentUser.value).filter { it.name.lowercase().contains(q) || it.username.lowercase().contains(q) },
                "posts" to allPosts.filter { it.text.lowercase().contains(q) || it.author.name.lowercase().contains(q) },
                "videos" to allVideos.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) },
                "groups" to allGroups.filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) },
                "pages" to allPages.filter { it.name.lowercase().contains(q) || it.bio.lowercase().contains(q) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun navigateTo(screen: Screen) {
        _screenBackStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (_screenBackStack.isNotEmpty()) {
            _currentScreen.value = _screenBackStack.removeAt(_screenBackStack.size - 1)
            return true
        }
        return false
    }

    fun selectTab(tab: Int) {
        if (tab == 2) {
            // "Create" button opens Create Post screen
            navigateTo(Screen.CreatePost)
        } else {
            _selectedTab.value = tab
            if (_currentScreen.value !is Screen.Main) {
                _currentScreen.value = Screen.Main
            }
        }
    }

    fun setVideoTab(index: Int) {
        _videoTab.value = index
    }

    fun setVideoFeedCategory(cat: String) {
        _videoFeedCategory.value = cat
    }

    fun openStory(story: Story) {
        repository.markStoryViewed(story.id)
        _activeStory.value = story
    }

    fun closeStory() {
        _activeStory.value = null
    }

    fun openComments(post: Post) {
        _activeCommentPost.value = post
    }

    fun closeComments() {
        _activeCommentPost.value = null
    }

    fun openReportDialog(type: String, id: String, summary: String) {
        _reportingItem.value = Triple(type, id, summary)
    }

    fun closeReportDialog() {
        _reportingItem.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // Auth flows
    fun login(identifier: String, pass: String) {
        repository.login(identifier, pass)
        _currentScreen.value = Screen.Main
        showToast("Welcome back to Fadx!")
    }

    fun signUp(name: String, username: String, email: String, phone: String, pass: String, dob: String, gender: String) {
        repository.signUp(name, username, email, phone, pass, dob, gender)
        _currentScreen.value = Screen.Main
        showToast("Account created successfully!")
    }

    fun logout() {
        repository.logout()
        _screenBackStack.clear()
        _currentScreen.value = Screen.Welcome
        showToast("Logged out")
    }

    // Direct actions
    fun toggleReaction(postId: String, reaction: ReactionType) {
        repository.toggleReaction(postId, reaction)
    }

    fun addComment(postId: String, text: String) {
        repository.addComment(postId, text)
        _activeCommentPost.value = posts.value.find { it.id == postId }
    }

    fun createPost(text: String, mediaUrls: List<String> = emptyList(), visibility: PostVisibility = PostVisibility.PUBLIC, location: String? = null) {
        repository.createPost(text, mediaUrls, visibility = visibility, location = location)
        navigateBack()
        showToast("Post shared with ${visibility.label}")
    }

    fun deletePost(postId: String) {
        repository.deletePost(postId)
        showToast("Post deleted")
    }

    fun toggleSavePost(post: Post) {
        repository.toggleSavePost(post)
        showToast(if (!post.isSaved) "Saved to collection" else "Removed from saved")
    }

    fun addStory(text: String, mediaUrl: String = "", type: StoryType = StoryType.TEXT) {
        repository.addStory(text, mediaUrl, type)
        showToast("Story shared for 24 hours!")
    }

    fun toggleVideoLike(videoId: String) {
        repository.toggleVideoLike(videoId)
    }

    fun acceptFriendRequest(requestId: String) {
        repository.acceptFriendRequest(requestId)
        showToast("Friend request accepted")
    }

    fun declineFriendRequest(requestId: String) {
        repository.declineFriendRequest(requestId)
        showToast("Request removed")
    }

    fun removeFriend(userId: String) {
        repository.removeFriend(userId)
        showToast("Friend removed")
    }

    fun sendMessage(chatId: String, text: String, type: MessageType = MessageType.TEXT, mediaUrl: String? = null) {
        repository.sendMessage(chatId, text, type, mediaUrl)
    }

    fun startChatWithUser(user: User) {
        val chatId = repository.getOrCreateChat(user)
        navigateTo(Screen.ChatConversation(chatId))
    }

    fun toggleJoinGroup(groupId: String) {
        repository.toggleJoinGroup(groupId)
        showToast("Group membership updated")
    }

    fun createGroup(name: String, description: String, category: String, isPrivate: Boolean) {
        repository.createGroup(name, description, category, isPrivate)
        navigateBack()
        showToast("Group created successfully!")
    }

    fun toggleFollowPage(pageId: String) {
        repository.toggleFollowPage(pageId)
        showToast("Page following updated")
    }

    fun createPage(name: String, handle: String, category: PageCategory, bio: String) {
        repository.createPage(name, handle, category, bio)
        navigateBack()
        showToast("Page created successfully!")
    }

    fun createProduct(title: String, price: Double, description: String, category: ProductCategory, location: String, imageUrl: String) {
        repository.createProduct(title, price, description, category, location, imageUrl)
        navigateBack()
        showToast("Product listing published on Marketplace")
    }

    fun toggleEventStatus(eventId: String, isGoing: Boolean) {
        repository.toggleEventStatus(eventId, isGoing)
        showToast(if (isGoing) "Marked as Going 🎉" else "Marked as Interested ✨")
    }

    fun markAllNotificationsRead() {
        repository.markAllNotificationsRead()
        showToast("All notifications marked as read")
    }

    fun updateProfile(name: String, bio: String, location: String, website: String, avatar: String?, cover: String?) {
        repository.updateProfile(name, bio, location, website, avatar, cover)
        navigateBack()
        showToast("Profile updated successfully")
    }

    fun submitReport(reason: ReportReason) {
        val item = _reportingItem.value ?: return
        repository.submitReport(item.first, item.second, item.third, reason)
        closeReportDialog()
        showToast("Thank you. Report received for review.")
    }

    fun resolveReport(reportId: String, action: String) {
        repository.resolveReport(reportId, action)
        showToast("Report resolved: $action")
    }

    fun dismissReport(reportId: String) {
        repository.dismissReport(reportId)
        showToast("Report dismissed")
    }

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
    }

    fun toggle2FA() {
        repository.toggle2FA()
        showToast("Two-Factor Authentication toggled")
    }
}
