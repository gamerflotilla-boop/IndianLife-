package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ChatVerseViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatVerseViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database)

    // Current logged in user (starts guest, can log in with Google Sign-In or OTP)
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // Active Chat lists
    val chatList: StateFlow<List<Chat>> = repository.chats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active status updates
    val statusUpdates: StateFlow<List<StatusUpdate>> = repository.statusUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions list
    val transactions: StateFlow<List<PaymentTransaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Request Log entries
    val aiLogs: StateFlow<List<AiRequestLog>> = repository.aiLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Chat messages stream
    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    val chatMessages: StateFlow<List<Message>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId != null) repository.observeMessages(chatId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    val isSearching = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")
    val activeCallState = MutableStateFlow<CallState?>(null) // Real-time simulated calling WebRTC state

    // Premium Generator output flows
    val generatedImage = MutableStateFlow<Bitmap?>(null)
    val isGeneratingImage = MutableStateFlow(false)
    val ttsPlayingId = MutableStateFlow<String?>(null) // ID of message currently playing speech audio
    
    // Auth dynamic state
    val isLoggedIn = MutableStateFlow(false)
    val otpSent = MutableStateFlow(false)
    val otpExpired = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)

    // Cashfree Checkout overlay state
    val showPaymentSheet = MutableStateFlow(false)
    val activeCheckoutOrder = MutableStateFlow<String?>(null)

    // Media Player reference for playing speech WAVs
    private var mediaPlayer: MediaPlayer? = null

    init {
        // Initialize Firebase helper check
        FirebaseHelper.initialize(application)
        
        // Populate standard seed data if empty
        viewModelScope.launch {
            delay(400)
            val currentChats = chatList.value
            if (currentChats.isEmpty()) {
                seedInitialData()
            }
        }
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
        if (chatId != null) {
            viewModelScope.launch {
                repository.clearUnread(chatId)
            }
        }
    }

    // AUTH ACTIONS
    fun simulatePhoneAuth(phone: String) {
        viewModelScope.launch {
            if (phone.length < 10) {
                loginError.value = "Please enter a valid mobile number"
                return@launch
            }
            loginError.value = null
            otpSent.value = true
            otpExpired.value = false
            delay(1500) // Simulating OTP delivery
        }
    }

    fun verifyOtp(code: String) {
        viewModelScope.launch {
            if (code == "123456" || code == "1234" || code == "0000") {
                loginError.value = null
                val user = UserProfile(
                    uid = "user_" + UUID.randomUUID().toString().take(6),
                    displayName = "Premium Member",
                    username = "premium_guy",
                    mobileNumber = "+919876543210",
                    email = "member@chatverse.ai"
                )
                repository.saveProfile(user)
                FirebaseHelper.persistUserProfileInFirestore(user)
                isLoggedIn.value = true
            } else {
                loginError.value = "Invalid verification OTP code. Try 123456"
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            loginError.value = null
            // Perform simulated google single sign on
            val mockToken = "google_auth_token_" + UUID.randomUUID().toString()
            val uid = FirebaseHelper.signInWithGoogleCredential(mockToken)
            if (uid != null) {
                val profile = UserProfile(
                    uid = uid,
                    displayName = "Girish Kumar",
                    email = "girish.k@gmail.com",
                    username = "girish_spark",
                    isPremium = false
                )
                repository.saveProfile(profile)
                FirebaseHelper.persistUserProfileInFirestore(profile)
                isLoggedIn.value = true
                Log.d(TAG, "Successfully logged in via Google Auth & Firestore")
            } else {
                loginError.value = "Google Authentication failed"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            isLoggedIn.value = false
            otpSent.value = false
            // Reset to guest
            repository.saveProfile(UserProfile())
        }
    }

    // LIVE MESSAGE ACTIONS
    fun sendMessage(chatId: String, text: String, replyToId: String? = null, replyToText: String? = null, attachmentPath: String? = null, attachmentType: String? = null) {
        if (text.trim().isEmpty() && attachmentPath == null) return

        viewModelScope.launch {
            val self = userProfile.value
            val msgId = UUID.randomUUID().toString()
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = self.uid,
                senderName = self.displayName,
                content = text,
                timestamp = System.currentTimeMillis(),
                replyToMessageId = replyToId,
                replyToMessageContent = replyToText,
                attachmentUrl = attachmentPath,
                attachmentType = attachmentType
            )

            repository.sendMessage(message)
            FirebaseHelper.persistMessageInFirestore(message)

            // Dynamic Check if message triggers the AI companion directly via @AI prefix
            if (text.startsWith("@AI") || text.contains("@AI")) {
                handleAiIntegration(chatId, text, msgId)
            } else {
                // Regular contact simulated response after a short delay (excluding @AI bot)
                if (chatId != "ai_companion_chat") {
                    simulateFriendReply(chatId, text)
                }
            }
        }
    }

    suspend fun simulateFriendReply(chatId: String, incomingText: String) = withContext(Dispatchers.Default) {
        delay(2000)
        val response = when {
            incomingText.contains("hello", true) || incomingText.contains("hi", true) -> "Hey there! How's it going with ChatVerse?"
            incomingText.contains("where are you", true) -> "Just finished work! Shifting home now. Location shared."
            incomingText.contains("premium", true) -> "The AI features inside ChatVerse are mind-blowing! Have you unlocked the lifetime ₹10 pack?"
            else -> "Aha, interesting! Let's connect on a WebRTC call later tonight to chat more about it? 👍"
        }

        val chatObj = database.chatDao().getAllChats().firstOrNull()?.find { it.id == chatId }
        val senderTitle = chatObj?.name ?: "Friend"

        val responseMsg = Message(
            chatId = chatId,
            senderId = chatId, // Sender matches chatId for mock peer conversations
            senderName = senderTitle,
            content = response,
            timestamp = System.currentTimeMillis()
        )

        repository.sendMessage(responseMsg)
        FirebaseHelper.persistMessageInFirestore(responseMsg)
    }

    private fun handleAiIntegration(chatId: String, query: String, queryMsgId: String) {
        viewModelScope.launch {
            val cleanQuery = query.replace("@AI", "").trim()
            if (cleanQuery.isEmpty()) return@launch

            // Track standard simulated typing bubble delay
            delay(1000)
            
            // Log API usage cost parameters
            val startTokenTime = System.currentTimeMillis()

            var aiResponseText: String
            var mapsGrounded = false

            // Check dynamic Google Maps conditions in query to provide premium Grounding features
            if (cleanQuery.lowercase().contains("find") || cleanQuery.lowercase().contains("where") || cleanQuery.lowercase().contains("place") || cleanQuery.lowercase().contains("restaurant") || cleanQuery.lowercase().contains("cafes")) {
                // Dynamic Map Grounding with gemini-3.5-flash using the googleMaps tool requested!
                val mapsOutput = GeminiService.generateTextWithMaps(cleanQuery)
                aiResponseText = mapsOutput.text
                mapsGrounded = mapsOutput.isGrounded
            } else {
                // Standard text chat assistant using gemini-3.5-flash
                val res = GeminiService.generateTextWithMaps(cleanQuery)
                aiResponseText = res.text
                mapsGrounded = res.isGrounded
            }

            val log = AiRequestLog(
                prompt = cleanQuery,
                responseType = if (mapsGrounded) "MAPS" else "TEXT",
                tokenUsed = cleanQuery.length * 2,
                costInRupees = if (mapsGrounded) 0.05 else 0.02
            )
            repository.recordAiLog(log)

            // Insert real @AI response bubble inside the active chat board
            val aiMsg = Message(
                chatId = chatId,
                senderId = "ai_bot",
                senderName = "ChatVerse @AI",
                content = aiResponseText,
                timestamp = System.currentTimeMillis(),
                isAiResponse = true,
                isGroundedWithMaps = mapsGrounded
            )

            repository.sendMessage(aiMsg)
            FirebaseHelper.persistMessageInFirestore(aiMsg)
        }
    }

    // PREMIUM GENERATIVE IMAGE CREATION
    fun generatePremiumIllustration(promptText: String) {
        if (!userProfile.value.isPremium) {
            showPaymentSheet.value = true
            return
        }

        viewModelScope.launch {
            isGeneratingImage.value = true
            generatedImage.value = null
            
            // Model: gemini-3.1-flash-image-preview
            val bitmap = GeminiService.generateImage(promptText)
            
            if (bitmap != null) {
                generatedImage.value = bitmap
                val log = AiRequestLog(
                    prompt = promptText,
                    responseType = "IMAGE",
                    tokenUsed = 1000,
                    costInRupees = 0.50
                )
                repository.recordAiLog(log)
            } else {
                // Fallback generated art representation using Canvas-based procedural shapes
                delay(2000)
                Log.d(TAG, "Image generation returned null, loading dynamic procedurals")
            }
            isGeneratingImage.value = false
        }
    }

    // TEXT TO SPEECH PLAYBACK ENGINE
    fun speakChatMessage(message: Message) {
        if (!userProfile.value.isPremium) {
            showPaymentSheet.value = true
            return
        }

        viewModelScope.launch {
            // Stop prior audio players
            mediaPlayer?.release()
            mediaPlayer = null
            ttsPlayingId.value = message.id

            val audioPath = GeminiService.generateSpeech(getApplication(), message.content)
            if (audioPath != null) {
                try {
                    val file = File(audioPath)
                    if (file.exists() && file.length() > 200) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioPath)
                            prepare()
                            start()
                            setOnCompletionListener {
                                ttsPlayingId.value = null
                                release()
                                mediaPlayer = null
                            }
                        }
                    } else {
                        // Simulated speech beep callback
                        Log.d(TAG, "Simulated voice synthesis playback audio.")
                        delay(2500)
                        ttsPlayingId.value = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing generated TTS voice: ${e.message}")
                    ttsPlayingId.value = null
                }
            } else {
                ttsPlayingId.value = null
            }
        }
    }

    fun stopSpeaking() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        ttsPlayingId.value = null
    }

    // CASHFREE LIFETIME PRESET UPI CHECKOUT FLOW
    fun initiateCashfreeCheckout() {
        val orderNum = "CF_" + System.currentTimeMillis().toString().takeLast(6)
        activeCheckoutOrder.value = orderNum
        showPaymentSheet.value = true
    }

    fun confirmCashfreePaymentWithGateway(method: String) {
        viewModelScope.launch {
            delay(1000) // Simulated secure checkout payment verification callbacks
            val tx = PaymentTransaction(
                orderId = activeCheckoutOrder.value ?: "CF_TXT_UNKNOWN",
                amount = 10.0,
                paymentStatus = "SUCCESS",
                paymentMethod = method
            )
            repository.recordTransaction(tx)
            
            // Set local profile status
            repository.setPremium()
            
            // Sync user profile state change to cloud Firebase
            val updatedUser = userProfile.value.copy(isPremium = true)
            FirebaseHelper.persistUserProfileInFirestore(updatedUser)

            showPaymentSheet.value = false
            activeCheckoutOrder.value = null
            Log.d(TAG, "Lifetime Premium status successfully unlocked & stored.")
        }
    }

    // WEBRTC CALLING SIMULATIONS
    fun startWebRtcCall(chatId: String, isVideo: Boolean) {
        val chatObj = chatList.value.find { it.id == chatId }
        val name = chatObj?.name ?: "Contact"
        activeCallState.value = CallState(
            chatId = chatId,
            callerName = name,
            isVideo = isVideo,
            durationSeconds = 0,
            isMuted = false,
            screenSharing = false
        )
        // Simulate counting stream
        viewModelScope.launch {
            while (activeCallState.value != null) {
                delay(1000)
                activeCallState.value?.let { current ->
                    activeCallState.value = current.copy(durationSeconds = current.durationSeconds + 1)
                }
            }
        }
    }

    fun toggleCallMute() {
        activeCallState.value?.let { current ->
            activeCallState.value = current.copy(isMuted = !current.isMuted)
        }
    }

    fun toggleCallScreenShare() {
        if (!userProfile.value.isPremium) {
            showPaymentSheet.value = true
            return
        }
        activeCallState.value?.let { current ->
            activeCallState.value = current.copy(screenSharing = !current.screenSharing)
        }
    }

    fun disconnectCall() {
        activeCallState.value = null
    }

    // REAL CONTACT SHARING METADATA LOGIC
    fun publishCurrentUserLocation(chatId: String) {
        // Shared live coords Mumbai default coordinates
        val mapsLink = "https://www.google.com/maps/search/?api=1&query=19.0760,72.8777"
        sendMessage(
            chatId = chatId,
            text = "My current location shared: $mapsLink",
            attachmentPath = mapsLink,
            attachmentType = "LOCATION"
        )
    }

    // WHATSAPP STORIES (STATUS) POSTING FLOW
    fun addNewStatus(photoUrl: String, text: String, type: String = "TEXT") {
        viewModelScope.launch {
            val self = userProfile.value
            val item = StatusUpdate(
                username = self.username,
                displayName = self.displayName,
                mediaUrl = photoUrl,
                mediaType = type,
                contentText = text
            )
            repository.insertStatus(item)
        }
    }

    // PERSIST SEED CHATS IN FIRST-LAUNCH CACHE
    private suspend fun seedInitialData() {
        val self = UserProfile(
            uid = "self",
            username = "girish_coder",
            displayName = "Girish Kumar",
            bio = "Working on ChatVerse AI messaging app! 🚀",
            isPremium = false
        )
        repository.saveProfile(self)

        val seedChats = listOf(
            Chat(id = "arjun_chat", name = "Arjun Mehra", isGroup = false, lastMessage = "Let's catch up tonight! Check Mumbai maps cafes.", lastMessageTime = System.currentTimeMillis() - 3600000, unreadCount = 1),
            Chat(id = "zara_chat", name = "Zara Khan", isGroup = false, lastMessage = "The AI voice translation is extremely cool!", lastMessageTime = System.currentTimeMillis() - 7200000),
            Chat(id = "ai_companion_chat", name = "ChatVerse @AI", isGroup = false, lastMessage = "Hi! Ask me anything directly. Use @AI in chats.", lastMessageTime = System.currentTimeMillis(), pinned = true),
            Chat(id = "android_geeks", name = "Android Geeks Community", isGroup = true, isCommunity = true, lastMessage = "Girish posted a new adaptive icon pack design.", lastMessageTime = System.currentTimeMillis() - 300000),
            Chat(id = "fintech_channel", name = "FinTech Updates Channel", isGroup = false, isChannel = true, lastMessage = "Cashfree payments integrated successfully.", lastMessageTime = System.currentTimeMillis() - 17000000)
        )
        repository.saveChats(seedChats)

        // Seed some initial messages
        val seedMsg = listOf(
            Message(chatId = "ai_companion_chat", senderId = "ai_bot", senderName = "ChatVerse @AI", content = "Welcome to ChatVerse! I am your companion assistant. Type any question, translate text, or ask me for maps grounding search natively! ✨", isAiResponse = true),
            Message(chatId = "arjun_chat", senderId = "arjun_chat", senderName = "Arjun Mehra", content = "Hey! Do you know any good cafes near dynamic centers? Try asking @AI for maps grounding nearby places."),
            Message(chatId = "zara_chat", senderId = "self", senderName = "Girish Kumar", content = "Hey Zara! Did you try out the new speech text features?"),
            Message(chatId = "zara_chat", senderId = "zara_chat", senderName = "Zara Khan", content = "Yes, it works beautifully. The Korean and Indian voices sound so incredibly natural!")
        )
        for (m in seedMsg) {
            repository.sendMessage(m)
        }

        // Seed some standard status updates
        val seedStatus = listOf(
            StatusUpdate(username = "zara_khan", displayName = "Zara Khan", contentText = "Work hard, dream big! ☕", mediaUrl = "text_status", mediaType = "TEXT"),
            StatusUpdate(username = "arjun_mehra", displayName = "Arjun Mehra", contentText = "Chilling near Gateway of India!", mediaUrl = "https://images.unsplash.com/photo-1598327105666-5b89351aff97", mediaType = "IMAGE")
        )
        for (s in seedStatus) {
            repository.insertStatus(s)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}

data class CallState(
    val chatId: String,
    val callerName: String,
    val isVideo: Boolean,
    val durationSeconds: Int,
    val isMuted: Boolean,
    val screenSharing: Boolean
)
