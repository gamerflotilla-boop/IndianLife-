package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val uid: String = "self",
    val username: String = "currentUser",
    val displayName: String = "Guest User",
    val profilePicUrl: String = "",
    val bio: String = "Hey there! I am using ChatVerse AI.",
    val email: String = "guest@example.com",
    val mobileNumber: String = "+919876543210",
    val password: String = "Member@123",
    val isPremium: Boolean = false,
    val lastSeenStr: String = "Online",
    val hideOnlineStatus: Boolean = false,
    val hideReadReceipts: Boolean = false
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String = "",
    val isGroup: Boolean = false,
    val isChannel: Boolean = false,
    val isCommunity: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean = true,
    val isDelivered: Boolean = true,
    val isRead: Boolean = true,
    val replyToMessageId: String? = null,
    val replyToMessageContent: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null, // "IMAGE", "AUDIO", "LOCATION", "CONTACT", "DOCUMENT"
    val isStarred: Boolean = false,
    val reactionsStr: String = "", // comma separated list like "😄,❤"
    val isAiResponse: Boolean = false,
    val isGroundedWithMaps: Boolean = false
)

@Entity(tableName = "status_updates")
data class StatusUpdate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val mediaUrl: String,
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "GIF"
    val contentText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val viewersCount: Int = 0
)

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey val orderId: String,
    val amount: Double = 10.0,
    val paymentStatus: String = "SUCCESS", // "SUCCESS", "PENDING", "FAILED"
    val paymentMethod: String = "UPI",
    val referenceId: String = "CRFB" + (100000 + (Math.random() * 900000).toInt()),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_logs")
data class AiRequestLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val responseType: String, // "TEXT", "IMAGE", "TTS", "MAPS"
    val timestamp: Long = System.currentTimeMillis(),
    val tokenUsed: Int = 0,
    val costInRupees: Double = 0.0
)
