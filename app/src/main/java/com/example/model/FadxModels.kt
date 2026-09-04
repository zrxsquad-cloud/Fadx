package com.example.model

enum class ReactionType(val emoji: String, val label: String) {
    NONE("", "Like"),
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    HAHA("😆", "Haha"),
    WOW("😮", "Wow"),
    SAD("😢", "Sad"),
    ANGRY("😡", "Angry")
}

enum class PostVisibility(val label: String) {
    PUBLIC("Public"),
    FRIENDS("Friends"),
    ONLY_ME("Only Me"),
    CUSTOM("Custom")
}

enum class MediaType {
    NONE, IMAGE, CAROUSEL, VIDEO
}

enum class StoryType {
    IMAGE, VIDEO, TEXT
}

enum class ProductCategory(val label: String) {
    ELECTRONICS("Electronics"),
    FASHION("Fashion"),
    VEHICLES("Vehicles"),
    HOME("Home & Living"),
    JOBS("Jobs"),
    OTHER("Other")
}

enum class PageCategory(val label: String) {
    BUSINESS("Business"),
    ENTERTAINMENT("Entertainment"),
    CREATOR("Creator"),
    COMMUNITY("Community"),
    EDUCATION("Education"),
    TECHNOLOGY("Technology")
}

enum class NotificationType {
    LIKE, REACTION, COMMENT, REPLY, SHARE, FRIEND_REQUEST, FRIEND_ACCEPT, NEW_FOLLOWER, MESSAGE, GROUP_ACTIVITY, MENTION
}

enum class ReportReason(val label: String) {
    SPAM("Spam or misleading"),
    HARASSMENT("Harassment or bullying"),
    VIOLENCE("Violence or dangerous acts"),
    FAKE_ACCOUNT("Fake account or impersonation"),
    INAPPROPRIATE("Inappropriate or explicit content"),
    OTHER("Other violation")
}

data class User(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val phone: String = "+1 555 019 2834",
    val bio: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val location: String = "San Francisco, CA",
    val website: String = "https://fadx.social",
    val followersCount: Int = 1240,
    val followingCount: Int = 380,
    val friendsCount: Int = 412,
    val isVerified: Boolean = false,
    val isOnline: Boolean = true,
    val lastActive: String = "Just now",
    val gender: String = "Not Specified",
    val dob: String = "1998-05-14",
    val isFollowing: Boolean = false,
    val isFriend: Boolean = false,
    val hasPendingRequest: Boolean = false,
    val isBanned: Boolean = false
)

data class Comment(
    val id: String,
    val postId: String,
    val author: User,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList()
)

data class Post(
    val id: String,
    val author: User,
    val timestamp: String,
    val text: String,
    val mediaUrls: List<String> = emptyList(),
    val mediaType: MediaType = MediaType.NONE,
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val reactionsCount: Map<ReactionType, Int> = mapOf(ReactionType.LIKE to 0),
    val userReaction: ReactionType = ReactionType.NONE,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isSaved: Boolean = false,
    val isReported: Boolean = false,
    val locationTag: String? = null,
    val taggedFriends: List<String> = emptyList(),
    val comments: List<Comment> = emptyList()
) {
    val totalReactions: Int
        get() = reactionsCount.values.sum()
}

data class Story(
    val id: String,
    val author: User,
    val mediaUrl: String = "",
    val text: String = "",
    val type: StoryType = StoryType.IMAGE,
    val backgroundGradient: List<Long> = listOf(0xFF6C5CE7, 0xFF00CEC9),
    val timestamp: String = "2h ago",
    val expiresAtHours: Int = 22,
    val viewCount: Int = 142,
    val isViewedByMe: Boolean = false,
    val reactionsCount: Int = 28
)

data class VideoItem(
    val id: String,
    val author: User,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val duration: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isShort: Boolean = true,
    val soundTrackTitle: String = "Original Audio - " + author.name,
    val category: String = "Trending" // Recommended, Trending, Following, Saved
)

data class FriendRequest(
    val id: String,
    val user: User,
    val mutualFriendsCount: Int = 8,
    val timestamp: String = "1d ago"
)

enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE, STICKER, EMOJI
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val mediaUrl: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: String,
    val isRead: Boolean = true,
    val replyToText: String? = null,
    val voiceDurationSec: Int? = null
)

data class ChatThread(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isGroup: Boolean = false,
    val participants: List<User> = emptyList(),
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false
)

data class Group(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val avatarUrl: String,
    val membersCount: Int,
    val isPrivate: Boolean = false,
    val isJoined: Boolean = false,
    val rules: List<String> = emptyList(),
    val category: String = "Technology",
    val posts: List<Post> = emptyList(),
    val isAdmin: Boolean = false
)

data class Page(
    val id: String,
    val name: String,
    val handle: String,
    val category: PageCategory,
    val bio: String,
    val coverUrl: String,
    val avatarUrl: String,
    val followersCount: Int,
    val isLiked: Boolean = false,
    val isFollowed: Boolean = false,
    val website: String = "https://fadx.social",
    val posts: List<Post> = emptyList()
)

data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val description: String,
    val category: ProductCategory,
    val location: String,
    val images: List<String>,
    val seller: User,
    val isSaved: Boolean = false,
    val datePosted: String = "2 days ago",
    val isSold: Boolean = false
)

data class EventItem(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val description: String,
    val coverUrl: String,
    val organizer: User,
    val goingCount: Int,
    val interestedCount: Int,
    val isGoing: Boolean = false,
    val isInterested: Boolean = false
)

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val actor: User,
    val text: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val targetId: String? = null
)

data class SavedItem(
    val id: String,
    val type: String, // "Post", "Video", "Product"
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val collectionName: String = "All Saved",
    val originalId: String
)

data class ReportItem(
    val id: String,
    val targetType: String, // "Post", "User", "Comment", "Video", "Message", "Group"
    val targetId: String,
    val targetSummary: String,
    val reporterName: String,
    val reason: ReportReason,
    val timestamp: String,
    val status: String = "Pending" // Pending, Resolved, Dismissed
)
