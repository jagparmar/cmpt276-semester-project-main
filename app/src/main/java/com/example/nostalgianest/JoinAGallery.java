package com.example.nostalgianest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;

public class JoinAGallery extends AppCompatActivity {

    private EditText uidInput;
    private EditText passcodeInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_agallery);

        // Initialize UI elements
        uidInput = findViewById(R.id.editTextNumberSigned2);
        passcodeInput = findViewById(R.id.passcodeInput);
    }

    // Validate and join gallery method
    private void validateAndJoinGallery(final String albumId, final String passcode) {
        // Reference to the album in the database
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId);

        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve the stored passcode
                    String correctPasscode = dataSnapshot.child("passcode").getValue(String.class);

                    if (correctPasscode == null) {
                        Toast.makeText(JoinAGallery.this, "No passcode set for this gallery.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (correctPasscode.equals(passcode)) {
                        // Passcode is correct, proceed to join the gallery
                        joinGallery(albumId);
                    } else {
                        Toast.makeText(JoinAGallery.this, "Invalid passcode. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(JoinAGallery.this, "Gallery with this ID doesn't exist.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Error occurred: " + databaseError.getMessage());
                Toast.makeText(JoinAGallery.this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Join the gallery by adding the current user to the members list
    private void joinGallery(String albumId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            DatabaseReference membersRef = FirebaseDatabase.getInstance()
                    .getReference("albums")
                    .child(albumId)
                    .child("members")
                    .child(currentUserUid);

            membersRef.setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(JoinAGallery.this, "Successfully joined the gallery!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the join activity or redirect to the gallery
                } else {
                    Toast.makeText(JoinAGallery.this, "Failed to join the gallery. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "You need to log in first.", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle the button click for joining a gallery
    public void joinButtonClicked(View view) {
        try {
            String albumId = uidInput.getText().toString().trim();
            String passcode = passcodeInput.getText().toString().trim();

            if (albumId.isEmpty() || passcode.isEmpty()) {
                Toast.makeText(this, "Please enter both album ID and passcode.", Toast.LENGTH_SHORT).show();
                return;
            }

            validateAndJoinGallery(albumId, passcode);

        } catch (Exception e) {
            Log.e("Error", "Unexpected error occurred: " + e.getMessage());
            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
