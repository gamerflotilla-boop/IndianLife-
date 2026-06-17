package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    fun observeProfile(uid: String = "self"): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getProfile(uid: String = "self"): UserProfile?

    @Query("SELECT * FROM user_profile ORDER BY displayName ASC")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET isPremium = :isPremium WHERE uid = :uid")
    suspend fun setPremiumStatus(isPremium: Boolean, uid: String = "self")
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY pinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<Chat>)

    @Query("UPDATE chats SET lastMessage = :msg, lastMessageTime = :time WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, msg: String, time: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnreadCount(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun observeMessages(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("UPDATE messages SET content = :newText WHERE id = :id")
    suspend fun updateMessageContent(id: String, newText: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)
}

@Dao
interface StatusUpdateDao {
    @Query("SELECT * FROM status_updates ORDER BY timestamp DESC")
    fun getAllStatus(): Flow<List<StatusUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusUpdate)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PaymentTransaction)
}

@Dao
interface AiLogDao {
    @Query("SELECT * FROM ai_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AiRequestLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AiRequestLog)
    
    @Query("SELECT SUM(costInRupees) FROM ai_logs")
    suspend fun getTotalCost(): Double?
}

@Database(
    entities = [
        UserProfile::class,
        Chat::class,
        Message::class,
        StatusUpdate::class,
        PaymentTransaction::class,
        AiRequestLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusUpdateDao(): StatusUpdateDao
    abstract fun paymentDao(): PaymentDao
    abstract fun aiLogDao(): AiLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chatverse_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChatRepository(private val db: AppDatabase) {
    val userProfile: Flow<UserProfile?> = db.userProfileDao().observeProfile()
    val chats: Flow<List<Chat>> = db.chatDao().getAllChats()
    val statusUpdates: Flow<List<StatusUpdate>> = db.statusUpdateDao().getAllStatus()
    val transactions: Flow<List<PaymentTransaction>> = db.paymentDao().getAllTransactions()
    val aiLogs: Flow<List<AiRequestLog>> = db.aiLogDao().getAllLogs()
    val allProfiles: Flow<List<UserProfile>> = db.userProfileDao().getAllProfiles()

    suspend fun getProfile(): UserProfile? = db.userProfileDao().getProfile()
    suspend fun saveProfile(profile: UserProfile) = db.userProfileDao().saveProfile(profile)
    suspend fun setPremium(uid: String = "self") = db.userProfileDao().setPremiumStatus(true, uid)
    suspend fun updatePremiumStatus(isPremium: Boolean, uid: String) = db.userProfileDao().setPremiumStatus(isPremium, uid)

    fun observeMessages(chatId: String): Flow<List<Message>> = db.messageDao().observeMessages(chatId)

    suspend fun sendMessage(msg: Message) {
        db.messageDao().insertMessage(msg)
        db.chatDao().updateLastMessage(msg.chatId, msg.content, msg.timestamp)
    }

    suspend fun saveChat(chat: Chat) = db.chatDao().insertChat(chat)
    suspend fun saveChats(chats: List<Chat>) = db.chatDao().insertChats(chats)
    suspend fun clearUnread(chatId: String) = db.chatDao().clearUnreadCount(chatId)

    suspend fun updateMessage(msgId: String, content: String) = db.messageDao().updateMessageContent(msgId, content)
    suspend fun deleteMessage(msgId: String) = db.messageDao().deleteMessage(msgId)
    suspend fun toggleStarMessage(msgId: String, starred: Boolean) = db.messageDao().setStarred(msgId, starred)

    suspend fun insertStatus(status: StatusUpdate) = db.statusUpdateDao().insertStatus(status)
    suspend fun recordTransaction(tx: PaymentTransaction) {
        db.paymentDao().insertTransaction(tx)
        db.userProfileDao().setPremiumStatus(true)
    }

    suspend fun recordAiLog(log: AiRequestLog) = db.aiLogDao().insertLog(log)
    suspend fun getTotalAiCost(): Double = db.aiLogDao().getTotalCost() ?: 0.0
}
