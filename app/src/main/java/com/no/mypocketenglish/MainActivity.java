package com.no.mypocketenglish;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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
    private Button buttonSave, buttonList, buttonStartTest, buttonShowAnswer, buttonStartGame, buttonShowScore;
    private TextView textViewQuestion, textViewAnswer;
    private List<String> sentenceList;
    private List<String> testList;
    private Random random;
    private String currentSentenceSet = "";
    private String correctAnswer = "";
    private int totalScore = 0; // 총점 변수
    private int currentGameScore = 0; // 이번 게임 점수 변수

    // Translator 객체
    private Translator englishKoreanTranslator;

    // Internal Storage 파일명
    private static final String FILE_NAME = "sentence_data.txt";
    private static final String SCORE_FILE_NAME = "total_score_data.txt"; // 총점을 저장할 파일 이름

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 총점 로드
        totalScore = loadTotalScore();

        // 뷰 초기화
        editTextSentence = findViewById(R.id.editTextSentence);
        editTextTranslation = findViewById(R.id.editTextTranslation);
        buttonSave = findViewById(R.id.buttonSave);
        buttonList = findViewById(R.id.buttonList);
        buttonStartTest = findViewById(R.id.buttonStartTest);
        buttonShowAnswer = findViewById(R.id.buttonShowAnswer);
        buttonStartGame = findViewById(R.id.buttonStartGame);
        buttonShowScore = findViewById(R.id.buttonShowScore);
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
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Translation model downloaded", Toast.LENGTH_SHORT).show());

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

        // 게임 시작 버튼
        buttonStartGame.setOnClickListener(v -> showGameDialog());

        // 총점 버튼을 눌렀을 때 다이얼로그 표시
        buttonShowScore.setOnClickListener(v -> showScoreDialog());
    }

    // 번역 함수
    private void translateText(String text) {
        englishKoreanTranslator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    SpannableString spannableTranslation = new SpannableString(translatedText);
                    spannableTranslation.setSpan(
                            new ForegroundColorSpan(0xFF999999),
                            0, spannableTranslation.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    editTextTranslation.setText(spannableTranslation);
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Translation failed", Toast.LENGTH_SHORT).show());
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

    // 게임 시작 다이얼로그
    private void showGameDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game);

        TextView textViewQuestion = dialog.findViewById(R.id.textViewQuestion);
        Button buttonOption1 = dialog.findViewById(R.id.buttonOption1);
        Button buttonOption2 = dialog.findViewById(R.id.buttonOption2);
        Button buttonOption3 = dialog.findViewById(R.id.buttonOption3);
        Button buttonOption4 = dialog.findViewById(R.id.buttonOption4);

        currentGameScore = 0;  // 새로운 게임 시작 시, 이번 게임 점수는 0으로 리셋
        startGame(textViewQuestion, buttonOption1, buttonOption2, buttonOption3, buttonOption4);

        dialog.setOnDismissListener(dialogInterface -> {
            Toast.makeText(MainActivity.this, "Game closed", Toast.LENGTH_SHORT).show();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        dialog.show();
    }

    private void startGame(TextView textViewQuestion, Button... options) {
        sentenceList = loadSentenceSets();
        resetTestList();

        if (!testList.isEmpty()) {
            int randomIndex = random.nextInt(testList.size());
            currentSentenceSet = testList.remove(randomIndex);
            String[] parts = currentSentenceSet.split("///");

            if (parts.length == 2) {
                final String correctAnswer = parts[1];
                textViewQuestion.setText(parts[0]);

                int correctButtonIndex = random.nextInt(4);
                for (int i = 0; i < options.length; i++) {
                    final int index = i;
                    if (i == correctButtonIndex) {
                        options[i].setText(correctAnswer);
                    } else {
                        options[i].setText(generateWrongAnswer());
                    }

                    options[i].setOnClickListener(v -> {
                        if (options[index].getText().equals(correctAnswer)) {
                            currentGameScore++;  // 이번 게임 점수 증가
                            totalScore++;  // 총점 증가
                            saveTotalScore(totalScore);
                            Toast.makeText(MainActivity.this, "Correct! Total Score: " + totalScore, Toast.LENGTH_SHORT).show();
                        } else {
                            currentGameScore = 0;  // 틀리면 이번 게임 점수만 0으로 리셋
                            Toast.makeText(MainActivity.this, "Wrong answer", Toast.LENGTH_SHORT).show();
                        }
                        startGame(textViewQuestion, options);
                    });
                }
            }
        }
    }

    private String generateWrongAnswer() {
        List<String> wrongAnswers = new ArrayList<>(sentenceList);
        wrongAnswers.remove(currentSentenceSet);
        String randomWrongAnswer = wrongAnswers.get(random.nextInt(wrongAnswers.size()));
        return randomWrongAnswer.split("///")[1];
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
            Toast.makeText(this, "No answer available", Toast.LENGTH_SHORT).show();
        }
    }

    // 총점 다이얼로그를 보여주는 메서드
    private void showScoreDialog() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_score);

        // 다이얼로그에서 총점 표시
        TextView textViewTotalScore = dialog.findViewById(R.id.textViewTotalScore);
        textViewTotalScore.setText("Total Score: " + totalScore);

        dialog.show();
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

    // 총점을 파일에 저장하는 메서드
    private void saveTotalScore(int newTotalScore) {
        try (FileOutputStream fos = openFileOutput(SCORE_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(String.valueOf(newTotalScore).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 파일에서 총점을 불러오는 메서드
    private int loadTotalScore() {
        int loadedTotalScore = 0;
        try (FileInputStream fis = openFileInput(SCORE_FILE_NAME);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line = reader.readLine();
            if (line != null) {
                loadedTotalScore = Integer.parseInt(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadedTotalScore;
    }
}
