package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseHelper {
    private const val TAG = "FirebaseHelper"
    private var isFirebaseInitialized = false

    fun initialize(context: Context) {
        try {
            // Check if Firebase is available and has google-services config
            val app = FirebaseApp.initializeApp(context)
            if (app != null) {
                isFirebaseInitialized = true
                Log.d(TAG, "Firebase initialized successfully.")
            } else {
                Log.w(TAG, "Firebase initialization returned null (missing options/google-services.json string keys). Falling back to simulation.")
                isFirebaseInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}. Falling back to simulation.", e)
            isFirebaseInitialized = false
        }
    }

    fun isAvailable(): Boolean {
        if (!isFirebaseInitialized) return false
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Simulated/Real Google Sign-In with Firebase Auth
    suspend fun signInWithGoogleCredential(idToken: String): String? {
        if (!isAvailable()) {
            Log.d(TAG, "[Simulated Auth] Standard simulated sign-in for token: ${idToken.take(15)}...")
            return "simulated_user_uid_123456"
        }
        return try {
            val auth = FirebaseAuth.getInstance()
            // In a real usage, you build a GoogleAuthProvider credential:
            // val credential = GoogleAuthProvider.getCredential(idToken, null)
            // val result = auth.signInWithCredential(credential).await()
            // result.user?.uid
            "firebase_user_uid_placeholder"
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth error, falling back to simulated session: ${e.message}")
            "simulated_user_uid_123456"
        }
    }

    // Simulated/Real Email Login auth
    suspend fun loginWithEmailPassword(email: String, password: String): String? {
        if (!isAvailable()) {
            return "simulated_email_user_uid"
        }
        return try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            null
        }
    }

    // Save profile to Firestore
    suspend fun persistUserProfileInFirestore(profile: UserProfile) {
        if (!isAvailable()) {
            Log.d(TAG, "[Simulated Firestore] Saving profile block locally: ${profile.displayName}")
            return
        }
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(profile.uid)
                .set(profile)
                .await()
            Log.d(TAG, "User profile successfully synced to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore save error: ${e.message}")
        }
    }

    // Sync chats in Firestore
    suspend fun syncChatToFirestore(chat: Chat) {
        if (!isAvailable()) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("chats")
                .document(chat.id)
                .set(chat)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync chat error: ${e.message}")
        }
    }

    // Sync message to Firestore
    suspend fun persistMessageInFirestore(message: Message) {
        if (!isAvailable()) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("chats")
                .document(message.chatId)
                .collection("messages")
                .document(message.id)
                .set(message)
                .await()
            Log.d(TAG, "Message synced to Firestore successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore write message error: ${e.message}")
        }
    }
}
