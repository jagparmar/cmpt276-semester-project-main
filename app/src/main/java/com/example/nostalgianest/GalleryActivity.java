package com.example.nostalgianest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;  // Add this import for View
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;
    private TextView noGalleryMessage;  // Add TextView for no galleries message
    private ProgressBar loadingProgressBar;  // Add ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Initialize RecyclerView, ProgressBar, and TextView for no galleries
        recyclerView = findViewById(R.id.galleryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadingProgressBar = findViewById(R.id.loadingProgressBar);  // Link to ProgressBar
        noGalleryMessage = findViewById(R.id.noGalleryMessage);  // Link to TextView

        // Load galleries for the current user
        loadGalleries();
    }

    private void loadGalleries() {
        // Get the current authenticated user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated. Redirecting to login...", Toast.LENGTH_SHORT).show();
            // Redirect to login activity
            startActivity(new Intent(GalleryActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String currentUserUid = currentUser.getUid();
        DatabaseReference albumsRef = FirebaseDatabase.getInstance().getReference("albums");

        // Show ProgressBar while fetching data
        loadingProgressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noGalleryMessage.setVisibility(View.GONE);

        albumsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<albumObject> galleryList = new ArrayList<>();

                // Iterate through albums
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Check if the current user is a member of the album
                    if (snapshot.child("members").hasChild(currentUserUid)) {
                        albumObject album = snapshot.getValue(albumObject.class);
                        if (album != null && snapshot.getKey() != null) {
                            album.setUid(snapshot.getKey()); // Set the unique ID for the album
                            galleryList.add(album);
                        }
                    }
                }

                // Hide ProgressBar and display results
                loadingProgressBar.setVisibility(View.GONE);

                // Check and display the galleries
                if (!galleryList.isEmpty()) {
                    // Hide the "No galleries" message and show the RecyclerView
                    noGalleryMessage.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if (galleryAdapter == null) {
                        galleryAdapter = new GalleryAdapter(galleryList, GalleryActivity.this::openGallery);
                        recyclerView.setAdapter(galleryAdapter);
                    } else {
                        galleryAdapter.updateData(galleryList);
                    }
                } else {
                    // Show the "No galleries" message and hide the RecyclerView
                    noGalleryMessage.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(GalleryActivity.this, "No galleries available for your account.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loadingProgressBar.setVisibility(View.GONE);  // Hide ProgressBar
                Toast.makeText(GalleryActivity.this, "Error fetching galleries: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void openGallery(String galleryUid) {
        // Navigate to SpecificGalleryActivity
        Intent intent = new Intent(this, SpecificGalleryActivity.class);
        intent.putExtra("GALLERY_UID", galleryUid);
        startActivity(intent);
    }
}
