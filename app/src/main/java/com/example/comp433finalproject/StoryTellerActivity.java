package com.example.comp433finalproject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StoryTellerActivity extends AppCompatActivity {
    private TextToSpeech tts = null;
    private EditText searchTagsEditText;
    private CheckBox includeSketchesCheckBox;
    private ListView listView;
    private TextView tagsTextView;
    private TextView storyTextView;
    MediaPlayer clickSound;

    private SQLiteDatabase photoDatabase, sketchDatabase;
    private ArrayList<TaggedImage> allItems = new ArrayList<>();
    private ArrayList<TaggedImage> filteredItems = new ArrayList<>();
    private ArrayList<String> selectedTags = new ArrayList<>();

    private String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    private String API_KEY = "YOUR_API_KEY_HERE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_teller);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
        tts.setSpeechRate(1.2f);

        searchTagsEditText = findViewById(R.id.searchTagsEditText);
        includeSketchesCheckBox = findViewById(R.id.includeSketchesCheckBox);
        listView = findViewById(R.id.listView);
        tagsTextView = findViewById(R.id.tagsTextView);
        storyTextView = findViewById(R.id.storyTextView);
        clickSound = MediaPlayer.create(this, R.raw.click_buttons);

        Button findButton = findViewById(R.id.findButton);
        findButton.setOnClickListener(v -> filterItems());

        includeSketchesCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> filterItems());

        photoDatabase = openOrCreateDatabase("ImagesDB", MODE_PRIVATE, null);
        sketchDatabase = openOrCreateDatabase("SketchesDB", MODE_PRIVATE, null);
        photoDatabase.execSQL("CREATE TABLE IF NOT EXISTS images(id INTEGER PRIMARY KEY, image BLOB, datetime TEXT, tags TEXT)");
        sketchDatabase.execSQL("CREATE TABLE IF NOT EXISTS sketches(id INTEGER PRIMARY KEY, image BLOB, datetime TEXT, tags TEXT)");

        loadItems();
        filterItems();

        Button storyButton = findViewById(R.id.storyButton);
        storyButton.setOnClickListener(v -> generateStory());
    }

    private void loadItems() {
        Cursor c = photoDatabase.rawQuery("SELECT * FROM images", null);
        while (c.moveToNext()) {
            byte[] imageBytes = c.getBlob(1);
            String datetime = c.getString(2);
            String tags = c.getString(3);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            TaggedImage image = new TaggedImage(bitmap, tags, datetime, false);
            allItems.add(image);
        }
        c.close();

        Cursor c2 = sketchDatabase.rawQuery("SELECT * FROM sketches", null);
        while (c2.moveToNext()) {
            byte[] imageBytes = c2.getBlob(1);
            String datetime = c2.getString(2);
            String tags = c2.getString(3);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            TaggedImage sketch = new TaggedImage(bitmap, tags, datetime, true);
            allItems.add(sketch);
        }
        c2.close();
    }

    private void filterItems() {
        clickSound.start();
        filteredItems.clear();

        String searchTags = searchTagsEditText.getText().toString();
        String[] searchTerms = searchTags.split(",");

        for (TaggedImage item : allItems) {
            boolean matchesSearch = false;
            for (String term : searchTerms) {
                if (item.getTags().contains(term.trim())) {
                    matchesSearch = true;
                    break;
                }
            }

            boolean isPhoto = !item.getIsSketch();
            if (includeSketchesCheckBox.isChecked() || isPhoto) {
                if (matchesSearch || searchTags.isEmpty()) {
                    filteredItems.add(item);
                }
            }
        }
        updateListView();
    }

    private void updateListView() {
        TaggedImageAdapter adapter = new TaggedImageAdapter(this, R.layout.list_item, filteredItems, true, tagsTextView);
        listView.setAdapter(adapter);
        applyAnimation(listView);
    }

    private void generateStory() {
        clickSound.start();
        TaggedImageAdapter adapter = (TaggedImageAdapter) listView.getAdapter();
        ArrayList<TaggedImage> selectedItems = adapter.getSelectedItems();
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Select at least one item.", Toast.LENGTH_SHORT).show();
        } else {
            for (TaggedImage item : selectedItems) {
                selectedTags.add(item.getTags());
            }

            String keywords = selectedTags.toString();
            keywords = keywords.replace("[", "").replace("]", "");
            try {
                makeHttpRequest(keywords);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void makeHttpRequest(String keywords) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("context", "story");
        data.put("max_tokens", 100);
        data.put("mode", "twitter");
        data.put("model", "claude-3-haiku");


        String[] keywordsArray = keywords.split(",");
        data.put("keywords", new JSONArray(keywordsArray));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("success", response.toString());
                try {
                    JSONObject dataObject = response.getJSONObject("data");
                    JSONArray outputsArray = dataObject.getJSONArray("outputs");
                    JSONObject outputObject = outputsArray.getJSONObject(0);
                    String reponseString = outputObject.getString("text");
                    storyTextView.setText(reponseString);
                    applyAnimation(storyTextView);
                    String story = storyTextView.getText().toString();
                    tts.speak(story, TextToSpeech.QUEUE_FLUSH, null, null);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", new String(error.networkResponse.data));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void applyAnimation(View view) {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        view.startAnimation(fadeIn);
    }

    public void back(View view) {
        clickSound.start();
        finish();
    }
}
