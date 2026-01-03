package com.example.nostalgianest;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SwiperGameStart extends AppCompatActivity implements View.OnTouchListener {

    private ImageView imageView;
    private TextView rememberedCountTextView;

    private float initialX;
    private List<String> rememberedUriImages;
    private List<String> imageList;
    private int currentImageIndex = 0;

    private DatabaseReference databaseReference;
    private String userId; // Current authenticated user's UID
    private String selectedAlbum;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiper_game_start);

        progressBar = findViewById(R.id.progressBar);

        imageView = findViewById(R.id.SwiperGameImageDisplay);
        rememberedCountTextView = findViewById(R.id.rememberedNumber);

        rememberedUriImages = new ArrayList<>();
        imageList = new ArrayList<>();

        // Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("albums");

        // Get the current authenticated user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish(); // End the activity if no user is authenticated
            return;
        }

        // Prompt album selection
        promptAlbumSelection();

        // Set touch listener for the ImageView
        imageView.setOnTouchListener(this);
    }

    private void promptAlbumSelection() {
        showProgressBar();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> albumNames = new ArrayList<>();
                albumNames.add("All Albums");

                for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot membersSnapshot = albumSnapshot.child("members");

                    // Check if the user is a member of the album
                    if (membersSnapshot.hasChild(userId)) {
                        String albumName = albumSnapshot.child("albumName").getValue(String.class);
                        if (albumName != null) {
                            albumNames.add(albumName); // Add the album name if the user is a member
                        }
                    }
                }

                if (albumNames.size() == 1) {
                    Toast.makeText(SwiperGameStart.this, "You are not a member of any albums yet.", Toast.LENGTH_SHORT).show();
                }
                hideProgressBar();
                showAlbumSelectionDialog(albumNames);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SwiperGameStart.this, "Failed to load albums: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                hideProgressBar();
            }
        });
    }

    private void showAlbumSelectionDialog(List<String> albumNames) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an Album");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, albumNames);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        builder.setView(spinner);

        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedAlbum = spinner.getSelectedItem().toString();
            loadImagesFromFirebase(selectedAlbum);
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void loadImagesFromFirebase(String albumName) {
        showProgressBar();// Show progress bar while loading images

        if (albumName.equals("All Albums")) {
            imageList.clear();
            loadImagesFromAllAlbums();
        } else {
            databaseReference.orderByChild("albumName").equalTo(albumName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                            String albumUid = albumSnapshot.getKey();
                            loadImagesFromAlbum(albumUid);
                        }
                    } else {
                        Toast.makeText(SwiperGameStart.this, "Album not found.", Toast.LENGTH_SHORT).show();
                    }

                    progressBar.setVisibility(View.GONE); // Hide progress bar after loading images
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(SwiperGameStart.this, "Failed to load images: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE); // Hide progress bar on error
                }
            });
        }

    }

    private void loadImagesFromAllAlbums() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot membersSnapshot = albumSnapshot.child("members");
                    if (membersSnapshot.hasChild(userId)) {
                        String albumUid = albumSnapshot.getKey();
                        loadImagesFromAlbum(albumUid);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SwiperGameStart.this, "Failed to load images: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImagesFromAlbum(String albumUid) {
        databaseReference.child(albumUid).child("images").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot imageSnapshot : dataSnapshot.getChildren()) {
                    String base64Image = imageSnapshot.getValue(String.class);
                    if (base64Image != null) {
                        imageList.add(base64Image);
                    }
                }

                if (!imageList.isEmpty() && currentImageIndex == 0) {
                    displayImage(currentImageIndex);
                } else if (imageList.isEmpty()) {
                    Toast.makeText(SwiperGameStart.this, "No images available.", Toast.LENGTH_SHORT).show();
                }

                hideProgressBar(); // Hide progress bar after loading images
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SwiperGameStart.this, "Failed to load images: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                hideProgressBar(); // Hide progress bar on error
            }
        });
    }

    private void displayImage(int index) {
        if (index >= 0 && index < imageList.size()) {
            String base64Image = imageList.get(index);
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            imageView.setImageBitmap(bitmap);
            animateFadeIn(imageView); // Animate the image fading in
        }
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private Bitmap decodeBase64ToBitmap(String base64Image) {
        byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                return true;

            case MotionEvent.ACTION_UP:
                float finalX = event.getX();
                float displacement = finalX - initialX;

                if (displacement > 0) {
                    handleSwipeRight();
                } else if (displacement < 0) {
                    handleSwipeLeft();
                }
                return true;

            default:
                return false;
        }
    }

    private void handleSwipeRight() {
        if (currentImageIndex < imageList.size()) {
            rememberedUriImages.add(imageList.get(currentImageIndex));
            updateRememberedCount();
            Toast.makeText(this, "Image remembered!", Toast.LENGTH_SHORT).show();
        }
        animateSwipe(imageView, 1000, this::showNextImage); // Wait for animation to complete
    }

    private void handleSwipeLeft() {
        Toast.makeText(this, "Image skipped!", Toast.LENGTH_SHORT).show();
        animateSwipe(imageView, -1000, this::showNextImage); // Wait for animation to complete
    }


    private void showNextImage() {
        currentImageIndex++;
        if (currentImageIndex < imageList.size()) {
            displayImage(currentImageIndex);
        } else {
            showSummaryDialog();
        }
    }

    private void showSummaryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over");
        builder.setMessage("You have finished swiping all the images. You remembered " + rememberedUriImages.size() + " images.");

        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(SwiperGameStart.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void updateRememberedCount() {
        rememberedCountTextView.setText("Remembered: " + rememberedUriImages.size());
    }

    private void animateSwipe(View view, float endX, Runnable onAnimationEnd) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0f, endX);
        animator.setDuration(300); // Adjust duration as needed
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setTranslationX(0); // Reset position for the next image
                if (onAnimationEnd != null) {
                    onAnimationEnd.run(); // Trigger the next action
                }
            }
        });
        animator.start();
    }


    private void animateFadeIn(View view) {
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(300).start();
    }
}
