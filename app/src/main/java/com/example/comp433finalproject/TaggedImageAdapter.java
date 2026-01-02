package com.example.comp433finalproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;

public class TaggedImageAdapter extends ArrayAdapter<TaggedImage> {
    private boolean showCheckbox; // Flag to control CheckBox visibility
    private HashSet<Integer> selectedPositions = new HashSet<>();
    private static final int MAX_SELECTIONS = 3; // Limit for selections
    private Context context;
    private TextView storyTextView;

    TaggedImageAdapter(Context context, int resource, ArrayList<TaggedImage> objects, boolean showCheckbox) {
        super(context, resource, objects);
        this.context = context;
        this.showCheckbox = showCheckbox;
    }

    TaggedImageAdapter(Context context, int resource, ArrayList<TaggedImage> objects, boolean showCheckbox, TextView storyTextView) {
        super(context, resource, objects);
        this.context = context;
        this.showCheckbox = showCheckbox;
        this.storyTextView = storyTextView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TaggedImage taggedImage = getItem(position);
        ImageView imageView = convertView.findViewById(R.id.imageView);
        TextView tagsTextView = convertView.findViewById(R.id.tagsTextView);
        TextView dateTextView = convertView.findViewById(R.id.dateTextView);

        imageView.setImageBitmap(taggedImage.getImage());
        tagsTextView.setText(taggedImage.getTags());
        dateTextView.setText(taggedImage.getTimestamp());
        CheckBox checkBox = convertView.findViewById(R.id.selectCheckBox);

        if (showCheckbox) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setOnCheckedChangeListener(null); // Prevent triggering listener when setting state
            checkBox.setChecked(selectedPositions.contains(position));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selectedPositions.size() < MAX_SELECTIONS) {
                        selectedPositions.add(position);
                    } else {
                        buttonView.setChecked(false);
                        Toast.makeText(context, "You can select up to 3 items.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    selectedPositions.remove(position);
                }
                if (storyTextView != null) {
                    ArrayList<String> selectedTags = new ArrayList<>();
                    for (int pos : selectedPositions) {
                        selectedTags.add(getItem(pos).getTags());
                    }
                    storyTextView.setText("You selected: " + String.join(", ", selectedTags));
                }
            });
        } else {
            checkBox.setVisibility(View.GONE);
        }

        return convertView;
    }

    public ArrayList<TaggedImage> getSelectedItems() {
        ArrayList<TaggedImage> selectedItems = new ArrayList<>();
        for (int position : selectedPositions) {
            selectedItems.add(getItem(position));
        }
        return selectedItems;
    }
}
