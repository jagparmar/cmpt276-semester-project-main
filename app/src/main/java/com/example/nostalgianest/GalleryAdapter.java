package com.example.nostalgianest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {
    private List<albumObject> galleryList;
    private final OnGalleryClickListener onGalleryClickListener;

    // Constructor
    public GalleryAdapter(List<albumObject> galleryList, OnGalleryClickListener listener) {
        this.galleryList = galleryList;
        this.onGalleryClickListener = listener;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        albumObject album = galleryList.get(position);
        holder.bind(album, onGalleryClickListener);
    }

    @Override
    public int getItemCount() {
        return galleryList.size();
    }

    // Update gallery data
    public void updateData(List<albumObject> newGalleryList) {
        this.galleryList = newGalleryList;
        notifyDataSetChanged();
    }

    // ViewHolder for RecyclerView
    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        private final Button galleryButton;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            galleryButton = itemView.findViewById(R.id.btnGalleryItem);
        }

        public void bind(albumObject album, OnGalleryClickListener listener) {
            galleryButton.setText(album.getAlbumName());
            galleryButton.setOnClickListener(v -> listener.onGalleryClick(album.getUid()));
        }
    }

    // Interface for click handling
    public interface OnGalleryClickListener {
        void onGalleryClick(String galleryUid);
    }
}
