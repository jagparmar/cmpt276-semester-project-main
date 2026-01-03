package com.example.nostalgianest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SendMemoriesActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_CODE = 101;

    private ImageView selectedImageView;
    private Button selectImageButton, uploadImageButton;
    private Spinner gallerySpinner; // Spinner to select a gallery
    private TextView selectedGalleryTextView;

    private Bitmap selectedImageBitmap;
    private String selectedAlbumUID; // The UID of the selected album
    private DatabaseReference firebaseDatabase;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_memories); // Replace with your actual layout name

        // Initialize views
        selectedImageView = findViewById(R.id.selectedImageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        gallerySpinner = findViewById(R.id.gallerySpinner); // Initialize Spinner
        selectedGalleryTextView = findViewById(R.id.selectedGalleryTextView); // Initialize TextView

        // Initialize Firebase reference
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("albums");

        selectImageButton.setOnClickListener(v -> openImagePicker());
        uploadImageButton.setOnClickListener(v -> uploadImageToAlbum());

        loadAlbumsIntoSpinner();
    }

    private void loadAlbumsIntoSpinner() {
        // Get the current authenticated user
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch albums from Firebase
        firebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> albumNames = new ArrayList<>();
                List<String> albumUids = new ArrayList<>(); // To store corresponding album UIDs

                for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                    // Check if the current user is a member of this album
                    if (albumSnapshot.child("members").hasChild(currentUserUid)) {
                        String albumName = albumSnapshot.child("albumName").getValue(String.class);
                        albumNames.add(albumName);
                        albumUids.add(albumSnapshot.getKey()); // Store the UID of the album
                    }
                }

                // Check if the user has any albums
                if (albumNames.isEmpty()) {
                    Toast.makeText(SendMemoriesActivity.this, "You are not a member of any albums.", Toast.LENGTH_SHORT).show();
                } else {
                    // Set up the spinner adapter with the list of album names
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(SendMemoriesActivity.this,
                            android.R.layout.simple_spinner_item, albumNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    gallerySpinner.setAdapter(adapter);

                    // Set an item selected listener to update the selected album UID
                    gallerySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            selectedAlbumUID = albumUids.get(position); // Get the UID of the selected album
                            String selectedAlbumName = albumNames.get(position);
                            selectedGalleryTextView.setText("Selected Gallery: " + selectedAlbumName);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            selectedGalleryTextView.setText("No gallery selected");
                            selectedAlbumUID = null;
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SendMemoriesActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void findAlbumByName(String albumName) {
        firebaseDatabase.orderByChild("albumName").equalTo(albumName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Assuming album names are unique
                    for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                        selectedAlbumUID = albumSnapshot.getKey(); // Get the UID of the album
                        selectedGalleryTextView.setText("Selected Gallery: " + albumName); // Update the TextView
                        Toast.makeText(SendMemoriesActivity.this, "Album found: " + albumName, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(SendMemoriesActivity.this, "Album not found.", Toast.LENGTH_SHORT).show();
                    selectedAlbumUID = null;
                    selectedGalleryTextView.setText("No gallery selected"); // Reset TextView if not found
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SendMemoriesActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // Convert URI to Bitmap and display it
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                selectedImageView.setImageBitmap(selectedImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageToAlbum() {
        if (selectedImageBitmap == null) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAlbumUID == null) {
            Toast.makeText(this, "Please select a valid album first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert Bitmap to Base64 string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        // Reference the specific album's images node
        DatabaseReference imagesRef = firebaseDatabase.child(selectedAlbumUID).child("images");
        String imageKey = imagesRef.push().getKey();

        if (imageKey != null) {
            imagesRef.child(imageKey).setValue(base64Image)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Start the animation when the upload is successful
                            animateImageUpload();
                        } else {
                            Toast.makeText(this, "Image upload failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Failed to generate image key.", Toast.LENGTH_SHORT).show();
        }
    }

    private void animateImageUpload() {
        // Animate the image view to slide up
        selectedImageView.animate()
                .translationY(-500)  // Adjust based on your layout to slide up (e.g., move it 500 pixels up)
                .alpha(0f)           // Fade it out
                .setDuration(800)    // Duration of the animation (800ms)
                .withEndAction(() -> {
                    // Once the animation is done, reset the image and bring it back to the original position
                    selectedImageBitmap = null;  // Clear the selected image bitmap
                    selectedImageView.setImageResource(0);  // Clear the displayed image
                    selectedImageView.setImageResource(R.drawable.image_placeholder); // Set a placeholder image

                    // Bring back the ImageView to its original position and make it visible again
                    selectedImageView.animate()
                            .translationY(0)  // Return to the original position
                            .alpha(1f)        // Fade it back in
                            .setDuration(500) // Duration of the return animation (500ms)
                            .start();

                    Toast.makeText(this, "Image uploaded successfully to album!", Toast.LENGTH_SHORT).show();
                })
                .start();
    }

}