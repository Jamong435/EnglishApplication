package com.no.mypocketenglish;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SentenceListActivity extends AppCompatActivity {

    private static final String TAG = "SentenceListActivity";
    private ListView listView;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SentencesPrefs";
    private static final String SENTENCES_KEY = "Sentences";
    private ArrayAdapter<String> adapter;
    private List<String> sentenceList;
    private List<String> fullSentenceList;
    private Button buttonBack;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence_list);

        listView = findViewById(R.id.listView);
        SearchView searchView = findViewById(R.id.searchView);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        buttonBack = findViewById(R.id.buttonBack);

        Log.d(TAG, "onCreate: App started, initializing data");

        // 저장된 문장 세트를 불러옵니다
        try {
            sentenceList = loadSentenceSets();
            if (sentenceList == null) {
                sentenceList = new ArrayList<>(); // 리스트가 null인 경우 빈 리스트로 초기화
                Log.d(TAG, "onCreate: sentenceList was null, initialized to empty list.");
            }
            fullSentenceList = new ArrayList<>(sentenceList); // 검색을 위해 전체 리스트를 저장
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sentenceList);
            listView.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error loading sentences", e);
            Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
        }

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showCRUDDialog(position);
            return true;
        });
    }

    // 리스트 필터링
    private void filterList(String query) {
        sentenceList.clear();
        if (TextUtils.isEmpty(query)) {
            sentenceList.addAll(fullSentenceList);
        } else {
            for (String sentenceSet : fullSentenceList) {
                if (sentenceSet.toLowerCase().contains(query.toLowerCase())) {
                    sentenceList.add(sentenceSet);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // 문장 세트 불러오기
    private List<String> loadSentenceSets() {
        try {
            // Set<String> 형태로 저장된 데이터를 불러오려고 시도
            if (sharedPreferences.contains(SENTENCES_KEY)) {
                Object storedValue = sharedPreferences.getAll().get(SENTENCES_KEY);

                // 기존 데이터가 String 형태로 저장되었을 경우 처리
                if (storedValue instanceof String) {
                    String oldStringValue = (String) storedValue;
                    Log.d(TAG, "loadSentenceSets: Found old String format, converting to Set<String>");

                    // 기존 String 데이터를 Set<String>으로 변환
                    Set<String> sentenceSets = new HashSet<>();
                    sentenceSets.add(oldStringValue);

                    // 변환된 데이터를 다시 Set<String>으로 SharedPreferences에 저장
                    sharedPreferences.edit().putStringSet(SENTENCES_KEY, sentenceSets).apply();
                }

                // Set<String> 데이터를 불러옴
                Set<String> sentenceSets = sharedPreferences.getStringSet(SENTENCES_KEY, new HashSet<>());
                List<String> sentenceList = new ArrayList<>();
                for (String sentenceSet : sentenceSets) {
                    String[] parts = sentenceSet.split("///");
                    if (parts.length == 2) {
                        sentenceList.add("English: " + parts[0] + "\nKorean: " + parts[1]);
                    }
                }
                Log.d(TAG, "loadSentenceSets: Loaded " + sentenceList.size() + " sentences.");
                return sentenceList;
            }
        } catch (Exception e) {
            Log.e(TAG, "loadSentenceSets: Error loading sentence sets", e);
        }
        return new ArrayList<>();
    }

    private void showCRUDDialog(int position) {
        final String selectedSentenceSet = sentenceList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose action");

        builder.setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
            if (which == 0) {
                showEditDialog(position);
            } else if (which == 1) {
                deleteSentenceSet(position);
            }
        });

        builder.show();
    }

    private void showEditDialog(int position) {
        final String selectedSentenceSet = sentenceList.get(position);
        String[] parts = selectedSentenceSet.replace("English: ", "").replace("Korean: ", "").split("\n");

        final EditText editSentence = new EditText(this);
        editSentence.setHint("English Sentence");
        editSentence.setText(parts[0]);

        final EditText editTranslation = new EditText(this);
        editTranslation.setHint("Korean Translation");
        editTranslation.setText(parts[1]);

        final android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(editSentence);
        layout.addView(editTranslation);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Sentence");
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newSentence = editSentence.getText().toString().trim();
            String newTranslation = editTranslation.getText().toString().trim();

            if (!newSentence.isEmpty() && !newTranslation.isEmpty()) {
                updateSentenceSet(position, newSentence, newTranslation);
            } else {
                Toast.makeText(SentenceListActivity.this, "Both fields must be filled", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSentenceSet(int position, String newSentence, String newTranslation) {
        sentenceList.set(position, "English: " + newSentence + "\nKorean: " + newTranslation);
        fullSentenceList.set(position, "English: " + newSentence + "\nKorean: " + newTranslation);
        adapter.notifyDataSetChanged();

        saveSentenceSets();
        Toast.makeText(this, "Sentence updated!", Toast.LENGTH_SHORT).show();
    }

    private void deleteSentenceSet(int position) {
        sentenceList.remove(position);
        fullSentenceList.remove(position);
        adapter.notifyDataSetChanged();

        saveSentenceSets();
        Toast.makeText(this, "Sentence deleted!", Toast.LENGTH_SHORT).show();
    }

    private void saveSentenceSets() {
        try {
            Set<String> sentenceSets = new HashSet<>();
            for (String sentence : fullSentenceList) {
                String[] parts = sentence.replace("English: ", "").replace("\nKorean: ", "///").split("///");
                if (parts.length == 2) {
                    sentenceSets.add(parts[0] + "///" + parts[1]);
                }
            }

            Log.d(TAG, "saveSentenceSets: Saving " + sentenceSets.size() + " sentences");
            sharedPreferences.edit().putStringSet(SENTENCES_KEY, sentenceSets).apply();

        } catch (Exception e) {
            Log.e(TAG, "saveSentenceSets: Error saving sentence sets", e);
        }
    }
}
