package com.example.nostalgianest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpecificGalleryActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 71;

    // UI Elements
    private Button uploadImageButton, leaveGalleryButton, deleteGalleryButton, editGalleryButton;
    private TextView galleryNameTextView, galleryDescriptionTextView, galleryUIDTextView;
    private RecyclerView membersRecyclerView;
    private GridView galleryGridView;
    private ProgressBar progressBar;

    // Firebase and data
    private String currentGalleryUid;
    private List<String> imageUrls, imageIds; // Image URLs and IDs from Firebase
    private GalleryImageAdapter imageAdapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_gallery);

        // Initialize UI elements
        uploadImageButton = findViewById(R.id.uploadImageButton);
        leaveGalleryButton = findViewById(R.id.leaveGalleryButton);
        deleteGalleryButton = findViewById(R.id.deleteGalleryButton);
        editGalleryButton = findViewById(R.id.editGalleryButton);
        galleryNameTextView = findViewById(R.id.galleryNameTextView);
        galleryDescriptionTextView = findViewById(R.id.galleryDescriptionTextView);
        galleryUIDTextView = findViewById(R.id.galleryUIDTextView);
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        galleryGridView = findViewById(R.id.galleryGridView);
        // Initialize the ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Initialize data lists
        imageUrls = new ArrayList<>();
        imageIds = new ArrayList<>();

        // Get gallery UID from the previous activity
        currentGalleryUid = getIntent().getStringExtra("GALLERY_UID");

        // Set up the adapter
        imageAdapter = new GalleryImageAdapter(this, imageUrls, imageIds, currentGalleryUid);
        galleryGridView.setAdapter(imageAdapter);

        // Fetch gallery details
        if (currentGalleryUid != null) {
            fetchGalleryDetails(currentGalleryUid);
        } else {
            Toast.makeText(this, "Gallery UID is missing.", Toast.LENGTH_SHORT).show();
        }

        // Set up RecyclerView for members
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up button click listeners
        uploadImageButton.setOnClickListener(v -> openFileChooser());
        leaveGalleryButton.setOnClickListener(v -> leaveGallery());
        deleteGalleryButton.setOnClickListener(v -> deleteGallery());
        editGalleryButton.setOnClickListener(v -> toggleEditMode());
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode; // Toggle edit mode
        imageAdapter.setEditMode(isEditMode); // Notify the adapter
        editGalleryButton.setText(isEditMode ? "Done" : "Edit Gallery");
        Toast.makeText(this, isEditMode ? "Edit mode enabled" : "Edit mode disabled", Toast.LENGTH_SHORT).show();
    }

    private void fetchGalleryDetails(String galleryUid) {
        progressBar.setVisibility(View.VISIBLE); // Show the loading spinner

        DatabaseReference galleryRef = FirebaseDatabase.getInstance().getReference("albums").child(galleryUid);

        galleryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE); // Hide the loading spinner

                if (dataSnapshot.exists()) {
                    String ownerId = dataSnapshot.child("owner").getValue(String.class);
                    String galleryName = dataSnapshot.child("albumName").getValue(String.class);
                    String galleryDescription = dataSnapshot.child("description").getValue(String.class);

                    // Update UI with gallery details
                    galleryNameTextView.setText(galleryName);
                    galleryDescriptionTextView.setText(galleryDescription);
                    galleryUIDTextView.setText("UID: " + galleryUid);

                    // Fetch images
                    imageUrls.clear();
                    imageIds.clear();
                    for (DataSnapshot imageSnapshot : dataSnapshot.child("images").getChildren()) {
                        String imageId = imageSnapshot.getKey();
                        String imageUrl = imageSnapshot.getValue(String.class);
                        if (imageId != null && imageUrl != null) {
                            imageIds.add(imageId);
                            imageUrls.add(imageUrl);
                        }
                    }
                    imageAdapter.notifyDataSetChanged();

                    // Fetch members
                    List<String> memberList = new ArrayList<>();
                    for (DataSnapshot memberSnapshot : dataSnapshot.child("members").getChildren()) {
                        memberList.add(memberSnapshot.getKey());
                    }

                    // Set up member list
                    String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    if (currentUserUid != null && currentUserUid.equals(ownerId)) {
                        deleteGalleryButton.setVisibility(View.VISIBLE);
                        leaveGalleryButton.setVisibility(View.GONE);
                    } else {
                        deleteGalleryButton.setVisibility(View.GONE);
                        leaveGalleryButton.setVisibility(View.VISIBLE);
                    }

                    MemberAdapter memberAdapter = new MemberAdapter(SpecificGalleryActivity.this, memberList, galleryUid, ownerId, currentUserUid);
                    membersRecyclerView.setAdapter(memberAdapter);
                } else {
                    Toast.makeText(SpecificGalleryActivity.this, "Gallery not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE); // Hide the spinner if there's an error
                Toast.makeText(SpecificGalleryActivity.this, "Failed to load gallery data.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                String base64Image = imageToBase64(imageUri);
                uploadImageToDatabase(base64Image);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String imageToBase64(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    private void uploadImageToDatabase(String base64Image) {
        DatabaseReference imagesRef = FirebaseDatabase.getInstance().getReference("albums").child(currentGalleryUid).child("images");
        String imageId = imagesRef.push().getKey();
        if (imageId != null) {
            imagesRef.child(imageId).setValue(base64Image).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(SpecificGalleryActivity.this, "Image uploaded successfully.", Toast.LENGTH_SHORT).show();
                    fetchGalleryDetails(currentGalleryUid);
                } else {
                    Toast.makeText(SpecificGalleryActivity.this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Failed to generate image ID.", Toast.LENGTH_SHORT).show();
        }
    }

    private void leaveGallery() {
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference galleryRef = FirebaseDatabase.getInstance().getReference("albums").child(currentGalleryUid);
        galleryRef.child("members").child(currentUserUid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Left the gallery.", Toast.LENGTH_SHORT).show();

                // Navigate to GalleryActivity
                Intent intent = new Intent(SpecificGalleryActivity.this, GalleryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear the back stack
                startActivity(intent);
                finish(); // Close the current activity
            } else {
                Toast.makeText(this, "Failed to leave the gallery.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void deleteGallery() {
        DatabaseReference galleryRef = FirebaseDatabase.getInstance().getReference("albums").child(currentGalleryUid);
        galleryRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Gallery deleted successfully.", Toast.LENGTH_SHORT).show();
                finish(); // Close activity
            } else {
                Toast.makeText(this, "Failed to delete gallery.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
