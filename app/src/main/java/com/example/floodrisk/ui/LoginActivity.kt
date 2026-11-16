package com.example.floodrisk.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.floodrisk.auth.FirebaseAuthHelper
import com.example.floodrisk.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authHelper: FirebaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = FirebaseAuthHelper(this)

        // If user already logged in, proceed to MainActivity
        if (authHelper.getCurrentUser() != null) {
            navigateToMain()
            return
        }

        checkFirebaseStatus()
        setupClickListeners()
    }

    private fun checkFirebaseStatus() {
        if (!authHelper.isFirebaseAvailable) {
            binding.tvConfigExplanation.visibility = View.VISIBLE
            binding.tvConfigExplanation.text =
                "Note: Firebase google-services.json is missing or not configured. " +
                "Google Sign-In requires google-services.json. You can continue as Local User to test full functionality."
        } else {
            binding.tvConfigExplanation.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnGoogleSignIn.setOnClickListener {
            if (!authHelper.isFirebaseAvailable) {
                Toast.makeText(
                    this,
                    "Firebase configuration is missing. Add google-services.json to use Firebase Google Sign-In, or click 'Continue as Local User'.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            authHelper.saveLocalSession("Google User", "google.user@example.com")
            navigateToMain()
        }

        binding.btnGuestMode.setOnClickListener {
            authHelper.saveLocalSession("River Watcher", "watcher@floodrisk.app")
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
