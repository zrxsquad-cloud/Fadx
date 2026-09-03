package com.example.data.supabase

import com.example.model.MediaType
import com.example.model.Post
import com.example.model.PostVisibility
import com.example.model.ReactionType
import com.example.model.User
import org.json.JSONArray
import org.json.JSONObject

data class SupabasePost(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String,
    val content: String,
    val mediaUrls: List<String> = emptyList(),
    val mediaType: String = "NONE",
    val visibility: String = "PUBLIC",
    val location: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val createdAt: String? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("user_id", userId)
        json.put("author_name", authorName)
        json.put("author_username", authorUsername)
        json.put("author_avatar", authorAvatar)
        json.put("content", content)

        val mediaArray = JSONArray()
        mediaUrls.forEach { mediaArray.put(it) }
        json.put("media_urls", mediaArray)

        json.put("media_type", mediaType)
        json.put("visibility", visibility)
        if (location != null) json.put("location", location)
        json.put("likes_count", likesCount)
        json.put("comments_count", commentsCount)
        json.put("shares_count", sharesCount)
        return json.toString()
    }

    fun toAppPost(): Post {
        val postMediaType = try {
            MediaType.valueOf(mediaType.uppercase())
        } catch (e: Exception) {
            if (mediaUrls.size > 1) MediaType.CAROUSEL else if (mediaUrls.size == 1) MediaType.IMAGE else MediaType.NONE
        }

        val postVisibility = try {
            PostVisibility.valueOf(visibility.uppercase())
        } catch (e: Exception) {
            PostVisibility.PUBLIC
        }

        val author = User(
            id = userId,
            name = authorName.ifBlank { "Fadx User" },
            username = authorUsername.ifBlank { "user_${userId.take(6)}" },
            email = "",
            avatarUrl = authorAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500&q=80" }
        )

        return Post(
            id = id,
            author = author,
            timestamp = "Recently",
            text = content,
            mediaUrls = mediaUrls,
            mediaType = postMediaType,
            visibility = postVisibility,
            locationTag = location,
            reactionsCount = if (likesCount > 0) mapOf(ReactionType.LIKE to likesCount) else emptyMap(),
            commentsCount = commentsCount,
            sharesCount = sharesCount
        )
    }

    companion object {
        fun fromAppPost(post: Post): SupabasePost {
            return SupabasePost(
                id = post.id,
                userId = post.author.id,
                authorName = post.author.name,
                authorUsername = post.author.username,
                authorAvatar = post.author.avatarUrl,
                content = post.text,
                mediaUrls = post.mediaUrls,
                mediaType = post.mediaType.name,
                visibility = post.visibility.name,
                location = post.locationTag,
                likesCount = post.reactionsCount.values.sum(),
                commentsCount = post.commentsCount,
                sharesCount = post.sharesCount
            )
        }

        fun fromJsonObject(json: JSONObject): SupabasePost {
            val mediaUrlsList = mutableListOf<String>()
            val mediaArray = json.optJSONArray("media_urls")
            if (mediaArray != null) {
                for (i in 0 until mediaArray.length()) {
                    mediaUrlsList.add(mediaArray.optString(i))
                }
            }

            return SupabasePost(
                id = json.optString("id", ""),
                userId = json.optString("user_id", ""),
                authorName = json.optString("author_name", ""),
                authorUsername = json.optString("author_username", ""),
                authorAvatar = json.optString("author_avatar", ""),
                content = json.optString("content", ""),
                mediaUrls = mediaUrlsList,
                mediaType = json.optString("media_type", "NONE"),
                visibility = json.optString("visibility", "PUBLIC"),
                location = if (json.has("location") && !json.isNull("location")) json.optString("location") else null,
                likesCount = json.optInt("likes_count", 0),
                commentsCount = json.optInt("comments_count", 0),
                sharesCount = json.optInt("shares_count", 0),
                createdAt = if (json.has("created_at") && !json.isNull("created_at")) json.optString("created_at") else null
            )
        }
    }
}
