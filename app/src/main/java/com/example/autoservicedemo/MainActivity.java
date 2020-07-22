package com.example.autoservicedemo;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TestFactory factory = TestFactory.getSingleton();
        while (factory.hasNextValue()) {
            Log.e("TestTag", factory.getValue().value());
        }
    }
}
