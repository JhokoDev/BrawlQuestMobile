package com.google.mediapipe.examples.poselandmarker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Status extends AppCompatActivity {

    private TextView txt_hp, txt_mp, txt_nivel;
    private ProgressBar hpProgressBar, mpProgressBar; // Barra de MP adicionada
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

        // Inicializar TextViews
        txt_hp = findViewById(R.id.txt_hp);
        txt_mp = findViewById(R.id.txt_mp);
        txt_nivel = findViewById(R.id.txt_nivel);

        // Inicializar ProgressBars
        hpProgressBar = findViewById(R.id.hp_progress_bar);
        mpProgressBar = findViewById(R.id.mp_progress_bar); // Inicialização da barra de MP

        // Inicializar Botões
        btn_forca = findViewById(R.id.btn_forca);
        btn_vitalidade = findViewById(R.id.btn_vitalidade);
        btn_agilidade = findViewById(R.id.btn_agilidade);
        btn_sentidos = findViewById(R.id.btn_sentidos);
        btn_inteligencia = findViewById(R.id.btn_inteligencia);

        updateNivel();
        calcularStatus();
        atualizarStatusUI();

        // Atualiza o texto inicial dos botões
        updateButtonTexts();

        // Configurar listeners dos botões
        if (GameStateManager.getPontos() > 0) {
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

        menuButtons[2].setOnClickListener(v -> {
            startActivity(new Intent(Status.this, Gps.class));
        });

        // Ações de teste para outros botões
        menuButtons[0].setOnClickListener(v -> showToast("Botão 1"));
        menuButtons[1].setOnClickListener(v -> showToast("Botão 2"));
       // menuButtons[2].setOnClickListener(v -> showToast("Botão 3"));
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
                    button.setVisibility(View.VISIBLE);
                    button.setAlpha(0f);

                    button.post(() -> {
                        float layoutX = button.getX();
                        float layoutY = button.getY();
                        float startTranslationX = toggleX - layoutX;
                        float startTranslationY = toggleY - layoutY;

                        button.setTranslationX(startTranslationX);
                        button.setTranslationY(startTranslationY);

                        ObjectAnimator animX = ObjectAnimator.ofFloat(button, "translationX", startTranslationX, 0f);
                        ObjectAnimator animY = ObjectAnimator.ofFloat(button, "translationY", startTranslationY, 0f);
                        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);

                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(animX, animY, alphaAnim);
                        set.setDuration(350);
                        set.setStartDelay(index * 50);
                        set.start();
                    });
                } else {
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
                    set.setStartDelay(index * 50);
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            button.setVisibility(View.GONE);
                            button.setTranslationX(0f);
                            button.setTranslationY(0f);
                            button.setAlpha(1f);
                        }
                    });
                    set.start();
                }
            } else {
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
        if (btn_forca != null) {
            btn_forca.setText("Força: " + GameStateManager.getForca());
        } else {
            Log.e("Status", "Botão btn_forca não encontrado no layout");
        }
        if (btn_vitalidade != null) {
            btn_vitalidade.setText("Vitalidade: " + GameStateManager.getVitalidade());
        } else {
            Log.e("Status", "Botão btn_vitalidade não encontrado no layout");
        }
        if (btn_agilidade != null) {
            btn_agilidade.setText("Agilidade: " + GameStateManager.getAgilidade());
        } else {
            Log.e("Status", "Botão btn_agilidade não encontrado no layout");
        }
        if (btn_sentidos != null) {
            btn_sentidos.setText("Sentidos: " + GameStateManager.getSentidos());
        } else {
            Log.e("Status", "Botão btn_sentidos não encontrado no layout");
        }
        if (btn_inteligencia != null) {
            btn_inteligencia.setText("Inteligência: " + GameStateManager.getInteligencia());
        } else {
            Log.e("Status", "Botão btn_inteligencia não encontrado no layout");
        }
    }

    private void updateNivel() {
        txt_nivel.setText(" " + GameStateManager.getLevel());
    }

    private void calcularStatus() {
        int vitalidade = GameStateManager.getVitalidade();
        int inteligencia = GameStateManager.getInteligencia();
        int nivel = GameStateManager.getLevel();

        hpi = vitalidade * 100;
        hp = hpi + (hpp + 10 * nivel - 10);

        mpi = inteligencia * 100;
        mp = mpi + (mpp + 10 * nivel - 10);

        // Atualizar a barra de vida
        if (hpProgressBar != null) {
            int maxHp = hpi + (hpp + 10 * nivel - 10);
            hpProgressBar.setMax(maxHp);
            hpProgressBar.setProgress(hp > maxHp ? maxHp : hp);
        }

        // Atualizar a barra de mana
        if (mpProgressBar != null) {
            int maxMp = mpi + (mpp + 10 * nivel - 10);
            mpProgressBar.setMax(maxMp);
            mpProgressBar.setProgress(mp > maxMp ? maxMp : mp);
        }
    }

    private void atualizarStatusUI() {
        if (txt_hp != null) {
            txt_hp.setText("HP: " + hp);
        }
        if (txt_mp != null) {
            txt_mp.setText("MP: " + mp);
        }
        if (hpProgressBar != null) {
            hpProgressBar.setProgress(hp);
        }
        if (mpProgressBar != null) {
            mpProgressBar.setProgress(mp);
        }
    }
}