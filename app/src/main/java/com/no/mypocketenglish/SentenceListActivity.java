package com.no.mypocketenglish;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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
    private ArrayAdapter<SpannableString> adapter;
    private List<SpannableString> sentenceList;
    private List<SpannableString> fullSentenceList;
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
            for (SpannableString sentenceSet : fullSentenceList) {
                if (sentenceSet.toString().toLowerCase().contains(query.toLowerCase())) {
                    sentenceList.add(sentenceSet);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // 문장 세트 불러오기
    private List<SpannableString> loadSentenceSets() {
        try {
            if (sharedPreferences.contains(SENTENCES_KEY)) {
                Set<String> sentenceSets = sharedPreferences.getStringSet(SENTENCES_KEY, new HashSet<>());
                List<SpannableString> sentenceList = new ArrayList<>();

                for (String sentenceSet : sentenceSets) {
                    String[] parts = sentenceSet.split("///");
                    if (parts.length == 2) {
                        String englishSentence = parts[0];
                        String koreanTranslation = parts[1];

                        // SpannableString으로 각 텍스트의 색상 적용
                        SpannableString spannableString = new SpannableString(englishSentence + "\n" + koreanTranslation);

                        // 영어 부분에 #000000 적용
                        spannableString.setSpan(
                                new ForegroundColorSpan(Color.parseColor("#000000")),
                                0,
                                englishSentence.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );

                        // 한국어 부분에 #999999 적용
                        spannableString.setSpan(
                                new ForegroundColorSpan(Color.parseColor("#999999")),
                                englishSentence.length() + 1, // 한 줄 바꿈 이후
                                spannableString.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );

                        // 리스트에 추가
                        sentenceList.add(spannableString);
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
        final SpannableString selectedSentenceSet = sentenceList.get(position);
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
        final SpannableString selectedSentenceSet = sentenceList.get(position);
        String[] parts = selectedSentenceSet.toString().split("\n");

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
        SpannableString updatedSentence = new SpannableString(newSentence + "\n" + newTranslation);

        // 영어 부분에 #000000 적용
        updatedSentence.setSpan(
                new ForegroundColorSpan(Color.parseColor("#000000")),
                0,
                newSentence.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // 한국어 부분에 #999999 적용
        updatedSentence.setSpan(
                new ForegroundColorSpan(Color.parseColor("#999999")),
                newSentence.length() + 1,
                updatedSentence.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        sentenceList.set(position, updatedSentence);
        fullSentenceList.set(position, updatedSentence);
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
            for (SpannableString sentence : fullSentenceList) {
                String[] parts = sentence.toString().split("\n");
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
