package com.example.nostalgianest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.Dialog;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class GalleryImageAdapter extends BaseAdapter {

    private Context context;
    private List<String> base64Images; // List of Base64 strings
    private List<String> imageIds; // List of corresponding image IDs from Firebase
    private String currentGalleryUid; // Gallery UID for Firebase reference
    private boolean isEditMode = false; // Flag to track edit mode

    public GalleryImageAdapter(Context context, List<String> base64Images, List<String> imageIds, String currentGalleryUid) {
        this.context = context;
        this.base64Images = base64Images;
        this.imageIds = imageIds;
        this.currentGalleryUid = currentGalleryUid;
    }

    public void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
        notifyDataSetChanged(); // Refresh the grid
    }

    @Override
    public int getCount() {
        return base64Images.size();
    }

    @Override
    public Object getItem(int position) {
        return base64Images.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_image_item, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.gridImageView);
            holder.deleteButton = convertView.findViewById(R.id.deleteButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String base64Image = base64Images.get(position);
        Bitmap bitmap = decodeBase64(base64Image);

        if (bitmap != null) {
            holder.imageView.setImageBitmap(bitmap);
        } else {
            holder.imageView.setImageResource(R.drawable.error_placeholder); // Fallback image
        }

        // Set an OnClickListener to show the image in a larger view
        holder.imageView.setOnClickListener(v -> showImageInDialog(bitmap));

        // Show or hide delete button based on edit mode
        holder.deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Set click listener for delete button
        holder.deleteButton.setOnClickListener(v -> {
            String imageId = imageIds.get(position); // Get the corresponding image ID
            deleteImageFromDatabase(imageId); // Delete image from Firebase
            base64Images.remove(position); // Remove from the local list
            imageIds.remove(position); // Remove the corresponding ID
            notifyDataSetChanged(); // Refresh the grid
        });

        return convertView;
    }

    private Bitmap decodeBase64(String base64Image) {
        try {
            String base64Data = base64Image.contains(",") ? base64Image.split(",")[1] : base64Image; // Handle Base64 headers
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteImageFromDatabase(String imageId) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance()
                .getReference("albums")
                .child(currentGalleryUid)
                .child("images")
                .child(imageId);

        databaseRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Image deleted successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to delete image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to show the image in a Dialog
    private void showImageInDialog(Bitmap bitmap) {
        if (bitmap == null) return;

        // Create a full-screen dialog
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_view);

        ImageView largeImageView = dialog.findViewById(R.id.largeImageView);
        largeImageView.setImageBitmap(bitmap);

        // Add a click listener to close the dialog when the image is tapped
        largeImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static class ViewHolder {
        ImageView imageView;
        ImageView deleteButton;
    }
}
