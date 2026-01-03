package com.example.nostalgianest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SwiperGame extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiper_game);

        // Initialize the ProgressBar
        progressBar = findViewById(R.id.progressBar);
    }

    public void startSwiperGame(View v) {
        // Show progress bar before starting the task
        progressBar.setVisibility(View.VISIBLE);

        // Check if the user has any albums with images
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        checkAlbum();
    }

    private void checkAlbum() {
        // Get the current authenticated user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            // Hide progress bar if user is not authenticated
            progressBar.setVisibility(View.GONE);
            return;
        }

        String currentUserUid = currentUser.getUid();
        DatabaseReference albumsRef = FirebaseDatabase.getInstance().getReference("albums");

        albumsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean albumFound = false;
                boolean hasImages = false;

                // Iterate through the albums to check if the user is part of any album with images
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("members").hasChild(currentUserUid)) {
                        albumFound = true;

                        // Check if the album has images
                        if (snapshot.child("images").exists() && snapshot.child("images").hasChildren()) {
                            hasImages = true;
                            break; // No need to check further, we found an album with images
                        }
                    }
                }

                // Based on the checks, start the game or show a toast
                if (albumFound && hasImages) {
                    // Hide progress bar before starting the next activity
                    progressBar.setVisibility(View.GONE);

                    Intent intent = new Intent(SwiperGame.this, SwiperGameStart.class);
                    startActivity(intent);
                } else if (albumFound) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SwiperGame.this, "The album has no images to display.", Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SwiperGame.this, "You currently have no albums.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SwiperGame.this, "Error fetching album data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
