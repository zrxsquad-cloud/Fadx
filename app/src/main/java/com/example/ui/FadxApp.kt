package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.auth.*
import com.example.ui.screens.create.*
import com.example.ui.screens.home.*
import com.example.ui.screens.hubs.*
import com.example.ui.screens.menu.*
import com.example.ui.screens.messages.*
import com.example.ui.screens.profile.*
import com.example.ui.screens.search.*
import com.example.ui.screens.videos.*
import com.example.ui.theme.FadxTheme
import com.example.viewmodel.FadxViewModel
import com.example.viewmodel.Screen

@Composable
fun FadxApp(
    viewModel: FadxViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    // Modal / Overlay states
    val activeStory by viewModel.activeStory.collectAsState()
    val activeCommentPost by viewModel.activeCommentPost.collectAsState()
    val reportingItem by viewModel.reportingItem.collectAsState()
    var activeSharePost by remember { mutableStateOf<Post?>(null) }

    // Toast listener
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Data streams
    val stories by viewModel.stories.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val videoTab by viewModel.videoTab.collectAsState()
    val videoCategory by viewModel.videoFeedCategory.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val friendsList by viewModel.friendsList.collectAsState()
    val chatThreads by viewModel.chatThreads.collectAsState()
    val messagesMap by viewModel.messagesMap.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val products by viewModel.products.collectAsState()
    val events by viewModel.events.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val savedItems by viewModel.savedItems.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchCategory by viewModel.searchCategory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val supabaseStatus by viewModel.supabaseConnectionStatus.collectAsState()

    // Back handling
    BackHandler(enabled = currentScreen !is Screen.Main && currentScreen !is Screen.Welcome) {
        if (!viewModel.navigateBack()) {
            // Top level back
        }
    }

    FadxTheme(darkTheme = isDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is Screen.Splash -> {
                            SplashScreen(
                                onTimeout = {
                                    if (isAuthenticated) {
                                        viewModel.navigateTo(Screen.Main)
                                    } else {
                                        viewModel.navigateTo(Screen.Welcome)
                                    }
                                }
                            )
                        }
                        is Screen.Welcome -> {
                            WelcomeScreen(
                                onCreateAccountClick = { viewModel.navigateTo(Screen.SignUp) },
                                onLoginClick = { viewModel.navigateTo(Screen.Login) },
                                onQuickDemoAccess = {
                                    viewModel.login("alex.vance@fadx.social", "password")
                                }
                            )
                        }
                        is Screen.Login -> {
                            LoginScreen(
                                onLoginSubmit = { id, pass -> viewModel.login(id, pass) },
                                onForgotPasswordClick = { viewModel.navigateTo(Screen.ForgotPassword) },
                                onSignUpClick = { viewModel.navigateTo(Screen.SignUp) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.SignUp -> {
                            SignUpScreen(
                                onSignUpSubmit = { name, uName, email, phone, pass, dob, gender ->
                                    viewModel.signUp(name, uName, email, phone, pass, dob, gender)
                                },
                                onLoginClick = { viewModel.navigateTo(Screen.Login) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.ForgotPassword -> {
                            ForgotPasswordScreen(
                                onRecoveryComplete = {
                                    viewModel.showToast("Password reset successfully. Please log in.")
                                    viewModel.navigateTo(Screen.Login)
                                },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Main -> {
                            Scaffold(
                                topBar = {
                                    if (selectedTab != 1) { // Hide standard topbar in vertical shorts for immersive view
                                        FadxTopBar(
                                            onSearchClick = { viewModel.navigateTo(Screen.Search) },
                                            onMessagesClick = { viewModel.navigateTo(Screen.Messages) },
                                            unreadMessagesCount = chatThreads.sumOf { it.unreadCount }
                                        )
                                    }
                                },
                                bottomBar = {
                                    FadxBottomNav(
                                        selectedTab = selectedTab,
                                        onTabSelected = { viewModel.selectTab(it) },
                                        notificationsCount = notifications.count { !it.isRead }
                                    )
                                }
                            ) { padding ->
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        currentUser = currentUser,
                                        stories = stories,
                                        posts = posts,
                                        onAddStoryClick = {
                                            viewModel.addStory("Loving the vibe on Fadx today! ⚡️")
                                        },
                                        onStoryClick = { viewModel.openStory(it) },
                                        onCreatePostClick = { viewModel.navigateTo(Screen.CreatePost) },
                                        onReactionSelect = { post, reaction -> viewModel.toggleReaction(post.id, reaction) },
                                        onCommentClick = { viewModel.openComments(it) },
                                        onShareClick = { activeSharePost = it },
                                        onSaveClick = { viewModel.toggleSavePost(it) },
                                        onReportClick = { viewModel.openReportDialog("post", it.id, it.text.take(40)) },
                                        onDeleteClick = { viewModel.deletePost(it.id) },
                                        onAuthorClick = { viewModel.navigateTo(Screen.UserProfile(it.id)) },
                                        modifier = Modifier.padding(padding)
                                    )
                                    1 -> VideosScreen(
                                        videos = videos,
                                        videoTab = videoTab,
                                        onVideoTabChange = { viewModel.setVideoTab(it) },
                                        selectedCategory = videoCategory,
                                        onCategoryChange = { viewModel.setVideoFeedCategory(it) },
                                        onToggleLike = { viewModel.toggleVideoLike(it.id) },
                                        onAuthorClick = { viewModel.navigateTo(Screen.UserProfile(it.id)) },
                                        onShareVideo = { viewModel.showToast("Video link copied to clipboard") },
                                        onCommentVideo = { viewModel.showToast("Comments open for video") },
                                        modifier = Modifier.padding(padding)
                                    )
                                    3 -> NotificationsScreen(
                                        notifications = notifications,
                                        onNotificationClick = { viewModel.showToast("Notification selected") },
                                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                        modifier = Modifier.padding(padding)
                                    )
                                    4 -> MenuHubScreen(
                                        currentUser = currentUser,
                                        onProfileClick = { viewModel.navigateTo(Screen.UserProfile(currentUser.id)) },
                                        onFriendsClick = { viewModel.navigateTo(Screen.Friends) },
                                        onGroupsClick = { viewModel.navigateTo(Screen.Groups) },
                                        onPagesClick = { viewModel.navigateTo(Screen.Pages) },
                                        onMarketplaceClick = { viewModel.navigateTo(Screen.Marketplace) },
                                        onEventsClick = { viewModel.navigateTo(Screen.Events) },
                                        onSavedClick = { viewModel.navigateTo(Screen.SavedItems) },
                                        onAdminClick = { viewModel.navigateTo(Screen.AdminPanel) },
                                        onSettingsClick = { viewModel.navigateTo(Screen.Settings) },
                                        onLogoutClick = { viewModel.logout() },
                                        modifier = Modifier.padding(padding)
                                    )
                                }
                            }
                        }
                        is Screen.Search -> {
                            SearchScreen(
                                query = searchQuery,
                                onQueryChange = { viewModel.searchQuery.value = it },
                                selectedCategory = searchCategory,
                                onCategorySelect = { viewModel.searchCategory.value = it },
                                searchResults = searchResults,
                                onUserClick = { viewModel.navigateTo(Screen.UserProfile(it.id)) },
                                onGroupClick = { viewModel.showToast("Opening group ${it.name}") },
                                onPageClick = { viewModel.showToast("Opening page ${it.name}") },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Messages -> {
                            MessagesListScreen(
                                threads = chatThreads,
                                onThreadClick = { viewModel.navigateTo(Screen.ChatConversation(it.id)) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.ChatConversation -> {
                            val thread = chatThreads.find { it.id == screen.chatId }
                            val messages = messagesMap[screen.chatId] ?: emptyList()
                            ChatConversationScreen(
                                chatId = screen.chatId,
                                thread = thread,
                                messages = messages,
                                currentUserId = currentUser.id,
                                onSendMessage = { text, type, media ->
                                    viewModel.sendMessage(screen.chatId, text, type, media)
                                },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.CreatePost -> {
                            CreatePostScreen(
                                currentUser = currentUser,
                                onBackClick = { viewModel.navigateBack() },
                                onPostSubmit = { text, media, vis, loc ->
                                    viewModel.createPost(text, media, vis, loc)
                                }
                            )
                        }
                        is Screen.UserProfile -> {
                            val user = if (screen.userId == currentUser.id) currentUser else (friendsList.find { it.id == screen.userId } ?: currentUser)
                            val userPosts = posts.filter { it.author.id == screen.userId }
                            ProfileScreen(
                                user = user,
                                isCurrentUser = (screen.userId == currentUser.id),
                                userPosts = userPosts,
                                currentUserId = currentUser.id,
                                onEditProfileClick = { viewModel.navigateTo(Screen.EditProfile) },
                                onSettingsClick = { viewModel.navigateTo(Screen.Settings) },
                                onBackClick = { viewModel.navigateBack() },
                                onStartChat = { viewModel.startChatWithUser(user) },
                                onToggleFollow = { viewModel.showToast("Follow status toggled") },
                                onReactionSelect = { post, reaction -> viewModel.toggleReaction(post.id, reaction) },
                                onCommentClick = { viewModel.openComments(it) },
                                onShareClick = { activeSharePost = it },
                                onSaveClick = { viewModel.toggleSavePost(it) },
                                onReportClick = { viewModel.openReportDialog("profile", user.id, user.name) },
                                onDeleteClick = { viewModel.deletePost(it.id) }
                            )
                        }
                        is Screen.EditProfile -> {
                            EditProfileScreen(
                                currentUser = currentUser,
                                onSaveProfile = { name, bio, loc, web, avatar, cover ->
                                    viewModel.updateProfile(name, bio, loc, web, avatar, cover)
                                },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Friends -> {
                            FriendsScreen(
                                requests = friendRequests,
                                friends = friendsList,
                                onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                                onDeclineRequest = { viewModel.declineFriendRequest(it) },
                                onRemoveFriend = { viewModel.removeFriend(it) },
                                onUserClick = { viewModel.navigateTo(Screen.UserProfile(it.id)) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Groups -> {
                            GroupsScreen(
                                groups = groups,
                                onGroupClick = { viewModel.showToast("Opening community: ${it.name}") },
                                onCreateGroupClick = { viewModel.navigateTo(Screen.CreateGroup) },
                                onToggleJoin = { viewModel.toggleJoinGroup(it.id) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.CreateGroup -> {
                            CreateGroupScreen(
                                onGroupCreate = { name, desc, cat, priv ->
                                    viewModel.createGroup(name, desc, cat, priv)
                                },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Pages -> {
                            PagesScreen(
                                pages = pages,
                                onPageClick = { viewModel.showToast("Opening page: ${it.name}") },
                                onCreatePageClick = { viewModel.navigateTo(Screen.CreatePage) },
                                onToggleFollow = { viewModel.toggleFollowPage(it.id) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.CreatePage -> {
                            CreatePageScreen(
                                onPageCreate = { name, handle, cat, bio ->
                                    viewModel.createPage(name, handle, cat, bio)
                                },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Marketplace -> {
                            MarketplaceScreen(
                                products = products,
                                onCreateListingClick = { viewModel.navigateTo(Screen.CreateProduct) },
                                onProductClick = { viewModel.showToast("Viewing: ${it.title} ($${it.price.toInt()})") },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.CreateProduct -> {
                            CreateProductScreen(
                                onProductCreate = { title, price, desc, cat, loc, img ->
                                    viewModel.createProduct(title, price, desc, cat, loc, img)
                                },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Events -> {
                            EventsScreen(
                                events = events,
                                onCreateEventClick = { viewModel.showToast("Event creator opening") },
                                onToggleGoing = { viewModel.toggleEventStatus(it.id, !it.isGoing) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.SavedItems -> {
                            SavedItemsScreen(
                                savedItems = savedItems,
                                onItemClick = { viewModel.showToast("Opening saved item: ${it.title}") },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                currentTheme = themeMode,
                                onThemeChange = { viewModel.setThemeMode(it) },
                                twoFactorEnabled = twoFactorEnabled,
                                onToggle2FA = { viewModel.toggle2FA() },
                                supabaseStatus = supabaseStatus,
                                onTestSupabase = { viewModel.testSupabaseConnection() },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.AdminPanel -> {
                            AdminPanelScreen(
                                reports = reports,
                                onResolveReport = { id, action -> viewModel.resolveReport(id, action) },
                                onDismissReport = { viewModel.dismissReport(it) },
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }
                        else -> {}
                    }
                }

                // Global Modals & Dialogs
                activeStory?.let { story ->
                    StoryViewerDialog(
                        story = story,
                        onDismiss = { viewModel.closeStory() },
                        onSendReply = { reply ->
                            viewModel.showToast("Reply sent to ${story.author.name}")
                            viewModel.closeStory()
                        },
                        onReact = { emoji ->
                            viewModel.showToast("Reacted $emoji to ${story.author.name}'s story")
                        }
                    )
                }

                activeCommentPost?.let { post ->
                    CommentsBottomSheet(
                        post = post,
                        currentUser = currentUser,
                        onDismiss = { viewModel.closeComments() },
                        onAddComment = { text -> viewModel.addComment(post.id, text) }
                    )
                }

                reportingItem?.let { item ->
                    ReportDialog(
                        targetSummary = item.third,
                        onDismiss = { viewModel.closeReportDialog() },
                        onSubmitReport = { reason -> viewModel.submitReport(reason) }
                    )
                }

                activeSharePost?.let { post ->
                    ShareBottomSheet(
                        friends = friendsList,
                        onDismiss = { activeSharePost = null },
                        onSendDirect = { friend ->
                            viewModel.showToast("Post shared with ${friend.name}")
                        },
                        onCopyLink = {
                            viewModel.showToast("Post link copied to clipboard")
                        },
                        onShareExternal = {
                            viewModel.showToast("Opening system share sheet")
                        }
                    )
                }
            }
        }
    }
}
