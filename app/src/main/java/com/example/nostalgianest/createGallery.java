package com.example.nostalgianest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import java.util.UUID;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class createGallery extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_gallery);

        // Set padding for system bars (status bar, navigation bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void createAnAlbum(View view) {
        EditText inputtedDescription = findViewById(R.id.DescriptionInput);
        EditText albumName = findViewById(R.id.AlbumNameInput);
        EditText passcode = findViewById(R.id.createGalleryPasscodeInput);

        if (albumName.getText().toString().trim().isEmpty()) {
            showToast("Album name is required.");
            return;
        }

        if (passcode.getText().toString().trim().length() <= 7) {
            showToast("Passcode must be longer than 7 characters.");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            showToast("You must be logged in to create an album.");
            return;
        }

        String userUid = auth.getCurrentUser().getUid();
        Log.d("FirebaseAuth", "Current user UID: " + userUid);

        // Generate a simple UID for the album
        String simpleAlbumUid = UUID.randomUUID().toString().substring(0, 8); // Shorter UID

        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(simpleAlbumUid);
        albumObject newAlbum = new albumObject(
                simpleAlbumUid,
                passcode.getText().toString().trim(),
                inputtedDescription.getText().toString().trim(),
                albumName.getText().toString().trim()
        );
        newAlbum.setOwner(userUid);
        newAlbum.addMember(userUid);

        storeAlbumInFirebase(newAlbum, albumRef);
    }

    // Save the album to Firebase
    private void storeAlbumInFirebase(albumObject newAlbum, DatabaseReference albumRef) {
        albumRef.setValue(newAlbum)
                .addOnSuccessListener(aVoid -> {
                    showToast("Gallery successfully created!");
                    Intent intent = new Intent(this, MemoriesActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to create gallery: " + e.getMessage());
                });
    }

    // Helper to show Toast messages
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
