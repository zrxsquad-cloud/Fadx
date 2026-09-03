package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.UserCacheRepository
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabasePost
import com.example.data.supabase.SupabaseUserProfile
import com.example.data.supabase.SupabaseUserSession
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class FadxRepository(
    private var userCacheRepository: UserCacheRepository? = null
) {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    // Supabase Remote Client Instance
    val supabaseClient: SupabaseClient = SupabaseClient.instance

    /**
     * Initializes Room local cache repository and Supabase persistent storage.
     */
    fun initCache(context: Context) {
        appContext = context.applicationContext
        if (userCacheRepository == null) {
            val database = AppDatabase.getInstance(context)
            userCacheRepository = UserCacheRepository(database.userDao())
            supabaseClient.initialize(context)
            repoScope.launch {
                checkAndRestoreSession()
            }
        }
    }

    /**
     * Checks if a valid persistent session exists in storage and restores user data.
     */
    suspend fun checkAndRestoreSession(): Boolean = withContext(Dispatchers.IO) {
        val session = supabaseClient.auth.currentSessionOrNull()
        if (session != null && !session.userId.isNullOrBlank()) {
            _isAuthenticated.value = true
            Log.i("FadxRepository", "Restoring session for user ${session.userId}")

            // 1. Immediately read from local Room database cache
            val cached = userCacheRepository?.getUserDirect(session.userId)
            if (cached != null) {
                _currentUser.value = cached.toAppUser()
                Log.i("FadxRepository", "Loaded user from local Room cache: ${cached.fullName}")
            }

            // 2. Sync fresh profile from Supabase public.profiles table
            syncUserProfile(session.userId)
            fetchRemotePosts()
            return@withContext true
        } else {
            _isAuthenticated.value = false
            return@withContext false
        }
    }

    /**
     * Synchronizes Supabase profile data into the local Room database.
     */
    suspend fun syncUserProfile(userId: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.fetchProfile(userId)
            if (result.isSuccess) {
                val remoteProfile = result.getOrThrow()
                val user = remoteProfile.toAppUser()
                _currentUser.value = user
                userCacheRepository?.cacheSupabaseProfile(remoteProfile)
                Log.i("FadxRepository", "Synced profile from Supabase: ${remoteProfile.username}")
                Result.success(user)
            } else {
                val ex = result.exceptionOrNull()
                Log.w("FadxRepository", "Could not fetch remote profile: ${ex?.message}")
                Result.failure(ex ?: Exception("Unknown error syncing profile"))
            }
        } catch (e: Exception) {
            Log.e("FadxRepository", "Exception in syncUserProfile", e)
            Result.failure(e)
        }
    }

    /**
     * Public profile search querying Supabase public.profiles and local Room cache.
     * Complies with RLS rules.
     */
    suspend fun searchPublicProfiles(query: String): List<User> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = query.trim().lowercase()

        val localMatches = (sampleUsers + _currentUser.value).filter {
            it.name.lowercase().contains(q) || it.username.lowercase().contains(q)
        }

        val remoteProfiles = supabaseClient.searchProfiles(q).getOrDefault(emptyList())
        val remoteUsers = remoteProfiles.map { it.toAppUser() }

        (remoteUsers + localMatches).distinctBy { it.id }
    }


    // Current Authenticated User
    private val _currentUser = MutableStateFlow(
        User(
            id = "user_me",
            name = "Alex Vance",
            username = "alexvance",
            email = "alex.vance@fadx.social",
            phone = "+1 (555) 438-9201",
            bio = "Product designer & Creative technologist. Building the future of vibrant social connections on Fadx. 🚀✨",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80",
            location = "San Francisco, CA",
            website = "https://alexvance.design",
            followersCount = 4280,
            followingCount = 612,
            friendsCount = 524,
            isVerified = true,
            isOnline = true,
            dob = "1997-08-22",
            gender = "Male"
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    // Authentication State
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // App Theme Preference: "SYSTEM", "LIGHT", "DARK"
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Sample Users
    val sampleUsers = listOf(
        User(
            id = "u1",
            name = "Elena Rostova",
            username = "elena_art",
            email = "elena@fadx.social",
            bio = "Digital artist & 3D animator. Exploring cyberpunk neon aesthetics. 🎨💜",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1200&q=80",
            followersCount = 18400,
            followingCount = 420,
            friendsCount = 310,
            isVerified = true,
            isFriend = true
        ),
        User(
            id = "u2",
            name = "Marcus Chen",
            username = "marcus_tech",
            email = "marcus@fadx.social",
            bio = "Software architect, AI enthusiast, marathon runner. 🏃‍♂️💻",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=1200&q=80",
            followersCount = 8900,
            followingCount = 310,
            friendsCount = 480,
            isVerified = true,
            isFriend = true
        ),
        User(
            id = "u3",
            name = "Sophia Martinez",
            username = "sophiatravels",
            email = "sophia@fadx.social",
            bio = "Globetrotter (42 countries & counting) | Travel photographer 📸✈️",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=1200&q=80",
            followersCount = 34500,
            followingCount = 512,
            friendsCount = 620,
            isVerified = true,
            isFriend = false,
            hasPendingRequest = true
        ),
        User(
            id = "u4",
            name = "David Kim",
            username = "david_music",
            email = "david@fadx.social",
            bio = "Synthesizer producer & Sound designer. Making beats in Tokyo. 🎧🎹",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1200&q=80",
            followersCount = 12600,
            followingCount = 290,
            friendsCount = 240,
            isVerified = false,
            isFriend = false
        ),
        User(
            id = "u5",
            name = "Amara Okafor",
            username = "amara_style",
            email = "amara@fadx.social",
            bio = "Sustainable fashion designer & Creative Director at Luma 🌿👗",
            avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1200&q=80",
            followersCount = 22100,
            followingCount = 380,
            friendsCount = 490,
            isVerified = true,
            isFriend = true
        )
    )

    // Stories State
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    // Posts State
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // Videos State
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    // Friend Requests & Friends
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _friendsList = MutableStateFlow<List<User>>(emptyList())
    val friendsList: StateFlow<List<User>> = _friendsList.asStateFlow()

    // Messages / Chat Threads
    private val _chatThreads = MutableStateFlow<List<ChatThread>>(emptyList())
    val chatThreads: StateFlow<List<ChatThread>> = _chatThreads.asStateFlow()

    private val _messagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messagesMap: StateFlow<Map<String, List<ChatMessage>>> = _messagesMap.asStateFlow()

    // Groups
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    // Pages
    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages.asStateFlow()

    // Marketplace Products
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    // Events
    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events: StateFlow<List<EventItem>> = _events.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Saved Items
    private val _savedItems = MutableStateFlow<List<SavedItem>>(emptyList())
    val savedItems: StateFlow<List<SavedItem>> = _savedItems.asStateFlow()

    // Reports (Admin panel)
    private val _reports = MutableStateFlow<List<ReportItem>>(emptyList())
    val reports: StateFlow<List<ReportItem>> = _reports.asStateFlow()

    // Privacy & Notification Settings
    private val _privacySettings = MutableStateFlow(
        mapOf(
            "post_visibility" to "Public",
            "friend_requests" to "Everyone",
            "messages" to "Everyone",
            "friend_list" to "Public",
            "profile_visibility" to "Public"
        )
    )
    val privacySettings: StateFlow<Map<String, String>> = _privacySettings.asStateFlow()

    private val _notificationSettings = MutableStateFlow(
        mapOf(
            "push_enabled" to true,
            "likes" to true,
            "comments" to true,
            "messages" to true,
            "friend_requests" to true,
            "group_activity" to true
        )
    )
    val notificationSettings: StateFlow<Map<String, Boolean>> = _notificationSettings.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    // Blocked users (Google Play UGC & user safety compliance)
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    init {
        initSampleData()
    }

    private fun initSampleData() {
        val me = _currentUser.value

        // Initial Stories
        _stories.value = listOf(
            Story(
                id = "story_me",
                author = me,
                mediaUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                text = "Sunsets at the bay! 🌅✨",
                type = StoryType.IMAGE,
                timestamp = "Just now",
                isViewedByMe = true
            ),
            Story(
                id = "story_1",
                author = sampleUsers[0],
                mediaUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800&q=80",
                text = "New 3D render drop on Fadx! Feedback appreciated 💜",
                type = StoryType.IMAGE,
                timestamp = "35m ago",
                isViewedByMe = false
            ),
            Story(
                id = "story_2",
                author = sampleUsers[1],
                text = "Debugging distributed nodes at 2 AM with a fresh espresso ☕⚡",
                type = StoryType.TEXT,
                backgroundGradient = listOf(0xFF0984E3, 0xFF6C5CE7),
                timestamp = "2h ago",
                isViewedByMe = false
            ),
            Story(
                id = "story_3",
                author = sampleUsers[2],
                mediaUrl = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&q=80",
                text = "Touching down in Kyoto! Temple bells and cherry blossoms 🌸",
                type = StoryType.IMAGE,
                timestamp = "4h ago",
                isViewedByMe = false
            ),
            Story(
                id = "story_4",
                author = sampleUsers[4],
                mediaUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=800&q=80",
                text = "Spring collection fittings are officially underway! 🌿🧵",
                type = StoryType.IMAGE,
                timestamp = "6h ago",
                isViewedByMe = false
            )
        )

        // Initial Posts
        _posts.value = listOf(
            Post(
                id = "post_1",
                author = sampleUsers[0],
                timestamp = "2 hours ago",
                text = "Just unveiled my new interactive cyber-art series 'Neon Odyssey' built entirely with modern WebGPU and shader effects. What color palette hits you strongest? 🌌🚀",
                mediaUrls = listOf(
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1000&q=80",
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1000&q=80",
                    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&q=80"
                ),
                mediaType = MediaType.CAROUSEL,
                reactionsCount = mapOf(
                    ReactionType.LIKE to 240,
                    ReactionType.LOVE to 184,
                    ReactionType.WOW to 96
                ),
                userReaction = ReactionType.LOVE,
                commentsCount = 42,
                sharesCount = 18,
                locationTag = "San Francisco Digital Arts Center",
                comments = listOf(
                    Comment(
                        id = "c1",
                        postId = "post_1",
                        author = sampleUsers[1],
                        text = "The lighting physics in slide 2 is unreal Elena! 🔥",
                        timestamp = "1h ago",
                        likesCount = 12,
                        isLiked = true
                    ),
                    Comment(
                        id = "c2",
                        postId = "post_1",
                        author = me,
                        text = "Electric violet vibes represent Fadx perfectly! Outstanding work.",
                        timestamp = "45m ago",
                        likesCount = 6,
                        isLiked = false
                    )
                )
            ),
            Post(
                id = "post_2",
                author = sampleUsers[2],
                timestamp = "4 hours ago",
                text = "Sunrise over the mist-covered peaks of Mount Fuji. One of those breathtaking mornings that reminds you how vast and beautiful our world truly is. 🏔️✨",
                mediaUrls = listOf("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1000&q=80"),
                mediaType = MediaType.IMAGE,
                reactionsCount = mapOf(
                    ReactionType.LIKE to 532,
                    ReactionType.LOVE to 340,
                    ReactionType.WOW to 120
                ),
                commentsCount = 68,
                sharesCount = 45,
                locationTag = "Fujiyoshida, Japan",
                comments = listOf(
                    Comment(
                        id = "c3",
                        postId = "post_2",
                        author = sampleUsers[4],
                        text = "The framing is absolutely magical Sophia! 🌸",
                        timestamp = "3h ago",
                        likesCount = 18
                    )
                )
            ),
            Post(
                id = "post_3",
                author = sampleUsers[1],
                timestamp = "6 hours ago",
                text = "Key takeaway from testing high-concurrency microservices: Keep state machines simple, log structural telemetry with zero allocation, and trust reactive streams! Who else is building distributed backends this week? ⚙️",
                mediaType = MediaType.NONE,
                reactionsCount = mapOf(
                    ReactionType.LIKE to 180,
                    ReactionType.HAHA to 14,
                    ReactionType.WOW to 32
                ),
                commentsCount = 29,
                sharesCount = 12,
                comments = emptyList()
            ),
            Post(
                id = "post_4",
                author = sampleUsers[4],
                timestamp = "9 hours ago",
                text = "Our zero-waste studio workshop recap! We reused 400kg of recycled organic cotton for this season's jackets. Sustainable design is no longer the future — it's the right now. 🌱👗",
                mediaUrls = listOf("https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=1000&q=80"),
                mediaType = MediaType.IMAGE,
                reactionsCount = mapOf(
                    ReactionType.LIKE to 410,
                    ReactionType.LOVE to 295
                ),
                commentsCount = 37,
                sharesCount = 28
            )
        )

        // Initial Videos
        _videos.value = listOf(
            VideoItem(
                id = "v1",
                author = sampleUsers[0],
                title = "Behind the Scenes: 3D Holographic Rendering",
                description = "Quick walkthrough of how I create neon chromatic dispersion in Blender 🎨 #3d #render #fadx",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80",
                duration = "0:45",
                likesCount = 8400,
                commentsCount = 420,
                sharesCount = 180,
                isShort = true,
                category = "Trending"
            ),
            VideoItem(
                id = "v2",
                author = sampleUsers[3],
                title = "Live Analog Synth Jam in Tokyo Studio",
                description = "Modular synth waves creating futuristic ambient soundscapes 🎹🎧 #music #synth #ambient",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
                duration = "1:15",
                likesCount = 12200,
                commentsCount = 890,
                sharesCount = 430,
                isShort = true,
                category = "Recommended"
            ),
            VideoItem(
                id = "v3",
                author = sampleUsers[2],
                title = "Exploring Hidden Alleyways of Shibuya",
                description = "Late night street food discoveries & neon photography spots 🍜🇯🇵 #travel #japan #vlog",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800&q=80",
                duration = "3:40",
                likesCount = 24500,
                commentsCount = 1320,
                sharesCount = 980,
                isShort = false,
                category = "Following"
            )
        )

        // Friend Requests & Friends
        _friendRequests.value = listOf(
            FriendRequest(
                id = "fr_1",
                user = sampleUsers[2],
                mutualFriendsCount = 14,
                timestamp = "3h ago"
            ),
            FriendRequest(
                id = "fr_2",
                user = sampleUsers[3],
                mutualFriendsCount = 6,
                timestamp = "1d ago"
            )
        )

        _friendsList.value = listOf(sampleUsers[0], sampleUsers[1], sampleUsers[4])

        // Initial Chat Threads
        _chatThreads.value = listOf(
            ChatThread(
                id = "chat_elena",
                name = "Elena Rostova",
                avatarUrl = sampleUsers[0].avatarUrl,
                isGroup = false,
                participants = listOf(sampleUsers[0]),
                lastMessage = "Let me know what you think of the new palette!",
                lastMessageTime = "10:42 AM",
                unreadCount = 2,
                isOnline = true
            ),
            ChatThread(
                id = "chat_marcus",
                name = "Marcus Chen",
                avatarUrl = sampleUsers[1].avatarUrl,
                isGroup = false,
                participants = listOf(sampleUsers[1]),
                lastMessage = "Sent you the repository link for the benchmarks.",
                lastMessageTime = "Yesterday",
                unreadCount = 0,
                isOnline = false
            ),
            ChatThread(
                id = "chat_fadx_creators",
                name = "Fadx Creators Hub 🚀",
                avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=500&q=80",
                isGroup = true,
                participants = listOf(sampleUsers[0], sampleUsers[1], sampleUsers[4]),
                lastMessage = "Amara: New design drop this Friday at 5 PM EST!",
                lastMessageTime = "2d ago",
                unreadCount = 0,
                isOnline = true
            )
        )

        _messagesMap.value = mapOf(
            "chat_elena" to listOf(
                ChatMessage(
                    id = "m1",
                    senderId = sampleUsers[0].id,
                    senderName = "Elena Rostova",
                    senderAvatar = sampleUsers[0].avatarUrl,
                    text = "Hey Alex! Loved your feedback on my artwork post earlier ✨",
                    timestamp = "10:38 AM"
                ),
                ChatMessage(
                    id = "m2",
                    senderId = "user_me",
                    senderName = "Alex Vance",
                    senderAvatar = me.avatarUrl,
                    text = "Anytime! The depth in the cyberpunk shading is top-notch.",
                    timestamp = "10:40 AM"
                ),
                ChatMessage(
                    id = "m3",
                    senderId = sampleUsers[0].id,
                    senderName = "Elena Rostova",
                    senderAvatar = sampleUsers[0].avatarUrl,
                    text = "Let me know what you think of the new palette!",
                    timestamp = "10:42 AM",
                    isRead = false
                )
            ),
            "chat_marcus" to listOf(
                ChatMessage(
                    id = "m4",
                    senderId = sampleUsers[1].id,
                    senderName = "Marcus Chen",
                    senderAvatar = sampleUsers[1].avatarUrl,
                    text = "Sent you the repository link for the benchmarks.",
                    timestamp = "Yesterday"
                )
            ),
            "chat_fadx_creators" to listOf(
                ChatMessage(
                    id = "m5",
                    senderId = sampleUsers[4].id,
                    senderName = "Amara Okafor",
                    senderAvatar = sampleUsers[4].avatarUrl,
                    text = "New design drop this Friday at 5 PM EST!",
                    timestamp = "2d ago"
                )
            )
        )

        // Initial Groups
        _groups.value = listOf(
            Group(
                id = "grp_1",
                name = "Modern Android & Jetpack Compose Artisans",
                description = "A vibrant community of Android developers crafting cutting-edge Compose UIs and clean architecture.",
                coverUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=1000&q=80",
                avatarUrl = "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?w=500&q=80",
                membersCount = 14200,
                isPrivate = false,
                isJoined = true,
                category = "Technology",
                rules = listOf("Be respectful and inclusive", "No promotional spam", "Share code snippets cleanly")
            ),
            Group(
                id = "grp_2",
                name = "Global Travel & Nomads Network",
                description = "Tips, destination guides, and meetups for remote wanderers exploring every continent.",
                coverUrl = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=1000&q=80",
                avatarUrl = "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=500&q=80",
                membersCount = 28900,
                isPrivate = false,
                isJoined = false,
                category = "Community"
            ),
            Group(
                id = "grp_3",
                name = "Digital Art & Generative Aesthetics",
                description = "Showcasing 3D renders, shaders, generative design, and creative coding.",
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&q=80",
                avatarUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500&q=80",
                membersCount = 9800,
                isPrivate = true,
                isJoined = true,
                isAdmin = true,
                category = "Creator"
            )
        )

        // Initial Pages
        _pages.value = listOf(
            Page(
                id = "pg_1",
                name = "TechPulse Media",
                handle = "@techpulse",
                category = PageCategory.TECHNOLOGY,
                bio = "Breaking tech developments, AI breakthroughs, and hardware reviews daily.",
                coverUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=1000&q=80",
                avatarUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&q=80",
                followersCount = 142000,
                isLiked = true,
                isFollowed = true
            ),
            Page(
                id = "pg_2",
                name = "Vogue Nova Style",
                handle = "@voguenova",
                category = PageCategory.CREATOR,
                bio = "High fashion, street culture, and sustainable runway trends worldwide.",
                coverUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1000&q=80",
                avatarUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=500&q=80",
                followersCount = 89400,
                isLiked = false,
                isFollowed = false
            ),
            Page(
                id = "pg_3",
                name = "SoundWave Collective",
                handle = "@soundwave",
                category = PageCategory.ENTERTAINMENT,
                bio = "Curating progressive electronic and synthwave tracks from indie producers.",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1000&q=80",
                avatarUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
                followersCount = 56100,
                isLiked = true,
                isFollowed = true
            )
        )

        // Initial Marketplace Products
        _products.value = listOf(
            Product(
                id = "prod_1",
                title = "Sony WH-1000XM5 Wireless Headphones (Midnight Edition)",
                price = 289.00,
                description = "Like new condition, used for 2 weeks in smoke-free home studio. Comes with original case, braided aux cord, and USB-C charger.",
                category = ProductCategory.ELECTRONICS,
                location = "San Francisco, CA",
                images = listOf("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80"),
                seller = sampleUsers[1],
                isSaved = true
            ),
            Product(
                id = "prod_2",
                title = "Custom Mechanical Keyboard (Lubed Gateron Oil Kings)",
                price = 195.00,
                description = "Aluminum CNC case, hot-swappable PCB, PBT dye-sub keycaps with custom RGB underglow. Smooth linear switches.",
                category = ProductCategory.ELECTRONICS,
                location = "Oakland, CA",
                images = listOf("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80"),
                seller = sampleUsers[0],
                isSaved = false
            ),
            Product(
                id = "prod_3",
                title = "Fuji X-T4 Mirrorless Camera Body + 18-55mm Lens",
                price = 1150.00,
                description = "Pristine condition with under 4,000 shutter count. Incredible film simulations for street and travel photography.",
                category = ProductCategory.ELECTRONICS,
                location = "San Jose, CA",
                images = listOf("https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80"),
                seller = sampleUsers[2],
                isSaved = false
            ),
            Product(
                id = "prod_4",
                title = "Minimalist Walnut Ergonomic Standing Desk",
                price = 450.00,
                description = "Solid American walnut hardwood with dual motorized height adjustment, memory presets, and cable management tray.",
                category = ProductCategory.HOME,
                location = "San Francisco, CA",
                images = listOf("https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800&q=80"),
                seller = sampleUsers[4],
                isSaved = false
            )
        )

        // Initial Events
        _events.value = listOf(
            EventItem(
                id = "evt_1",
                title = "Fadx Social Tech & Creator Summit 2026",
                date = "Oct 18, 2026",
                time = "10:00 AM - 6:00 PM",
                location = "Moscone Center, San Francisco",
                description = "Join top designers, creators, and mobile architects exploring next-gen social network ecosystems.",
                coverUrl = "https://images.unsplash.com/photo-1511578314322-379afb476865?w=1000&q=80",
                organizer = sampleUsers[1],
                goingCount = 840,
                interestedCount = 1420,
                isGoing = true
            ),
            EventItem(
                id = "evt_2",
                title = "Tokyo Underground Synth Night",
                date = "Nov 04, 2026",
                time = "9:00 PM - 3:00 AM",
                location = "Shibuya Sound Lab, Tokyo",
                description = "Immersive audio-visual performance with analog modular synths and projection mapping.",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1000&q=80",
                organizer = sampleUsers[3],
                goingCount = 310,
                interestedCount = 620,
                isInterested = true
            )
        )

        // Initial Notifications
        _notifications.value = listOf(
            NotificationItem(
                id = "notif_1",
                type = NotificationType.REACTION,
                actor = sampleUsers[0],
                text = "loved your comment on 'Neon Odyssey'",
                timestamp = "15m ago",
                isRead = false,
                targetId = "post_1"
            ),
            NotificationItem(
                id = "notif_2",
                type = NotificationType.FRIEND_REQUEST,
                actor = sampleUsers[2],
                text = "sent you a friend request",
                timestamp = "3h ago",
                isRead = false
            ),
            NotificationItem(
                id = "notif_3",
                type = NotificationType.COMMENT,
                actor = sampleUsers[1],
                text = "commented: 'Great perspective on reactive architecture!'",
                timestamp = "6h ago",
                isRead = true,
                targetId = "post_3"
            ),
            NotificationItem(
                id = "notif_4",
                type = NotificationType.GROUP_ACTIVITY,
                actor = sampleUsers[4],
                text = "posted a new update in 'Modern Android & Jetpack Compose Artisans'",
                timestamp = "1d ago",
                isRead = true,
                targetId = "grp_1"
            )
        )

        // Initial Saved Items
        _savedItems.value = listOf(
            SavedItem(
                id = "save_1",
                type = "Post",
                title = "Neon Odyssey Cyber-Art Series",
                subtitle = "By Elena Rostova",
                imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&q=80",
                collectionName = "Inspiration",
                originalId = "post_1"
            ),
            SavedItem(
                id = "save_2",
                type = "Product",
                title = "Sony WH-1000XM5 Wireless Headphones",
                subtitle = "$289.00 - Electronics",
                imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&q=80",
                collectionName = "Wishlist",
                originalId = "prod_1"
            )
        )

        // Initial Reports (for Admin dashboard)
        _reports.value = listOf(
            ReportItem(
                id = "rep_1",
                targetType = "Post",
                targetId = "post_spam_test",
                targetSummary = "Unsolicited promotional links for crypto tokens",
                reporterName = "Marcus Chen",
                reason = ReportReason.SPAM,
                timestamp = "2h ago",
                status = "Pending"
            ),
            ReportItem(
                id = "rep_2",
                targetType = "User",
                targetId = "user_fake_01",
                targetSummary = "Impersonation account copying profile details",
                reporterName = "Elena Rostova",
                reason = ReportReason.FAKE_ACCOUNT,
                timestamp = "5h ago",
                status = "Pending"
            )
        )
    }

    // --- Actions & Mutators ---

    suspend fun loginWithSupabase(emailOrUsername: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val result = supabaseClient.signInWithPassword(emailOrUsername, pass)
        if (result.isSuccess) {
            val session = result.getOrThrow()
            _isAuthenticated.value = true
            val userId = session.userId ?: "user_me"

            // Attempt to fetch profile from public.profiles table
            val profileResult = supabaseClient.fetchProfile(userId)
            val finalUser = if (profileResult.isSuccess) {
                val p = profileResult.getOrThrow()
                userCacheRepository?.cacheSupabaseProfile(p)
                p.toAppUser()
            } else {
                // If profile row doesn't exist yet, construct and upsert it
                val newProfile = SupabaseUserProfile(
                    id = userId,
                    email = session.email ?: emailOrUsername,
                    username = emailOrUsername.substringBefore("@").filter { it.isLetterOrDigit() }.lowercase().ifBlank { "user_${userId.take(6)}" },
                    fullName = emailOrUsername.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }.ifBlank { "Fadx Explorer" },
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
                    coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80",
                    createdAt = System.currentTimeMillis().toString()
                )
                supabaseClient.upsertProfile(newProfile)
                userCacheRepository?.cacheSupabaseProfile(newProfile)
                newProfile.toAppUser()
            }
            _currentUser.value = finalUser
            Result.success(finalUser)
        } else {
            // Local fallback / demo login if offline or demo accounts
            if (emailOrUsername.contains("alex.vance", ignoreCase = true) || !emailOrUsername.contains("@")) {
                _isAuthenticated.value = true
                Result.success(_currentUser.value)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Login failed"))
            }
        }
    }

    fun login(emailOrUsername: String, pass: String): Boolean {
        _isAuthenticated.value = true
        repoScope.launch {
            loginWithSupabase(emailOrUsername, pass)
        }
        return true
    }

    suspend fun signUpWithSupabase(
        name: String,
        username: String,
        email: String,
        phone: String,
        pass: String,
        dob: String,
        gender: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val metadata = mapOf(
            "full_name" to name,
            "username" to username,
            "phone" to phone
        )
        supabaseClient.signUp(email, pass, metadata)
        val session = supabaseClient.auth.currentSessionOrNull()
        val userId = session?.userId ?: "user_${UUID.randomUUID().toString().take(8)}"

        val profile = SupabaseUserProfile(
            id = userId,
            email = email,
            username = username,
            fullName = name,
            phone = phone,
            dob = dob,
            gender = gender,
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80",
            createdAt = System.currentTimeMillis().toString()
        )

        // Upsert to Supabase profiles and cache to local Room DB
        supabaseClient.upsertProfile(profile)
        userCacheRepository?.cacheSupabaseProfile(profile)

        val appUser = profile.toAppUser()
        _currentUser.value = appUser
        _isAuthenticated.value = true
        Result.success(appUser)
    }

    fun signUp(name: String, username: String, email: String, phone: String, pass: String, dob: String, gender: String) {
        _currentUser.value = _currentUser.value.copy(
            name = name.ifBlank { "Alex Vance" },
            username = username.ifBlank { "alexvance" },
            email = email.ifBlank { "alex@fadx.social" },
            phone = phone.ifBlank { "+1 555 019 2834" },
            dob = dob,
            gender = gender
        )
        _isAuthenticated.value = true
        repoScope.launch {
            signUpWithSupabase(name, username, email, phone, pass, dob, gender)
        }
    }

    fun logout() {
        _isAuthenticated.value = false
        supabaseClient.signOut()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    // Post Interactions
    fun toggleReaction(postId: String, reaction: ReactionType) {
        var userReacted = false
        var targetReaction = ReactionType.NONE
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val currentReaction = post.userReaction
                val newReaction = if (currentReaction == reaction) ReactionType.NONE else reaction
                targetReaction = newReaction
                userReacted = (newReaction != ReactionType.NONE)
                val newCounts = post.reactionsCount.toMutableMap()

                if (currentReaction != ReactionType.NONE) {
                    val count = (newCounts[currentReaction] ?: 1) - 1
                    if (count <= 0) newCounts.remove(currentReaction) else newCounts[currentReaction] = count
                }
                if (newReaction != ReactionType.NONE) {
                    newCounts[newReaction] = (newCounts[newReaction] ?: 0) + 1
                }

                post.copy(
                    userReaction = newReaction,
                    reactionsCount = newCounts
                )
            } else post
        }

        // Sync like to Supabase post_likes table
        val me = _currentUser.value.id
        repoScope.launch {
            try {
                if (userReacted) {
                    supabaseClient.addLike(postId, me, targetReaction.name)
                } else {
                    supabaseClient.removeLike(postId, me)
                }
            } catch (e: Exception) {
                Log.w("FadxRepository", "Error syncing reaction to Supabase", e)
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        val newComment = Comment(
            id = "c_${UUID.randomUUID()}",
            postId = postId,
            author = _currentUser.value,
            text = text.trim(),
            timestamp = "Just now",
            likesCount = 0
        )
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(
                    comments = listOf(newComment) + post.comments,
                    commentsCount = post.commentsCount + 1
                )
            } else post
        }

        // Sync comment to Supabase comments table
        val me = _currentUser.value
        repoScope.launch {
            try {
                supabaseClient.addComment(
                    commentId = newComment.id,
                    postId = postId,
                    userId = me.id,
                    userName = me.name,
                    userAvatar = me.avatarUrl,
                    content = newComment.text
                )
            } catch (e: Exception) {
                Log.w("FadxRepository", "Error syncing comment to Supabase", e)
            }
        }
    }

    suspend fun syncCommentsForPost(postId: String) = withContext(Dispatchers.IO) {
        val result = supabaseClient.fetchComments(postId)
        if (result.isSuccess) {
            val jsonList = result.getOrThrow()
            if (jsonList.isNotEmpty()) {
                val cloudComments = jsonList.map { obj ->
                    Comment(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        postId = obj.optString("post_id", postId),
                        author = User(
                            id = obj.optString("user_id"),
                            name = obj.optString("user_name", "Fadx User"),
                            username = obj.optString("user_name", "user").filter { it.isLetterOrDigit() }.lowercase(),
                            email = "user@fadx.social",
                            avatarUrl = obj.optString("user_avatar", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500&q=80")
                        ),
                        text = obj.optString("content"),
                        timestamp = "Recently",
                        likesCount = 0
                    )
                }
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        val existingIds = cloudComments.map { it.id }.toSet()
                        val merged = (cloudComments + post.comments.filter { it.id !in existingIds })
                        post.copy(
                            comments = merged,
                            commentsCount = maxOf(post.commentsCount, merged.size)
                        )
                    } else post
                }
            }
        }
    }

    fun createPost(
        text: String,
        mediaUrls: List<String> = emptyList(),
        mediaType: MediaType = MediaType.NONE,
        visibility: PostVisibility = PostVisibility.PUBLIC,
        location: String? = null
    ) {
        val newPost = Post(
            id = "post_${UUID.randomUUID()}",
            author = _currentUser.value,
            timestamp = "Just now",
            text = text,
            mediaUrls = mediaUrls,
            mediaType = if (mediaUrls.size > 1) MediaType.CAROUSEL else if (mediaUrls.size == 1) MediaType.IMAGE else mediaType,
            visibility = visibility,
            locationTag = location,
            reactionsCount = emptyMap(),
            commentsCount = 0,
            sharesCount = 0
        )
        _posts.value = listOf(newPost) + _posts.value

        // Upload local images to Supabase Storage 'posts' bucket and sync to posts table
        repoScope.launch {
            try {
                val uploadedMediaUrls = mediaUrls.map { url ->
                    if (url.startsWith("content://") || url.startsWith("file://")) {
                        appContext?.let { ctx ->
                            val uploadRes = supabaseClient.uploadMediaUri(ctx, "posts", url)
                            uploadRes.getOrNull() ?: url
                        } ?: url
                    } else {
                        url
                    }
                }

                val finalPost = if (uploadedMediaUrls != mediaUrls) {
                    val updated = newPost.copy(mediaUrls = uploadedMediaUrls)
                    _posts.value = _posts.value.map { if (it.id == newPost.id) updated else it }
                    updated
                } else {
                    newPost
                }

                val supabasePost = SupabasePost.fromAppPost(finalPost)
                val result = supabaseClient.createPost(supabasePost)
                if (result.isSuccess) {
                    Log.i("FadxRepository", "Post synced to Supabase: ${finalPost.id}")
                } else {
                    Log.w("FadxRepository", "Could not sync post to Supabase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w("FadxRepository", "Exception syncing post to Supabase", e)
            }
        }
    }

    fun deletePost(postId: String) {
        _posts.value = _posts.value.filter { it.id != postId }
        repoScope.launch {
            try {
                supabaseClient.deletePost(postId)
            } catch (e: Exception) {
                Log.w("FadxRepository", "Exception deleting post from Supabase", e)
            }
        }
    }

    fun toggleSavePost(post: Post) {
        val currentlySaved = post.isSaved
        _posts.value = _posts.value.map {
            if (it.id == post.id) it.copy(isSaved = !currentlySaved) else it
        }
        if (!currentlySaved) {
            val save = SavedItem(
                id = "save_${UUID.randomUUID()}",
                type = "Post",
                title = if (post.text.length > 40) post.text.take(40) + "..." else post.text,
                subtitle = "By ${post.author.name}",
                imageUrl = post.mediaUrls.firstOrNull() ?: post.author.avatarUrl,
                collectionName = "All Saved",
                originalId = post.id
            )
            _savedItems.value = listOf(save) + _savedItems.value
        } else {
            _savedItems.value = _savedItems.value.filter { it.originalId != post.id }
        }
    }

    // Stories
    fun addStory(text: String, mediaUrl: String = "", type: StoryType = StoryType.TEXT) {
        val newStory = Story(
            id = "story_${UUID.randomUUID()}",
            author = _currentUser.value,
            mediaUrl = mediaUrl,
            text = text,
            type = type,
            timestamp = "Just now",
            isViewedByMe = true
        )
        _stories.value = listOf(newStory) + _stories.value
    }

    fun markStoryViewed(storyId: String) {
        _stories.value = _stories.value.map {
            if (it.id == storyId) it.copy(isViewedByMe = true, viewCount = it.viewCount + 1) else it
        }
    }

    // Video Interactions
    fun toggleVideoLike(videoId: String) {
        _videos.value = _videos.value.map {
            if (it.id == videoId) {
                val liked = !it.isLiked
                it.copy(
                    isLiked = liked,
                    likesCount = if (liked) it.likesCount + 1 else it.likesCount - 1
                )
            } else it
        }
    }

    // Friends Management
    fun acceptFriendRequest(requestId: String) {
        val req = _friendRequests.value.find { it.id == requestId }
        if (req != null) {
            _friendRequests.value = _friendRequests.value.filter { it.id != requestId }
            _friendsList.value = listOf(req.user.copy(isFriend = true)) + _friendsList.value
            // Increment friend count
            _currentUser.value = _currentUser.value.copy(friendsCount = _currentUser.value.friendsCount + 1)
        }
    }

    fun declineFriendRequest(requestId: String) {
        _friendRequests.value = _friendRequests.value.filter { it.id != requestId }
    }

    fun removeFriend(userId: String) {
        _friendsList.value = _friendsList.value.filter { it.id != userId }
        _currentUser.value = _currentUser.value.copy(friendsCount = maxOf(0, _currentUser.value.friendsCount - 1))
    }

    fun toggleFollowUser(userId: String) {
        // Can toggle follow state on any user
    }

    // Messaging
    fun sendMessage(chatId: String, text: String, type: MessageType = MessageType.TEXT, mediaUrl: String? = null) {
        if (text.isBlank() && mediaUrl == null) return
        val me = _currentUser.value
        val msg = ChatMessage(
            id = "msg_${UUID.randomUUID()}",
            senderId = me.id,
            senderName = me.name,
            senderAvatar = me.avatarUrl,
            text = text,
            mediaUrl = mediaUrl,
            messageType = type,
            timestamp = "Just now"
        )
        val currentMsgs = _messagesMap.value[chatId] ?: emptyList()
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[chatId] = currentMsgs + msg
        _messagesMap.value = updatedMap

        // Update thread preview
        _chatThreads.value = _chatThreads.value.map {
            if (it.id == chatId) {
                it.copy(
                    lastMessage = if (type == MessageType.TEXT) text else "[Media/Voice Note]",
                    lastMessageTime = "Just now"
                )
            } else it
        }
    }

    fun getOrCreateChat(user: User): String {
        val existing = _chatThreads.value.find { !it.isGroup && it.participants.any { p -> p.id == user.id } }
        if (existing != null) return existing.id
        val newChatId = "chat_${user.id}"
        val newThread = ChatThread(
            id = newChatId,
            name = user.name,
            avatarUrl = user.avatarUrl,
            isGroup = false,
            participants = listOf(user),
            lastMessage = "Start a conversation with ${user.name}",
            lastMessageTime = "Now",
            isOnline = user.isOnline
        )
        _chatThreads.value = listOf(newThread) + _chatThreads.value
        _messagesMap.value = _messagesMap.value + (newChatId to emptyList())
        return newChatId
    }

    // Groups
    fun toggleJoinGroup(groupId: String) {
        _groups.value = _groups.value.map {
            if (it.id == groupId) {
                val joined = !it.isJoined
                it.copy(
                    isJoined = joined,
                    membersCount = if (joined) it.membersCount + 1 else it.membersCount - 1
                )
            } else it
        }
    }

    fun createGroup(name: String, description: String, category: String, isPrivate: Boolean) {
        val newGroup = Group(
            id = "grp_${UUID.randomUUID()}",
            name = name,
            description = description,
            coverUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=1000&q=80",
            avatarUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&q=80",
            membersCount = 1,
            isPrivate = isPrivate,
            isJoined = true,
            isAdmin = true,
            category = category,
            rules = listOf("Respect all group members", "No spamming")
        )
        _groups.value = listOf(newGroup) + _groups.value
    }

    // Pages
    fun toggleFollowPage(pageId: String) {
        _pages.value = _pages.value.map {
            if (it.id == pageId) {
                val followed = !it.isFollowed
                it.copy(
                    isFollowed = followed,
                    isLiked = followed,
                    followersCount = if (followed) it.followersCount + 1 else it.followersCount - 1
                )
            } else it
        }
    }

    fun createPage(name: String, handle: String, category: PageCategory, bio: String) {
        val newPage = Page(
            id = "pg_${UUID.randomUUID()}",
            name = name,
            handle = if (handle.startsWith("@")) handle else "@$handle",
            category = category,
            bio = bio,
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&q=80",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
            followersCount = 1,
            isLiked = true,
            isFollowed = true
        )
        _pages.value = listOf(newPage) + _pages.value
    }

    // Marketplace
    fun createProduct(title: String, price: Double, description: String, category: ProductCategory, location: String, imageUrl: String) {
        val newProduct = Product(
            id = "prod_${UUID.randomUUID()}",
            title = title,
            price = price,
            description = description,
            category = category,
            location = location,
            images = listOf(imageUrl.ifBlank { "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80" }),
            seller = _currentUser.value,
            datePosted = "Just now"
        )
        _products.value = listOf(newProduct) + _products.value
    }

    // Events
    fun toggleEventStatus(eventId: String, isGoing: Boolean) {
        _events.value = _events.value.map {
            if (it.id == eventId) {
                if (isGoing) {
                    it.copy(
                        isGoing = !it.isGoing,
                        goingCount = if (!it.isGoing) it.goingCount + 1 else it.goingCount - 1,
                        isInterested = false
                    )
                } else {
                    it.copy(
                        isInterested = !it.isInterested,
                        interestedCount = if (!it.isInterested) it.interestedCount + 1 else it.interestedCount - 1,
                        isGoing = false
                    )
                }
            } else it
        }
    }

    // Notifications
    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map { if (it.id == id) it.copy(isRead = true) else it }
    }

    fun triggerNotification(type: NotificationType, title: String, message: String, actorUser: User? = null) {
        val actor = actorUser ?: _friendsList.value.firstOrNull() ?: _currentUser.value
        val newNotif = NotificationItem(
            id = "notif_${UUID.randomUUID()}",
            type = type,
            actor = actor,
            text = message,
            timestamp = "Just now",
            isRead = false
        )
        _notifications.value = listOf(newNotif) + _notifications.value

        appContext?.let { ctx ->
            com.example.notifications.NotificationHelper.showNotification(
                context = ctx,
                notificationId = (System.currentTimeMillis() % 100000).toInt(),
                title = title,
                message = "${actor.name} $message"
            )
        }
    }

    // Profile updates
    fun updateProfile(name: String, bio: String, location: String, website: String, avatarUrl: String?, coverUrl: String?) {
        val updated = _currentUser.value.copy(
            name = name,
            bio = bio,
            location = location,
            website = website,
            avatarUrl = avatarUrl ?: _currentUser.value.avatarUrl,
            coverUrl = coverUrl ?: _currentUser.value.coverUrl
        )
        _currentUser.value = updated

        repoScope.launch {
            try {
                var finalAvatar = avatarUrl
                var finalCover = coverUrl

                appContext?.let { ctx ->
                    if (finalAvatar != null && (finalAvatar!!.startsWith("content://") || finalAvatar!!.startsWith("file://"))) {
                        finalAvatar = supabaseClient.uploadMediaUri(ctx, "avatars", finalAvatar!!).getOrNull() ?: finalAvatar
                    }
                    if (finalCover != null && (finalCover!!.startsWith("content://") || finalCover!!.startsWith("file://"))) {
                        finalCover = supabaseClient.uploadMediaUri(ctx, "avatars", finalCover!!).getOrNull() ?: finalCover
                    }
                }

                val withCloudAssets = updated.copy(
                    avatarUrl = finalAvatar ?: updated.avatarUrl,
                    coverUrl = finalCover ?: updated.coverUrl
                )
                _currentUser.value = withCloudAssets
                userCacheRepository?.cacheAppUser(withCloudAssets)
                val supabaseProfile = SupabaseUserProfile.fromAppUser(withCloudAssets)
                supabaseClient.upsertProfile(supabaseProfile)
                Log.i("FadxRepository", "Updated and synced profile to Supabase: ${withCloudAssets.id}")
            } catch (e: Exception) {
                Log.e("FadxRepository", "Error syncing profile update", e)
            }
        }
    }

    // Report submission & Admin actions
    fun submitReport(targetType: String, targetId: String, summary: String, reason: ReportReason) {
        val report = ReportItem(
            id = "rep_${UUID.randomUUID()}",
            targetType = targetType,
            targetId = targetId,
            targetSummary = summary,
            reporterName = _currentUser.value.name,
            reason = reason,
            timestamp = "Just now",
            status = "Pending"
        )
        _reports.value = listOf(report) + _reports.value
    }

    fun resolveReport(reportId: String, action: String) {
        _reports.value = _reports.value.map {
            if (it.id == reportId) it.copy(status = "Resolved ($action)") else it
        }
    }

    fun dismissReport(reportId: String) {
        _reports.value = _reports.value.map {
            if (it.id == reportId) it.copy(status = "Dismissed") else it
        }
    }

    fun toggle2FA() {
        _twoFactorEnabled.value = !_twoFactorEnabled.value
    }

    /**
     * Fetch feed posts from Supabase and merge with local posts,
     * filtering out content from blocked users.
     */
    suspend fun fetchRemotePosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.fetchPosts(30)
            if (result.isSuccess) {
                val remotePosts = result.getOrThrow().map { it.toAppPost() }
                if (remotePosts.isNotEmpty()) {
                    val currentIds = remotePosts.map { it.id }.toSet()
                    val blocked = _blockedUserIds.value
                    val merged = (remotePosts + _posts.value.filter { it.id !in currentIds })
                        .filter { it.author.id !in blocked }
                    _posts.value = merged
                }
                Result.success(remotePosts)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Block a user (Google Play UGC requirement).
     * Immediately hides their posts, stories, requests, and chats.
     */
    fun blockUser(userId: String) {
        val updated = _blockedUserIds.value + userId
        _blockedUserIds.value = updated
        _posts.value = _posts.value.filter { it.author.id !in updated }
        _stories.value = _stories.value.filter { it.author.id !in updated }
        _friendRequests.value = _friendRequests.value.filter { it.user.id !in updated }
        _chatThreads.value = _chatThreads.value.filter { thread -> thread.participants.none { it.id in updated } }

        val me = _currentUser.value.id
        repoScope.launch {
            try {
                supabaseClient.blockUser(me, userId)
                Log.i("FadxRepository", "Blocked user $userId in Supabase")
            } catch (e: Exception) {
                Log.w("FadxRepository", "Error syncing blocked user to Supabase", e)
            }
        }
    }

    /**
     * Unblock a previously blocked user.
     */
    fun unblockUser(userId: String) {
        _blockedUserIds.value = _blockedUserIds.value - userId
        val me = _currentUser.value.id
        repoScope.launch {
            try {
                supabaseClient.unblockUser(me, userId)
                Log.i("FadxRepository", "Unblocked user $userId in Supabase")
            } catch (e: Exception) {
                Log.w("FadxRepository", "Error syncing unblock user to Supabase", e)
            }
        }
    }

    /**
     * Delete user account & all associated local data (Google Play Policy requirement).
     */
    fun deleteAccount() {
        val userId = _currentUser.value.id
        repoScope.launch {
            try {
                supabaseClient.deleteProfile(userId)
                userCacheRepository?.clearCache()
                Log.i("FadxRepository", "Account and local cache deleted for $userId")
            } catch (e: Exception) {
                Log.w("FadxRepository", "Error executing delete account", e)
            }
        }
        _isAuthenticated.value = false
        _currentUser.value = sampleUsers.first()
    }

    companion object {
        @Volatile
        private var INSTANCE: FadxRepository? = null

        fun getInstance(context: Context? = null): FadxRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: FadxRepository()
                if (context != null) {
                    instance.initCache(context)
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
