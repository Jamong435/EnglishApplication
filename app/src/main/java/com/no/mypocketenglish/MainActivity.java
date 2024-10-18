package com.no.mypocketenglish;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private List<String> sentenceList;  // 전체 문장 리스트
    private List<String> testList;  // 테스트 시 사용할 문장 리스트
    private Random random;
    private String currentSentenceSet = ""; // 현재 테스트 중인 문장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 시스템 알림창 권한 요청
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

        // 문장 불러오기
        sentenceList = loadSentenceSets();

        // 저장된 문장이 없으면 빈 리스트로 초기화
        if (sentenceList.isEmpty()) {
            sentenceList = new ArrayList<>();
        }

        // 테스트 시작 시 사용할 리스트 초기화
        resetTestList();

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

        // "테스트 시작" 버튼 클릭 리스너
        buttonStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();  // 최신 데이터를 반영해 테스트 시작
            }
        });

        // "답지 확인" 버튼 클릭 리스너
        buttonShowAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnswer();
            }
        });
    }

    // 테스트 시작 시 문장을 무작위로 선택하고 출력
    private void startTest() {
        sentenceList = loadSentenceSets(); // 최신 데이터를 다시 로드
        resetTestList();  // 테스트 리스트 초기화

        // 남은 문장이 없으면 초기화
        if (testList.isEmpty()) {
            resetTestList();
        }

        if (!testList.isEmpty()) {
            int randomIndex = random.nextInt(testList.size());
            currentSentenceSet = testList.remove(randomIndex);

            String[] parts = currentSentenceSet.split("///");
            if (parts.length == 2) {
                textViewQuestion.setText(parts[0]);  // 영어 문장 표시
                textViewAnswer.setText("");  // 답은 숨김
                textViewQuestion.setVisibility(View.VISIBLE);  // 영어 문장 보이게 설정
                textViewAnswer.setVisibility(View.GONE);  // 답 숨김
                buttonShowAnswer.setVisibility(View.VISIBLE);  // 답지 확인 버튼 활성화
            } else {
                textViewQuestion.setText("No valid sentence found.");
                textViewQuestion.setVisibility(View.VISIBLE);
            }
        } else {
            textViewQuestion.setText("No sentences available.");
            textViewQuestion.setVisibility(View.VISIBLE);
        }
    }

    // 답을 보여주는 메서드
    private void showAnswer() {
        if (!currentSentenceSet.isEmpty()) {
            String[] parts = currentSentenceSet.split("///");
            if (parts.length == 2) {
                textViewAnswer.setText(parts[1]);  // 한국어 뜻 표시
                textViewAnswer.setVisibility(View.VISIBLE);  // 답 표시
            } else {
                textViewAnswer.setText("No answer available.");
                textViewAnswer.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "No answer available.", Toast.LENGTH_SHORT).show();
        }
    }

    // 문장을 저장하는 메서드
    private void saveSentenceSet(String sentence, String translation) {
        Set<String> sentenceSets = sharedPreferences.getStringSet(SENTENCES_KEY, new HashSet<>());
        sentenceSets.add(sentence + "///" + translation);
        sharedPreferences.edit().putStringSet(SENTENCES_KEY, sentenceSets).commit();

        // 새 문장 저장 후 리스트 업데이트
        sentenceList = loadSentenceSets();
        resetTestList();
    }

    // 문장을 불러오는 메서드
    private List<String> loadSentenceSets() {
        Set<String> sentenceSets = sharedPreferences.getStringSet(SENTENCES_KEY, new HashSet<>());
        return new ArrayList<>(sentenceSets);
    }

    // 테스트 리스트 초기화 (모든 문장 포함)
    private void resetTestList() {
        testList = new ArrayList<>(sentenceList);
    }
}
