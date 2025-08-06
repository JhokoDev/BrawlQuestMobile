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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Status extends AppCompatActivity {

    private TextView txt_hp, txt_mp, txt_nivel;
    private int hp, mp;
    private int hpi, hpp = 100;
    private int mpi, mpp = 10;

    private boolean areButtonsVisible = false;
    private ImageButton[] menuButtons;
    private ImageButton btnToggleMenu;
    private Button btn_forca, btn_vitalidade, btn_agilidade, btn_sentidos, btn_inteligencia;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estatus);
        GameStateManager.initialize(this);


        txt_hp = findViewById(R.id.txt_hp);
        txt_mp = findViewById(R.id.txt_mp);
        txt_nivel = findViewById(R.id.txt_nivel);
        updateNivel();

        calcularStatus();
        atualizarStatusUI();

        btn_forca = findViewById(R.id.btn_forca);
         btn_vitalidade = findViewById(R.id.btn_vitalidade);
         btn_agilidade = findViewById(R.id.btn_agilidade);
         btn_sentidos = findViewById(R.id.btn_sentidos);
         btn_inteligencia = findViewById(R.id.btn_inteligencia);

// Atualiza o texto inicial dos botões
        updateButtonTexts();
        if (GameStateManager.getPontos()>0) {
// Listeners
            btn_forca.setOnClickListener(v -> {
                GameStateManager.setForca(GameStateManager.getForca() + 1);
                updateButtonTexts();
                calcularStatus();
                atualizarStatusUI();
            });

            btn_vitalidade.setOnClickListener(v -> {
                GameStateManager.setVitalidade(GameStateManager.getVitalidade() + 1);
                updateButtonTexts();
                calcularStatus();
                atualizarStatusUI();
            });

            btn_agilidade.setOnClickListener(v -> {
                GameStateManager.setAgilidade(GameStateManager.getAgilidade() + 1);
                updateButtonTexts();
                calcularStatus();
                atualizarStatusUI();
            });

            btn_sentidos.setOnClickListener(v -> {
                GameStateManager.setSentidos(GameStateManager.getSentidos() + 1);
                updateButtonTexts();
                calcularStatus();
                atualizarStatusUI();
            });

            btn_inteligencia.setOnClickListener(v -> {
                GameStateManager.setInteligencia(GameStateManager.getInteligencia() + 1);
                updateButtonTexts();
                calcularStatus();
                atualizarStatusUI();
            });
        }
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

        // Configura ações dos botões
        menuButtons[3].setOnClickListener(v -> {
            startActivity(new Intent(Status.this, Missao.class));
        });

        // Ações de teste para outros botões
        menuButtons[0].setOnClickListener(v -> showToast("Botão 1"));
        menuButtons[1].setOnClickListener(v -> showToast("Botão 2"));
        menuButtons[2].setOnClickListener(v -> showToast("Botão 3"));
        menuButtons[4].setOnClickListener(v -> showToast("Botão 5"));
        menuButtons[5].setOnClickListener(v -> showToast("Botão 6"));
        menuButtons[6].setOnClickListener(v -> showToast("Botão 7"));
    }

    private void setButtonsVisibility(int visibility, boolean animate) {
        float toggleX = btnToggleMenu.getX();
        float toggleY = btnToggleMenu.getY();

        for (int i = 0; i < menuButtons.length; i++) {
            final ImageButton button = menuButtons[i];
            if (button == null) continue;

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
                        set.setStartDelay(index * 50); // Efeito cascata
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
                    set.setStartDelay(index * 50); // Efeito cascata
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

    private void updateButtonTexts() {
        btn_forca.setText("Força: " + GameStateManager.getForca());
        btn_vitalidade.setText("Vitalidade: " + GameStateManager.getVitalidade());
        btn_agilidade.setText("Agilidade: " + GameStateManager.getAgilidade());
        btn_sentidos.setText("Sentidos: " + GameStateManager.getSentidos());
        btn_inteligencia.setText("Inteligência: " + GameStateManager.getInteligencia());

    }

    private void updateNivel() {
        txt_nivel.setText("NIVEL: " + GameStateManager.getLevel());
    }

    private void calcularStatus() {
        int vitalidade = GameStateManager.getVitalidade();
        int inteligencia = GameStateManager.getInteligencia();
        int nivel = GameStateManager.getLevel();

        hpi = vitalidade * 100;
        hp = hpi + (hpp + 10 * nivel - 10);

        mpi = inteligencia * 100;
        mp = mpi + (mpp + 10 * nivel - 10);
    }

    private void atualizarStatusUI() {
        txt_hp.setText("HP: " + hp);
        txt_mp.setText("MP: " + mp);
    }


}