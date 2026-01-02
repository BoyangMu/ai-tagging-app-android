package com.example.comp433finalproject;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoTaggerActivity extends AppCompatActivity {
    private ImageView imageView;
    private EditText tagsEditText, findEditText;
    private SQLiteDatabase database;
    private final String API_KEY = "YOUR_API_KEY_HERE";
    MediaPlayer clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_photo_tagger);

        imageView = findViewById(R.id.imageView);
        tagsEditText = findViewById(R.id.tagsEditText);
        findEditText = findViewById(R.id.findEditText);
        clickSound = MediaPlayer.create(this, R.raw.click_buttons);

        database = openOrCreateDatabase("ImagesDB", MODE_PRIVATE, null);
        // database.execSQL("DELETE FROM images");
        database.execSQL("CREATE TABLE IF NOT EXISTS images(id INTEGER PRIMARY KEY, image BLOB, datetime TEXT, tags TEXT)");

        loadRecentImages();
    }

    private void loadRecentImages() {
        Cursor c = database.rawQuery("SELECT * FROM images ORDER BY datetime DESC", null);
        ArrayList<TaggedImage> photos = new ArrayList<>();

        while (c.moveToNext()) {
            byte[] imageBytes = c.getBlob(1);
            String datetime = c.getString(2);
            String label = c.getString(3);

            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            photos.add(new TaggedImage(bitmap, label, datetime, false));
            TaggedImageAdapter adapter = new TaggedImageAdapter(PhotoTaggerActivity.this, R.layout.list_item, photos, false);
            ListView lv = findViewById(R.id.photoList);
            lv.setAdapter(adapter);
        }
        c.close();
    }

    public void openCamera(View view) {
        clickSound.start();
        Intent cam_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cam_intent,1);
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap image = (Bitmap) extras.get("data");
            imageView.setImageBitmap(image);
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> labels = photoVision(bitmap);
                        runOnUiThread(() -> {
                            String apiTags = String.join(", ", labels);
                            tagsEditText.setText(apiTags);
                        });
                    } catch (IOException e) {
                        Log.e("vision", e.toString());
                    }
                }
            }).start();
        }
    }

    public void saveImage(View view) {
        clickSound.start();
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] imageBytes = outputStream.toByteArray();

        String tags = tagsEditText.getText().toString();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.getDefault());
        String datetime = isoFormat.format(new Date());

        ContentValues values = new ContentValues();
        values.put("image", imageBytes);
        values.put("datetime", datetime);
        values.put("tags", tags);
        database.insert("images", null, values);
        loadRecentImages();
    }

    public void findImages(View view) {
        clickSound.start();
        String queryTag = findEditText.getText().toString();
        String query = "SELECT * FROM images";
        if (!queryTag.isEmpty()) {
            query += " WHERE tags LIKE '%, " + queryTag + ",%' OR tags LIKE '" + queryTag + ",%' OR tags LIKE '%, " + queryTag + "' OR tags = '" + queryTag + "'";
        }
        query += " ORDER BY datetime DESC";

        Cursor c = database.rawQuery(query, null);
        ArrayList<TaggedImage> photos = new ArrayList<>();
        if (c.getCount() == 0) {
            TaggedImageAdapter adapter = new TaggedImageAdapter(PhotoTaggerActivity.this, R.layout.list_item, photos, false);
            ListView lv = findViewById(R.id.photoList);
            lv.setAdapter(adapter);
        } else {
            while (c.moveToNext()) {
                byte[] imageBytes = c.getBlob(1);
                String datetime = c.getString(2);
                String label = c.getString(3);

                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                photos.add(new TaggedImage(bitmap, label, datetime, false));
                TaggedImageAdapter adapter = new TaggedImageAdapter(PhotoTaggerActivity.this, R.layout.list_item, photos, false);
                ListView lv = findViewById(R.id.photoList);
                lv.setAdapter(adapter);
            }
        }
        c.close();
    }

    private List<String> photoVision(Bitmap bitmap) throws IOException {
        //1. ENCODE image.
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myImage = new Image();
        myImage.encodeContent(bout.toByteArray());

        //2. PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myImage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<Feature>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        //3.BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer(API_KEY));
        Vision vision = builder.build();

        //4. CALL Vision.Images.Annotate
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        BatchAnnotateImagesResponse response = task.execute();

        List<String> labels = new ArrayList<>();

        if (response.getResponses() != null && !response.getResponses().isEmpty()) {
            AnnotateImageResponse imageResponse = response.getResponses().get(0);

            if (imageResponse.getLabelAnnotations() != null && !imageResponse.getLabelAnnotations().isEmpty()) {
                for (EntityAnnotation annotation : imageResponse.getLabelAnnotations()) {
                    if (annotation.getScore() >= 0.85) {
                        labels.add(annotation.getDescription());
                    }
                }

                // If no label met the threshold, add only the first label
                if (labels.isEmpty()) {
                    labels.add(imageResponse.getLabelAnnotations().get(0).getDescription());
                }
            }
        }

        Log.v("MYTAG", "Labels: " + labels);
        return labels;
    }

    public void back(View view) {
        clickSound.start();
        finish();
    }
}
