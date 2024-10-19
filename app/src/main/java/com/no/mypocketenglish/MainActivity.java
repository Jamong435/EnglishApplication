package com.no.mypocketenglish;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText editTextSentence, editTextTranslation;
    private Button buttonSave, buttonList, buttonStartTest, buttonShowAnswer;
    private TextView textViewQuestion, textViewAnswer;
    private List<String> sentenceList;
    private List<String> testList;
    private Random random;
    private String currentSentenceSet = "";

    // Translator 객체
    private Translator englishKoreanTranslator;

    // Internal Storage 파일명
    private static final String FILE_NAME = "sentence_data.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startApp();
    }

    private void startApp() {
        setContentView(R.layout.activity_main);

        editTextSentence = findViewById(R.id.editTextSentence);
        editTextTranslation = findViewById(R.id.editTextTranslation);
        buttonSave = findViewById(R.id.buttonSave);
        buttonList = findViewById(R.id.buttonList);
        buttonStartTest = findViewById(R.id.buttonStartTest);
        buttonShowAnswer = findViewById(R.id.buttonShowAnswer);
        textViewQuestion = findViewById(R.id.textViewQuestion);
        textViewAnswer = findViewById(R.id.textViewAnswer);

        random = new Random();
        sentenceList = loadSentenceSets(); // 파일에서 데이터 로드

        if (sentenceList.isEmpty()) {
            sentenceList = new ArrayList<>();
        }

        resetTestList();

        // 번역 옵션 설정 (영어 -> 한국어)
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build();
        englishKoreanTranslator = com.google.mlkit.nl.translate.Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
        englishKoreanTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "번역 모델 다운로드 완료", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "번역 모델 다운로드 실패", Toast.LENGTH_SHORT).show());

        editTextSentence.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                translateText(editable.toString());
            }
        });

        buttonSave.setOnClickListener(v -> {
            String sentence = editTextSentence.getText().toString().trim();
            String translation = editTextTranslation.getText().toString().trim();
            if (!sentence.isEmpty() && !translation.isEmpty()) {
                saveSentenceSet(sentence, translation);
                editTextSentence.setText("");
                editTextTranslation.setText("");
                Toast.makeText(MainActivity.this, "Sentence set saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please enter both the sentence and its meaning.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SentenceListActivity.class);
            startActivity(intent);
        });

        buttonStartTest.setOnClickListener(v -> showTestDialog());

        buttonShowAnswer.setOnClickListener(v -> showAnswer(textViewAnswer));
    }

    // 번역 함수
    private void translateText(String text) {
        englishKoreanTranslator.translate(text)
                .addOnSuccessListener(translatedText -> editTextTranslation.setText(translatedText))
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "번역 실패", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                view.clearFocus();
            }
        }
        return super.onTouchEvent(event);
    }

    private void showTestDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_test);

        TextView textViewQuestion = dialog.findViewById(R.id.textViewQuestion);
        Button buttonShowAnswer = dialog.findViewById(R.id.buttonShowAnswer);
        TextView textViewAnswer = dialog.findViewById(R.id.textViewAnswer);
        Button buttonNextQuestion = dialog.findViewById(R.id.buttonNextQuestion);
        Button buttonClose = dialog.findViewById(R.id.buttonClose);

        startTest(textViewQuestion, textViewAnswer);

        buttonShowAnswer.setOnClickListener(v -> showAnswer(textViewAnswer));

        buttonNextQuestion.setOnClickListener(v -> startTest(textViewQuestion, textViewAnswer));

        buttonClose.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        dialog.show();
    }

    private void startTest(TextView textViewQuestion, TextView textViewAnswer) {
        sentenceList = loadSentenceSets();
        resetTestList();

        if (!testList.isEmpty()) {
            int randomIndex = random.nextInt(testList.size());
            currentSentenceSet = testList.remove(randomIndex);

            String[] parts = currentSentenceSet.split("///");
            if (parts.length == 2) {
                textViewQuestion.setText(parts[0]);
                textViewAnswer.setText("");
            } else {
                textViewQuestion.setText("No valid sentence found.");
            }
        } else {
            textViewQuestion.setText("No sentences available.");
        }
    }

    private void showAnswer(TextView textViewAnswer) {
        if (!currentSentenceSet.isEmpty()) {
            String[] parts = currentSentenceSet.split("///");
            if (parts.length == 2) {
                textViewAnswer.setText(parts[1]);
                textViewAnswer.setVisibility(View.VISIBLE);
            } else {
                textViewAnswer.setText("No answer available.");
                textViewAnswer.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "No answer available.", Toast.LENGTH_SHORT).show();
        }
    }

    // 문장을 파일에 저장하는 메서드
    private void saveSentenceSet(String sentence, String translation) {
        sentenceList.add(sentence + "///" + translation);
        try (FileOutputStream fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            for (String entry : sentenceList) {
                fos.write((entry + "\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 문장을 파일에서 불러오는 메서드
    private List<String> loadSentenceSets() {
        List<String> sentenceSets = new ArrayList<>();
        try (FileInputStream fis = openFileInput(FILE_NAME);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                sentenceSets.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sentenceSets;
    }

    private void resetTestList() {
        testList = new ArrayList<>(sentenceList);
    }
}
