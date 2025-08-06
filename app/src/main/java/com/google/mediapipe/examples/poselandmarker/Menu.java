package com.google.mediapipe.examples.poselandmarker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Configura o botão de Missões
        Button btnMissoes = findViewById(R.id.btn_missoes);
        btnMissoes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, Missao.class);
                startActivity(intent);
            }
        });

        // Configura o botão de Status
        Button btnStatus = findViewById(R.id.btn_status);
        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, Status.class);
                startActivity(intent);
            }
        });
    }
}