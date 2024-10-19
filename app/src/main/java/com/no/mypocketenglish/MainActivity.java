package com.no.mypocketenglish;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int SYSTEM_ALERT_WINDOW_PERMISSION = 2084;
    private EditText editTextSentence, editTextTranslation;
    private Button buttonSave, buttonList, buttonStartTest, buttonShowAnswer;
    private TextView textViewQuestion, textViewAnswer;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SentencesPrefs";
    private static final String SENTENCES_KEY = "Sentences";
    private List<String> sentenceList;
    private List<String> testList;
    private Random random;
    private String currentSentenceSet = "";

    // Translator 객체
    private Translator englishKoreanTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showPermissionDialog();
        } else {
            startApp();
        }
    }

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("권한 요청");
        builder.setMessage("이 앱을 사용하려면 '다른 앱 위에 표시' 권한이 필요합니다. 권한을 허용하시겠습니까?");
        builder.setPositiveButton("허용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestOverlayPermission();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "권한이 없으면 앱을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        builder.show();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startApp();
            } else {
                Toast.makeText(this, "SYSTEM_ALERT_WINDOW 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        random = new Random();

        sentenceList = loadSentenceSets();

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

        // 번역 모델 다운로드 (인터넷 연결 시)
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        englishKoreanTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void v) {
                                Toast.makeText(MainActivity.this, "번역 모델 다운로드 완료", Toast.LENGTH_SHORT).show();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(MainActivity.this, "번역 모델 다운로드 실패", Toast.LENGTH_SHORT).show();
                            }
                        });

        editTextSentence.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                translateText(editable.toString());
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        buttonList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SentenceListActivity.class);
                startActivity(intent);
            }
        });

        buttonStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTestDialog();
            }
        });

        buttonShowAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnswer(textViewAnswer);
            }
        });
    }

    // 번역 함수
    private void translateText(String text) {
        englishKoreanTranslator.translate(text)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        editTextTranslation.setText(translatedText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "번역 실패", Toast.LENGTH_SHORT).show();
                    }
                });
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

        buttonShowAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnswer(textViewAnswer);
            }
        });

        buttonNextQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest(textViewQuestion, textViewAnswer);
            }
        });

        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

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

    private void saveSentenceSet(String sentence, String translation) {
        Set<String> sentenceSets = sharedPreferences.getStringSet(SENTENCES_KEY, new HashSet<>());
        sentenceSets.add(sentence + "///" + translation);
        sharedPreferences.edit().putStringSet(SENTENCES_KEY, sentenceSets).commit();

        sentenceList = loadSentenceSets();
        resetTestList();
    }

    private List<String> loadSentenceSets() {
        Set<String> sentenceSets = sharedPreferences.getStringSet(SENTENCES_KEY, new HashSet<>());
        return new ArrayList<>(sentenceSets);
    }

    private void resetTestList() {
        testList = new ArrayList<>(sentenceList);
    }
}
