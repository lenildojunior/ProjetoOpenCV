package com.example.opencvtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class Configuracoes extends AppCompatActivity {

    TelephonyManager tm;
    String IMEI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        }


        /*Pegando o código do IMEI*/
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = tm.getDeviceId();

        Button Btn_confirmar = (Button) findViewById(R.id.btn_confirmar);
        ProgressBar pb_IMEI = (ProgressBar) findViewById(R.id.progress_IMEI);
        Btn_confirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent inte = new Intent(Configuracoes.this,MainActivity.class);
                startActivity(inte);
            }
        });

    }
}
