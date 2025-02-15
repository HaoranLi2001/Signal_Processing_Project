package com.example.android.signallab;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button accButton = findViewById(R.id.mAcceleratorButton);
        Button recButton = findViewById(R.id.mRecorderButton);
        Button vidButton = findViewById(R.id.mVideoButton);
        accButton.setOnClickListener(v -> {
            Intent startAccActivityIntent = new Intent(MainActivity.this,
                    AccelerometerActivity.class);
            startActivity(startAccActivityIntent);
        });
        recButton.setOnClickListener(v -> {
            Intent startRecordActivityIntent = new Intent(MainActivity.this,
                    RecorderActivity.class);
            startActivity(startRecordActivityIntent);
        });
        vidButton.setOnClickListener(v -> {
            Intent startRecordActivityIntent = new Intent(MainActivity.this,
                    VideoActivity.class);
            startActivity(startRecordActivityIntent);
        });

    }
}
