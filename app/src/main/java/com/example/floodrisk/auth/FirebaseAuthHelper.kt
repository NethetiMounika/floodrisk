package com.example.floodrisk.auth

import android.content.Context
import com.example.floodrisk.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseAuthHelper(private val context: Context) {

    val isFirebaseAvailable: Boolean
        get() {
            return try {
                FirebaseApp.getApps(context).isNotEmpty() && FirebaseAuth.getInstance() != null
            } catch (e: Exception) {
                false
            }
        }

    fun getCurrentUser(): UserProfile? {
        if (!isFirebaseAvailable) return getSavedLocalUser()
        val firebaseUser: FirebaseUser? = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
        return firebaseUser?.toDomainProfile() ?: getSavedLocalUser()
    }

    fun signOut() {
        if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        clearSavedLocalUser()
    }

    fun saveLocalSession(displayName: String, email: String, photoUrl: String? = null) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("display_name", displayName)
            .putString("email", email)
            .putString("photo_url", photoUrl ?: "")
            .apply()
    }

    private fun getSavedLocalUser(): UserProfile? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return null
        return UserProfile(
            uid = "local_user",
            displayName = prefs.getString("display_name", "User") ?: "User",
            email = prefs.getString("email", "user@example.com") ?: "user@example.com",
            photoUrl = prefs.getString("photo_url", null)?.takeIf { it.isNotEmpty() }
        )
    }

    private fun clearSavedLocalUser() {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun FirebaseUser.toDomainProfile(): UserProfile {
        return UserProfile(
            uid = uid,
            displayName = displayName ?: "User",
            email = email ?: "",
            photoUrl = photoUrl?.toString()
        )
    }
}
