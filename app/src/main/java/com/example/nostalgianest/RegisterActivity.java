package com.example.nostalgianest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private EditText name;  // Add the name EditText
    private Button register;
    private FirebaseAuth auth;
    private DatabaseReference database;  // Reference to Realtime Database
    private final long [] patternError = {0,300,100,300};  //delay,vibrate,pause,vibration, pattern used in error, heavy long vibrations convey error
    private final long [] patternSuccess = {0,100, 50, 100, 50, 200}; //short pulses with gradual increase convey completion and success
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");  // Reference to "Users" in Firebase

        // Initialize UI components
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        name = findViewById(R.id.name);  // Initialize name EditText
        register = findViewById(R.id.register);
        //Used to interact with devices vibrator hardware
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        // Register button click listener
        register.setOnClickListener(v -> {
            String txt_email = email.getText().toString().trim();
            String txt_password = password.getText().toString().trim();
            String txt_name = name.getText().toString().trim();  // Get name input

            // Check if fields are empty
            if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password) || TextUtils.isEmpty(txt_name)) {
                Toast.makeText(RegisterActivity.this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
                if(vibrator!= null && vibrator.hasVibrator()){
                    vibrator.vibrate(patternError, -1);
                }
            } else if (txt_password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                if(vibrator!= null && vibrator.hasVibrator()){
                    vibrator.vibrate(patternError, -1);
                }
            } else {
                registerUser(txt_email, txt_password, txt_name);  // Pass name to register method
            }
        });
    }

    private void registerUser(String email, String password, String name) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Get the current user's ID
                String userId = auth.getCurrentUser().getUid();

                // Create a User object to hold the data
                User user = new User(name, email);

                // Save the user information to the Firebase Realtime Database
                database.child(userId).setValue(user).addOnCompleteListener(databaseTask -> {
                    if (databaseTask.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        if(vibrator!= null && vibrator.hasVibrator()){
                            vibrator.vibrate(patternSuccess, -1);
                        }
                        // Navigate to LoginActivity
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();  // End the current activity
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                        if(vibrator!= null && vibrator.hasVibrator()){
                            vibrator.vibrate(patternError, -1);
                        }
                    }
                });
            } else {
                // Show specific error message if available
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed!";
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // User class to represent the user data
    public static class User {
        private String name;
        private String email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
