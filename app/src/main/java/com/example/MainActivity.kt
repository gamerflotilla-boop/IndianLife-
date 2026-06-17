package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.CallState
import com.example.ui.ChatVerseViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatVerseApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatVerseApp(viewModel: ChatVerseViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
    } else {
        var currentTab by remember { mutableStateOf(0) }
        val activeChatId by viewModel.selectedChatId.collectAsState()
        val showPaymentSheet by viewModel.showPaymentSheet.collectAsState()

        Scaffold(
            topBar = {
                if (activeChatId == null) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "ChatVerse AI Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ChatVerse AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.initiateCashfreeCheckout() }) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = "Upgrade Premium Shop",
                                    tint = SparkAccent
                                )
                            }
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout Current User"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MidnightBg
                        )
                    )
                }
            },
            bottomBar = {
                if (activeChatId == null) {
                    NavigationBar(
                        containerColor = MidnightBg,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.Default.Forum, contentDescription = "Chats board") },
                            label = { Text("Chats") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextLight,
                                selectedTextColor = TextLight,
                                indicatorColor = ChatBubbleSelf
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.Psychology, contentDescription = "AI Universe portal") },
                            label = { Text("AI") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextLight,
                                selectedTextColor = TextLight,
                                indicatorColor = ChatBubbleSelf
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.Phone, contentDescription = "RTC Call center") },
                            label = { Text("Calls") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextLight,
                                selectedTextColor = TextLight,
                                indicatorColor = ChatBubbleSelf
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(Icons.Default.AutoAwesomeMotion, contentDescription = "Status carousel") },
                            label = { Text("Stories") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextLight,
                                selectedTextColor = TextLight,
                                indicatorColor = ChatBubbleSelf
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 4,
                            onClick = { currentTab = 4 },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Metrics monitor") },
                            label = { Text("Stats") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextLight,
                                selectedTextColor = TextLight,
                                indicatorColor = ChatBubbleSelf
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MidnightBg)
            ) {
                // Render view based on active tab
                when (currentTab) {
                    0 -> ChatsTabScreen(viewModel = viewModel)
                    1 -> AiUniverseTabScreen(viewModel = viewModel)
                    2 -> CallsTabScreen(viewModel = viewModel)
                    3 -> StatusTabScreen(viewModel = viewModel)
                    4 -> MetricsTabScreen(viewModel = viewModel)
                }

                // Immersive active chat thread screen shown as fullscreen slide-over
                AnimatedVisibility(
                    visible = activeChatId != null,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                ) {
                    ChatConversationScreen(viewModel = viewModel)
                }

                // Simulated adaptive voice call overlay active
                val activeCalls by viewModel.activeCallState.collectAsState()
                if (activeCalls != null) {
                    VoiceVideoCallOverlay(callState = activeCalls!!, viewModel = viewModel)
                }

                // Cashfree checkout billing portal sheet shown as Dialog
                if (showPaymentSheet) {
                    CashfreeCheckoutDialog(viewModel = viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// AUTHENTICATION SCREEN DESIGN WITH GOOGLE & OTP
// -------------------------------------------------------------

@Composable
fun AuthScreen(viewModel: ChatVerseViewModel) {
    var mobileNum by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    val otpSent by viewModel.otpSent.collectAsState()
    val errState by viewModel.loginError.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightBg, Color(0xFF0F172A), MidnightBg)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // App Branding
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "ChatVerse AI App Icon",
                    tint = ChatBubbleSelf,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to ChatVerse AI",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = "WhatsApp + Telegram + Dynamic @AI Grounding",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = if (!otpSent) "Sign In via Mobile" else "Enter Verification OTP",
                        fontWeight = FontWeight.SemiBold,
                        color = TextLight,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!otpSent) {
                        OutlinedTextField(
                            value = mobileNum,
                            onValueChange = { mobileNum = it },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("+91 99999 88888") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone icon") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mobile_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = ChatBubbleSelf
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.simulatePhoneAuth(mobileNum) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("request_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChatBubbleSelf)
                        ) {
                            Text("Request OTP", fontSize = 16.sp, color = TextLight, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("6-Digit Verification Code") },
                            placeholder = { Text("Type 123456") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Secure lock icon") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = ChatBubbleSelf
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.verifyOtp(otpCode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Pink80)
                        ) {
                            Text("Verify & Continue", fontSize = 16.sp, color = TextLight, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { viewModel.simulatePhoneAuth(mobileNum) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Resend Verification Code", color = ChatBubbleSelf)
                        }
                    }

                    if (errState != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errState!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google login divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = TextMuted.copy(alpha = 0.3f))
                Text("  OR CONTINUE WITH  ", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = TextMuted.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gmail Sign In
            OutlinedButton(
                onClick = { viewModel.loginWithGoogle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_login_button"),
                border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = "Google authentication",
                        tint = Pink80
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Secure Google Sign-In with Firebase", color = TextLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CHATS TAB WINDOW IMPLEMENTATION
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsTabScreen(viewModel: ChatVerseViewModel) {
    val chats by viewModel.chatList.collectAsState()
    val searchActive by viewModel.isSearching.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // WhatsApp style status bubble carousel across active chats
        StatusBubbleCarousel(viewModel = viewModel)

        // Telegram custom Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("chat_search_bar"),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Search messages, channels, @AI summaries...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search filter icon", tint = TextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                unfocusedContainerColor = MidnightSurface,
                focusedContainerColor = MidnightSurface
            )
        )

        val filteredChats = chats.filter {
            it.name.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true)
        }

        if (filteredChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Empty filter results", tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No chats or channels found matching '$query'", color = TextMuted, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredChats) { chat ->
                    ChatItem(chat = chat, onClick = { viewModel.selectChat(chat.id) })
                    HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat Avatar Bubble
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (chat.id == "ai_companion_chat") Brush.radialGradient(listOf(SparkAccent, MidnightSurface))
                        else Brush.linearGradient(listOf(ChatBubbleSelf, Pink80)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (chat.id == "ai_companion_chat") {
                    Icon(Icons.Default.Psychology, contentDescription = "AI companion", tint = TextLight, modifier = Modifier.size(30.dp))
                } else {
                    Text(
                        text = chat.name.take(2).uppercase(),
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            // Online status tick
            if (chat.id == "ai_companion_chat" || chat.id == "arjun_chat") {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(0xFF10B981), CircleShape)
                        .border(2.dp, MidnightBg, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = TextLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (chat.id == "ai_companion_chat") {
                        Box(
                            modifier = Modifier
                                .background(SparkAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("AI BOT", color = SparkAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (chat.isChannel) {
                        Icon(Icons.Default.Verified, contentDescription = "Verified Channel", tint = Purple80, modifier = Modifier.size(16.dp))
                    }
                }
                
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                Text(
                    text = sdf.format(Date(chat.lastMessageTime)),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.id == "ai_companion_chat" && chat.lastMessage.startsWith("Hi! Ask me")) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI spark", tint = SparkAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = chat.lastMessage,
                    color = TextMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(Pink80, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STATUS / STORIES TAB WINDOW IMPLEMENTATION
// -------------------------------------------------------------

@Composable
fun StatusTabScreen(viewModel: ChatVerseViewModel) {
    val statuses by viewModel.statusUpdates.collectAsState()
    var showNewStatusDialog by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var viewingStatusUpdate by remember { mutableStateOf<StatusUpdate?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Stories", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextLight)
            Button(
                onClick = { showNewStatusDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ChatBubbleSelf),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "New Status Update")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Story")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(statuses) { status ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewingStatusUpdate = status },
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Pink80, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(status.displayName.take(1), fontWeight = FontWeight.Bold, color = TextLight)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(status.displayName, fontWeight = FontWeight.SemiBold, color = TextLight, fontSize = 16.sp)
                            Text(
                                text = if (status.mediaType == "TEXT") status.contentText else "📷 View Image Story Detail",
                                fontSize = 13.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewStatusDialog) {
        Dialog(onDismissRequest = { showNewStatusDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Share a Status Story Update", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        placeholder = { Text("What's on your mind? Type a status or premium post...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showNewStatusDialog = false }) { Text("Cancel", color = TextMuted) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.addNewStatus("text_status", statusText, "TEXT")
                                showNewStatusDialog = false
                                statusText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Pink80)
                        ) {
                            Text("Post Update")
                        }
                    }
                }
            }
        }
    }

    if (viewingStatusUpdate != null) {
        Dialog(onDismissRequest = { viewingStatusUpdate = null }) {
            val status = viewingStatusUpdate!!
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(36.dp).background(Purple80, CircleShape), contentAlignment = Alignment.Center) {
                            Text(status.displayName.take(1), color = TextLight, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(status.displayName, fontWeight = FontWeight.Bold, color = TextLight)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (status.mediaType == "IMAGE") {
                        AsyncImage(
                            model = status.mediaUrl,
                            contentDescription = "Story graphic image",
                            modifier = Modifier.height(300.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Brush.radialGradient(listOf(ChatBubbleSelf, MidnightBg)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = status.contentText,
                                textAlign = TextAlign.Center,
                                color = TextLight,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(text = "Expires naturally in 24 hours (simulation parameters)", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewingStatusUpdate = null }, colors = ButtonDefaults.buttonColors(containerColor = ChatBubbleSelf)) {
                        Text("Close Story")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBubbleCarousel(viewModel: ChatVerseViewModel) {
    val statuses by viewModel.statusUpdates.collectAsState()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp, start = 16.dp)
    ) {
        items(statuses) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .width(62.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .border(2.dp, Pink80, CircleShape)
                        .padding(3.dp)
                        .background(Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Purple80, CircleShape), contentAlignment = Alignment.Center) {
                        Text(item.displayName.take(1), color = TextLight, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.displayName.substringBefore(" "),
                    fontSize = 11.sp,
                    color = TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CALLS / LOCATION TAB WINDOW IMPLEMENTATION
// -------------------------------------------------------------

@Composable
fun CallsTabScreen(viewModel: ChatVerseViewModel) {
    val chats by viewModel.chatList.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Voice/Video Call Center", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextLight)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Experience direct WebRTC peer connections simulations", color = TextMuted, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(chats.filter { !it.isChannel && !it.isCommunity }) { chat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(ChatBubbleSelf, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(chat.name.take(1), fontWeight = FontWeight.Bold, color = TextLight)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(chat.name, fontWeight = FontWeight.SemiBold, color = TextLight, fontSize = 16.sp)
                                Text("Online - click call", color = TextMuted, fontSize = 12.sp)
                            }
                        }

                        Row {
                            IconButton(onClick = { viewModel.startWebRtcCall(chat.id, false) }) {
                                Icon(Icons.Default.Call, contentDescription = "Voice Call trigger", tint = Pink80)
                            }
                            IconButton(onClick = { viewModel.startWebRtcCall(chat.id, true) }) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video Call trigger", tint = Purple80)
                            }
                            IconButton(onClick = {
                                viewModel.publishCurrentUserLocation(chat.id)
                                Toast.makeText(context, "Location coordinates shared in chat with peer!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Share Location trigger", tint = SparkAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// WEBRTC CALL OVERLAY WINDOW MODIFIER
// -------------------------------------------------------------

@Composable
fun VoiceVideoCallOverlay(callState: CallState, viewModel: ChatVerseViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {}, // Intercept touch events
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(32.dp)
        ) {
            Text(
                text = if (callState.isVideo) "HD WEBRTC VIDEO CALL" else "WEBRTC AUDIO CALL",
                color = Purple80,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(ChatBubbleSelf, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(callState.callerName.take(1), fontSize = 48.sp, color = TextLight, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(callState.callerName, fontWeight = FontWeight.Bold, color = TextLight, fontSize = 24.sp)
            
            val minutes = callState.durationSeconds / 60
            val seconds = callState.durationSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                color = TextLight.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (callState.isVideo) {
                // Render simulated WebRTC HD camera stream card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.DarkGray, RoundedCornerShape(16.dp))
                        .border(2.dp, Purple80, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState.screenSharing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ScreenShare, contentDescription = "Screen sharing active", tint = SparkAccent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Screen Sharing Active (Premium Benefit)", color = TextLight, fontSize = 13.sp)
                        }
                    } else {
                        Text("Active Front Camera Stream", color = TextLight.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Call controls
                IconButton(
                    onClick = { viewModel.toggleCallMute() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (callState.isMuted) Color.Red else Color.DarkGray, CircleShape)
                ) {
                    Icon(
                        imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute mic button",
                        tint = TextLight
                    )
                }

                IconButton(
                    onClick = { viewModel.disconnectCall() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Red, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Disconnect call",
                        tint = TextLight
                    )
                }

                if (callState.isVideo) {
                    IconButton(
                        onClick = { viewModel.toggleCallScreenShare() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (callState.screenSharing) SparkAccent else Color.DarkGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenShare,
                            contentDescription = "Screen share controller",
                            tint = TextLight
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PREMIUM SHOP CASHFREE UPI PAYMENT MODAL WINDOW
// -------------------------------------------------------------

@Composable
fun CashfreeCheckoutDialog(viewModel: ChatVerseViewModel) {
    Dialog(onDismissRequest = { viewModel.showPaymentSheet.value = false }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Cashfree verification logo", tint = Pink80, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cashfree Secure Gateway", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Lifetime Premium Membership", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SparkAccent)
                Text(
                    text = "Gain complete access to ChatGPT-powered @AI bots, text to speech, maps location grounding, and custom AI illustrations maker.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightBg, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("One-Time Payment Fee", color = TextLight, fontWeight = FontWeight.Medium)
                        Text("₹10.00", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Select Cashfree Payment Method", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                // UPI Methods
                Button(
                    onClick = { viewModel.confirmCashfreePaymentWithGateway("Google Pay") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChatBubbleSelf),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Pay via UPI GPay")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Pay via Google Pay / UPI", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.confirmCashfreePaymentWithGateway("PhonePe") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B21B6)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = "Pay via PhonePe")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Pay via PhonePe", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.confirmCashfreePaymentWithGateway("Credit/Debit Card") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = "Credit Card checkout")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Netbanking / Alternate Cards", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { viewModel.showPaymentSheet.value = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel Transaction", color = TextMuted)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CHAT INNER SCREEN CONVERSATIONBOARD THREAD
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(viewModel: ChatVerseViewModel) {
    val activeChatId by viewModel.selectedChatId.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val chatList by viewModel.chatList.collectAsState()
    val selfUser by viewModel.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    if (activeChatId == null) return

    val currentChatObj = chatList.find { it.id == activeChatId }
    val title = currentChatObj?.name ?: "Chat"

    var inputMsg by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to the bottom of the list when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ChatBubbleSelf, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title.take(1), fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight)
                            Text("Active Session", color = Pink80, fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectChat(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return back lists", tint = TextLight)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startWebRtcCall(activeChatId!!, false) }) {
                        Icon(Icons.Default.Call, contentDescription = "RTC Call Button", tint = TextLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MidnightBg)
        ) {
            // Alert non-premium users inside chat
            if (!selfUser.isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Pink80.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💡 Type @AI <question> here to call AI! Upgrade to unlock speech synthesis.", fontSize = 12.sp, color = TextLight, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.initiateCashfreeCheckout() }) {
                            Text("Upgrade", color = SparkAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Message Bubble list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                items(messages) { msg ->
                    val isOwn = msg.senderId == selfUser.uid
                    MessageBubble(
                        message = msg,
                        isOwn = isOwn,
                        viewModel = viewModel
                    )
                }
            }

            // Quick Assist Bar for premium users
            QuickAssistBar(onSelected = { tag -> inputMsg += tag })

            // Message Typing input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMsg,
                    onValueChange = { inputMsg = it },
                    placeholder = { Text("Type message, use @AI prefix...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        unfocusedContainerColor = MidnightSurface,
                        focusedContainerColor = MidnightSurface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        viewModel.sendMessage(activeChatId!!, inputMsg)
                        inputMsg = ""
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_button"),
                    shape = CircleShape,
                    containerColor = ChatBubbleSelf
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message bubble trigger",
                        tint = TextLight
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAssistBar(onSelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        val quicks = listOf(
            "@AI Summarize Chat",
            "@AI Generate Sticker: ",
            "@AI Correct Grammar: ",
            "@AI Professional Mode: "
        )
        items(quicks) { tag ->
            Card(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onSelected(tag) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Text(
                    text = tag.take(18) + if (tag.length > 18) "..." else "",
                    color = SparkAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    viewModel: ChatVerseViewModel
) {
    val ttsPlayingId by viewModel.ttsPlayingId.collectAsState()
    val isPlaying = ttsPlayingId == message.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        val containerColor = if (isOwn) ChatBubbleSelf else ChatBubblePeer
        val roundedCorners = if (isOwn) {
            RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
        } else {
            RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isOwn) {
                IconButton(onClick = { viewModel.speakChatMessage(message) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "TTS synthesize trigger",
                        tint = if (isPlaying) SparkAccent else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = roundedCorners,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        color = TextLight,
                        fontSize = 15.sp
                    )

                    if (message.isGroundedWithMaps) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Maps icon", tint = SparkAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Grounded via Google Maps", color = SparkAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sdf.format(Date(message.timestamp)),
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            if (isOwn) {
                IconButton(onClick = { viewModel.speakChatMessage(message) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "TTS voice speaking self",
                        tint = if (isPlaying) SparkAccent else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PREMIUM AI UNIVERSE TOOLS PORTAL
// -------------------------------------------------------------

@Composable
fun AiUniverseTabScreen(viewModel: ChatVerseViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    var promptImageQuery by remember { mutableStateOf("") }
    val generatedBmp by viewModel.generatedImage.collectAsState()
    val isGenImg by viewModel.isGeneratingImage.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Writing assist fields
    var originalText by remember { mutableStateOf("") }
    var adjustMode by remember { mutableStateOf("Grammar Correction") }
    var rewrittenOutput by remember { mutableStateOf("") }
    var runningRewriteTask by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Universe Portal", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextLight)
        Text("Dynamic AI generator & text modifier hubs", color = TextMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Badge locks check
        if (!userProfile.isPremium) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium Lock", tint = SparkAccent, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Unlock Premium Lifelong", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextLight)
                    Text(
                        text = "To access advanced image generation, text to speech synthesis, and live maps API grounding.",
                        textAlign = TextAlign.Center,
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.initiateCashfreeCheckout() },
                        colors = ButtonDefaults.buttonColors(containerColor = SparkAccent)
                    ) {
                        Text("Pay ₹10 One-Time Lifetime", color = MidnightBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // 1. AI sticker / Image tool using gemini-3.1-flash-image-preview
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Magic generate art inline", tint = Purple80)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Stickers & Custom Mock Art", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight)
                    }
                    Text("Powered by gemini-3.1-flash-image-preview", color = TextMuted, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = promptImageQuery,
                        onValueChange = { promptImageQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("A retro cyber lotus flower sticker vector...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generatePremiumIllustration(promptImageQuery) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple80),
                        enabled = !isGenImg
                    ) {
                        if (isGenImg) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextLight)
                        } else {
                            Text("Generate AI Sticker")
                        }
                    }

                    if (generatedBmp != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            bitmap = generatedBmp!!.asImageBitmap(),
                            contentDescription = "Generated artwork",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Writing Assistant
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = "Tonal helper", tint = Pink80)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Smart Writing Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight)
                    }
                    Text("Modify grammar, adjust voice tone, or rewrite formally", color = TextMuted, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = originalText,
                        onValueChange = { originalText = it },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        placeholder = { Text("Enter message draft to process...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode chips Selection
                    LazyRow {
                        val modes = listOf("Grammar Correction", "Professional Tone", "Sarcastic Dialect", "Bulgarian Translation")
                        items(modes) { mode ->
                            Card(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { adjustMode = mode },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (adjustMode == mode) ChatBubbleSelf else MidnightBg
                                )
                            ) {
                                Text(mode, color = TextLight, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            runningRewriteTask = true
                            scope.launch {
                                // Dynamic text rewrite simulations using gemini-3.5-flash
                                val processed = "Your processed text updated with: $adjustMode -> $originalText"
                                delay(1200)
                                rewrittenOutput = processed
                                runningRewriteTask = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Pink80),
                        enabled = !runningRewriteTask
                    ) {
                        if (runningRewriteTask) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextLight)
                        } else {
                            Text("Perform Rewrite Transformation")
                        }
                    }

                    if (rewrittenOutput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MidnightBg, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(rewrittenOutput, color = TextLight, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SECURE ADMIN METRICS TAB WINDOW
// -------------------------------------------------------------

@Composable
fun MetricsTabScreen(viewModel: ChatVerseViewModel) {
    val txs by viewModel.transactions.collectAsState()
    val logs by viewModel.aiLogs.collectAsState()

    var premiumSum = txs.size * 10
    var apiRequestsSum = logs.size
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Admin Hub Statistics", fontWeight = FontWeight.Bold, fontSize = 23.sp, color = TextLight)
        Text("Real-time revenue monitoring and cost logs reports", color = TextMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Total sales
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Premium Revenue", color = TextMuted, fontSize = 12.sp)
                    Text("₹$premiumSum.00", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Pink80)
                    Text("Lifetime ₹10 package sales", color = TextMuted, fontSize = 10.sp)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total AI Requests", color = TextMuted, fontSize = 12.sp)
                    Text(apiRequestsSum.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Purple80)
                    Text("Active API calls records", color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time operations logs
        Text("API Cost & Request Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (logs.isEmpty()) {
                    Text("No AI operations logs registered yet. Type questions to the bot to create reports.", color = TextMuted, fontSize = 13.sp)
                } else {
                    logs.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(log.prompt, color = TextLight, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(log.responseType, color = SparkAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cost: ₹${log.costInRupees}", color = Pink80, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Tokens: ${log.tokenUsed}", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        HorizontalDivider(color = MidnightBg)
                    }
                }
            }
        }
    }
}
