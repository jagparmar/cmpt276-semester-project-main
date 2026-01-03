package com.example.nostalgianest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MemoriesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_memories);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
    //Used to re-direct to corrossponding subpages

    //memory page -> createGallery
    public void createMemoryButtonClicked(View v){
        //Re-direct to the create new gallery activity
        Intent i = new Intent(this, createGallery.class);
        startActivity(i);
    }
    //memory page ->joinAGallery
    public void joinGalleryClicked(View v){
        //Re-direct to the create new gallery activity
        Intent i = new Intent(this, JoinAGallery.class);
        startActivity(i);
    }
    //memory page ->GalleryActivity
    public void yourGalleryButtonCLicked(View v){
        //Re-direct to the Gallery Activity
        Intent i = new Intent(this,GalleryActivity.class);
        startActivity(i);
    }

    public void homeButtonClicked(View v){
        //Re-direct to the Gallery Activity
        Intent i = new Intent(this,MainActivity.class);
        startActivity(i);
    }

}