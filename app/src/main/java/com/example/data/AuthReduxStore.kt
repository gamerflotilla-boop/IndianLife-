package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// REDUX STATE CLASS representing state slice
data class AuthState(
    val isLoggedIn: Boolean = false,
    val loginId: String? = null,
    val token: String? = null,
    val sessionExpiry: Long? = null,
    val authType: String? = null, // "CREDENTIALS", "OTP", "GOOGLE_SSO"
    val loginError: String? = null,
    val isAuthenticating: Boolean = false,
    val userProfile: UserProfile? = null
)

// REDUX ACTIONS represent events changing authentication state
sealed class AuthAction {
    object LoginStart : AuthAction()
    data class LoginSuccess(
        val loginId: String,
        val token: String,
        val sessionExpiry: Long,
        val authType: String,
        val userProfile: UserProfile
    ) : AuthAction()
    data class LoginFailure(val errorMessage: String) : AuthAction()
    object ValidateSession : AuthAction()
    object Logout : AuthAction()
    data class UpdateToken(val newToken: String, val sessionExpiry: Long) : AuthAction()
}

// REDUX REDUCER (Pure State Transitions)
fun authReducer(state: AuthState, action: AuthAction): AuthState {
    return when (action) {
        is AuthAction.LoginStart -> state.copy(
            isAuthenticating = true,
            loginError = null
        )
        is AuthAction.LoginSuccess -> state.copy(
            isLoggedIn = true,
            loginId = action.loginId,
            token = action.token,
            sessionExpiry = action.sessionExpiry,
            authType = action.authType,
            isAuthenticating = false,
            loginError = null,
            userProfile = action.userProfile
        )
        is AuthAction.LoginFailure -> state.copy(
            isLoggedIn = false,
            loginId = null,
            token = null,
            sessionExpiry = null,
            authType = null,
            isAuthenticating = false,
            loginError = action.errorMessage,
            userProfile = null
        )
        is AuthAction.ValidateSession -> {
            val now = System.currentTimeMillis()
            val expired = state.sessionExpiry?.let { now > it } ?: true
            if (expired && state.isLoggedIn) {
                state.copy(
                    isLoggedIn = false,
                    token = null,
                    sessionExpiry = null,
                    loginError = "Session expired. Please log in again."
                )
            } else {
                state
            }
        }
        is AuthAction.Logout -> AuthState()
        is AuthAction.UpdateToken -> state.copy(
            token = action.newToken,
            sessionExpiry = action.sessionExpiry
        )
    }
}

// REDUX STORE for Secure Auth Slice Management
class AuthReduxStore(private val context: Context) {
    private val PREFS_NAME = "chatverse_auth_redux_prefs"
    private val KEY_TOKEN = "auth_redux_token"
    private val KEY_LOGIN_ID = "auth_redux_login_id"
    private val KEY_EXPIRY = "auth_redux_expiry"
    private val KEY_AUTH_TYPE = "auth_redux_auth_type"
    private val KEY_LOGGED_IN = "auth_redux_logged_in"

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // Run verification of credentials on app activation
        dispatch(AuthAction.ValidateSession)
    }

    private fun loadInitialState(): AuthState {
        val isLoggedIn = sharedPrefs.getBoolean(KEY_LOGGED_IN, false)
        val loginId = sharedPrefs.getString(KEY_LOGIN_ID, null)
        val token = sharedPrefs.getString(KEY_TOKEN, null)
        val sessionExpiry = sharedPrefs.getLong(KEY_EXPIRY, 0L).let { if (it == 0L) null else it }
        val authType = sharedPrefs.getString(KEY_AUTH_TYPE, null)

        val profUid = sharedPrefs.getString("prof_uid", null)
        val profUsername = sharedPrefs.getString("prof_username", null)
        val profDisplayName = sharedPrefs.getString("prof_display_name", null)
        val profEmail = sharedPrefs.getString("prof_email", null)
        val profPhone = sharedPrefs.getString("prof_phone", null)
        val profBio = sharedPrefs.getString("prof_bio", null)
        val profPremium = sharedPrefs.getBoolean("prof_premium", false)

        val restoredProfile = if (profUid != null && profUsername != null) {
            UserProfile(
                uid = profUid,
                username = profUsername,
                displayName = profDisplayName ?: "Guest",
                email = profEmail ?: "",
                mobileNumber = profPhone ?: "",
                bio = profBio ?: "",
                isPremium = profPremium
            )
        } else {
            null
        }

        val now = System.currentTimeMillis()
        val isExpired = sessionExpiry?.let { now > it } ?: true

        return if (isLoggedIn && !isExpired && token != null) {
            AuthState(
                isLoggedIn = true,
                loginId = loginId,
                token = token,
                sessionExpiry = sessionExpiry,
                authType = authType,
                userProfile = restoredProfile
            )
        } else {
            AuthState()
        }
    }

    fun dispatch(action: AuthAction) {
        val currentState = _state.value
        val newState = authReducer(currentState, action)
        _state.value = newState

        // Sync with persistent local directory storage
        persistState(newState)
        Log.d("AuthReduxStore", "Dispatched Action: ${action::class.simpleName} => New LoggedIn State: ${newState.isLoggedIn}")
    }

    private fun persistState(state: AuthState) {
        val editor = sharedPrefs.edit()
        if (state.isLoggedIn && state.token != null) {
            editor.putBoolean(KEY_LOGGED_IN, true)
            editor.putString(KEY_LOGIN_ID, state.loginId)
            editor.putString(KEY_TOKEN, state.token)
            editor.putLong(KEY_EXPIRY, state.sessionExpiry ?: 0L)
            editor.putString(KEY_AUTH_TYPE, state.authType)
            
            // Persist profile
            state.userProfile?.let { prof ->
                editor.putString("prof_uid", prof.uid)
                editor.putString("prof_username", prof.username)
                editor.putString("prof_display_name", prof.displayName)
                editor.putString("prof_email", prof.email)
                editor.putString("prof_phone", prof.mobileNumber)
                editor.putString("prof_bio", prof.bio)
                editor.putBoolean("prof_premium", prof.isPremium)
            }
        } else {
            editor.putBoolean(KEY_LOGGED_IN, false)
            editor.remove(KEY_LOGIN_ID)
            editor.remove(KEY_TOKEN)
            editor.remove(KEY_EXPIRY)
            editor.remove(KEY_AUTH_TYPE)
            editor.remove("prof_uid")
            editor.remove("prof_username")
            editor.remove("prof_display_name")
            editor.remove("prof_email")
            editor.remove("prof_phone")
            editor.remove("prof_bio")
            editor.remove("prof_premium")
        }
        editor.apply()
    }

    companion object {
        fun buildSimulatedJwt(loginId: String): String {
            val payload = "sec_jwt_payload.${UUID.randomUUID()}"
            return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.${payload}.signature_val"
        }
    }
}
