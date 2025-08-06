package com.google.mediapipe.examples.poselandmarker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Missao extends AppCompatActivity {

    private boolean areButtonsVisible = false;
    private ImageButton[] menuButtons;
    private ImageButton btnToggleMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missao);

        // Inicializa botões do menu
        menuButtons = new ImageButton[]{
                findViewById(R.id.btn1),
                findViewById(R.id.btn2),
                findViewById(R.id.btn3),
                findViewById(R.id.btn4),
                findViewById(R.id.btn5),
                findViewById(R.id.btn6),
                findViewById(R.id.btn7)
        };

        // Inicializa botão de toggle
        btnToggleMenu = findViewById(R.id.btn_back_main);

        // Oculta botões inicialmente sem animação
        setButtonsVisibility(View.GONE, false);

        // Configura toggle do menu
        btnToggleMenu.setOnClickListener(v -> {
            areButtonsVisible = !areButtonsVisible;
            setButtonsVisibility(areButtonsVisible ? View.VISIBLE : View.GONE, true);
        });

        // Configura o clique no botão Fortalecimento do Corpo
        Button btnFdc = findViewById(R.id.btn_fdc);
        btnFdc.setOnClickListener(v -> {
            Intent intent = new Intent(Missao.this, FortalecimentoDoCorpo.class);
            startActivity(intent);
        });

        // Configura os cliques nos outros botões para navegar para outras telas
        menuButtons[3].setOnClickListener(v -> {
            Intent intent = new Intent(Missao.this, Status.class);
            startActivity(intent);
        });

        menuButtons[4].setOnClickListener(v -> {
            Intent intent = new Intent(Missao.this, Loja.class);
            startActivity(intent);
        });

        // Ações de teste para outros botões
        menuButtons[0].setOnClickListener(v -> showToast("Botão 1"));
        menuButtons[1].setOnClickListener(v -> showToast("Botão 2"));
        menuButtons[2].setOnClickListener(v -> showToast("Botão 3"));
        menuButtons[5].setOnClickListener(v -> showToast("Botão 6"));
        menuButtons[6].setOnClickListener(v -> showToast("Botão 7"));
    }

    private void setButtonsVisibility(int visibility, boolean animate) {
        float toggleX = btnToggleMenu.getX();
        float toggleY = btnToggleMenu.getY();

        for (int i = 0; i < menuButtons.length; i++) {
            final ImageButton button = menuButtons[i];
            if (button == null) continue;

            // Cria uma cópia final do índice para uso na lambda
            final int index = i;

            if (animate) {
                if (visibility == View.VISIBLE) {
                    // Define visibilidade para VISIBLE e alpha para 0
                    button.setVisibility(View.VISIBLE);
                    button.setAlpha(0f);

                    // Usa post para obter a posição final após o layout
                    button.post(() -> {
                        float layoutX = button.getX();
                        float layoutY = button.getY();
                        float startTranslationX = toggleX - layoutX;
                        float startTranslationY = toggleY - layoutY;

                        // Define a translação inicial
                        button.setTranslationX(startTranslationX);
                        button.setTranslationY(startTranslationY);

                        // Cria animações
                        ObjectAnimator animX = ObjectAnimator.ofFloat(button, "translationX", startTranslationX, 0f);
                        ObjectAnimator animY = ObjectAnimator.ofFloat(button, "translationY", startTranslationY, 0f);
                        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);

                        // Combina animações em um AnimatorSet
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(animX, animY, alphaAnim);
                        set.setDuration(350);
                        set.setStartDelay(index * 50); // Usa index em vez de i
                        set.start();
                    });
                } else {
                    // Animação de saída: Deslizamento + Fade Out
                    float layoutX = button.getX();
                    float layoutY = button.getY();
                    float endTranslationX = toggleX - layoutX;
                    float endTranslationY = toggleY - layoutY;

                    ObjectAnimator animX = ObjectAnimator.ofFloat(button, "translationX", 0f, endTranslationX);
                    ObjectAnimator animY = ObjectAnimator.ofFloat(button, "translationY", 0f, endTranslationY);
                    ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f);

                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(animX, animY, alphaAnim);
                    set.setDuration(350);
                    set.setStartDelay(index * 50); // Usa index em vez de i
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            button.setVisibility(View.GONE);
                            button.setTranslationX(0f);
                            button.setTranslationY(0f);
                            button.setAlpha(1f); // Reseta alpha
                        }
                    });
                    set.start();
                }
            } else {
                // Sem animação (para inicialização)
                button.setVisibility(visibility);
                button.setTranslationX(0f);
                button.setTranslationY(0f);
                button.setAlpha(1f);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}