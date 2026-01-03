package com.example.nostalgianest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends ComponentActivity {

    private ProgressDialog progressDialog;
    private final long [] patternError = {0,300,100,300};  //delay,vibrate,pause,vibration, pattern used in error, heavy long vibrations convey error
    private final long [] patternSuccess = {0,100, 50, 100, 50, 200}; //short pulses with gradual increase convey completion and success
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if the user is already logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        // Set padding to prevent UI elements being hidden by system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button login = findViewById(R.id.loginButton);
        Button createNewBtn = findViewById(R.id.createNew);
        //Used to interact with devices vibrator hardware
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Add a progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        // Set onClickListener for login button
        login.setOnClickListener(v -> {
            String txtEmail = email.getText().toString().trim();
            String txtPassword = password.getText().toString().trim();

            // Perform validation before login
            if (txtEmail.isEmpty() || txtPassword.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                if(vibrator!= null && vibrator.hasVibrator()){
                    vibrator.vibrate(patternError, -1);
                }
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(txtEmail).matches()) {
                Toast.makeText(LoginActivity.this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                if(vibrator!= null && vibrator.hasVibrator()){
                    vibrator.vibrate(patternError, -1);
                }
                return;
            }

            // Check for network connection
            if (!isNetworkAvailable()) {
                Toast.makeText(LoginActivity.this, "No internet connection. Please try again.", Toast.LENGTH_SHORT).show();
                if(vibrator!= null && vibrator.hasVibrator()){
                    vibrator.vibrate(patternError, -1);
                }
                return;
            }

            // Show progress dialog and attempt login
            progressDialog.show();
            loginUser(txtEmail, txtPassword, auth);
        });

        // Set onClickListener for "Create New Account" button
        createNewBtn.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );



        // Toggle password visibility
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    // Function to log in user using Firebase Authentication
    private void loginUser(String email, String password, FirebaseAuth auth) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    if(vibrator!= null && vibrator.hasVibrator()){
                        vibrator.vibrate(patternSuccess, -1);
                    }
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    if (e instanceof FirebaseNetworkException) {
                        Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                        if(vibrator!= null && vibrator.hasVibrator()){
                            vibrator.vibrate(patternError, -1);
                        }
                    } else {
                        Toast.makeText(this, "Login Failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                        if(vibrator!= null && vibrator.hasVibrator()){
                            vibrator.vibrate(patternError, -1);
                        }
                    }
                    Log.e("LoginError", "Login failed for email: " + email, e);
                });
    }


    // Function to check network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
