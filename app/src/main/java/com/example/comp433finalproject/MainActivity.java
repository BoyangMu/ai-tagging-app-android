package com.example.comp433finalproject;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    MediaPlayer clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        clickSound = MediaPlayer.create(this, R.raw.click_buttons);
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);
    }

    public void openPhotoTagger(View view) {
        clickSound.start();
        Intent intent = new Intent(this, PhotoTaggerActivity.class);
        startActivity(intent);
    }

    public void openSketchTagger(View view) {
        clickSound.start();
        Intent intent = new Intent(this, SketchTaggerActivity.class);
        startActivity(intent);
    }

    public void openStoryTeller(View view) {
        clickSound.start();
        Intent intent = new Intent(this, StoryTellerActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent musicIntent = new Intent(this, MusicService.class);
        stopService(musicIntent);
    }
}