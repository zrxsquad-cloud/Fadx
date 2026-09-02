package com.example.ui.screens.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.MessageType
import com.example.model.User
import com.example.ui.components.AvatarImage
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxAccentGreen
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesListScreen(
    threads: List<ChatThread>,
    onThreadClick: (ChatThread) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredThreads = if (searchQuery.isBlank()) threads else threads.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Edit, contentDescription = "New Message", tint = FadxPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    focusedBorderColor = FadxPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Active Online Friends Horizontal Row
            Text(
                text = "Active Now",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredThreads) { thread ->
                    ChatThreadRow(thread = thread, onClick = { onThreadClick(thread) })
                }
            }
        }
    }
}

@Composable
fun ChatThreadRow(
    thread: ChatThread,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarImage(
            url = thread.avatarUrl,
            name = thread.name,
            size = 52.dp,
            showOnlineIndicator = true,
            isOnline = thread.isOnline
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = thread.lastMessageTime,
                    fontSize = 12.sp,
                    color = if (thread.unreadCount > 0) FadxPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (thread.isTyping) "typing..." else thread.lastMessage,
                    fontSize = 13.5.sp,
                    color = if (thread.isTyping) FadxSecondary else if (thread.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (thread.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(FadxPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thread.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    chatId: String,
    thread: ChatThread?,
    messages: List<ChatMessage>,
    currentUserId: String,
    onSendMessage: (String, MessageType, String?) -> Unit,
    onBackClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarImage(
                            url = thread?.avatarUrl,
                            name = thread?.name ?: "Chat",
                            size = 38.dp,
                            showOnlineIndicator = true,
                            isOnline = thread?.isOnline ?: false
                        )
                        Column {
                            Text(
                                text = thread?.name ?: "Chat Conversation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (thread?.isOnline == true) "Active now" else "Offline",
                                fontSize = 11.sp,
                                color = if (thread?.isOnline == true) FadxAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = FadxPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = FadxPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            // Message Bubbles
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUserId
                    MessageBubble(message = msg, isMe = isMe)
                }
            }

            // Quick Emojis bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("❤️", "👍", "🔥", "😂", "👏", "🎉", "👀").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSendMessage(emoji, MessageType.EMOJI, null) }
                            .padding(4.dp)
                    )
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Media attachment button
                IconButton(
                    onClick = {
                        // Send sample image
                        onSendMessage(
                            "Sent a photo",
                            MessageType.IMAGE,
                            "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80"
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Media",
                        tint = FadxPrimary
                    )
                }

                // Voice note button simulation
                IconButton(
                    onClick = {
                        onSendMessage("Voice note (0:18)", MessageType.VOICE, null)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Send Voice Note",
                        tint = FadxSecondary
                    )
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Message...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        focusedBorderColor = FadxPrimary
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText, MessageType.TEXT, null)
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(FadxPrimary)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) FadxPrimary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.messageType == MessageType.IMAGE && !message.mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Chat media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (message.messageType == MessageType.VOICE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play voice note",
                            tint = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "||||||||||||||||| 0:18",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        fontSize = if (message.messageType == MessageType.EMOJI) 28.sp else 14.sp,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isMe) {
                Icon(
                    imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                    contentDescription = null,
                    tint = if (message.isRead) FadxSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
