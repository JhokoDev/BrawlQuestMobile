package com.google.mediapipe.examples.poselandmarker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import java.util.Calendar;
import com.google.mediapipe.examples.poselandmarker.GameStateManager;

public class FortalecimentoDoCorpo extends AppCompatActivity {

    private Button btnFlexoes, btnAbdominais, btnAgachamentos, btnCorrer, btnBack, btnReco2, btnAtalho;
    private ImageButton[] menuButtons; // Botões btn1 a btn7
    private ImageButton btnToggleMenu; // btn_back_main
    private boolean areButtonsVisible = false; // Controla visibilidade dos botões de menu
    private int flex = 0;
    private int abdo = 0;
    private int agac = 0;
    private float corr = 0;
    private int meta;
    private int metaC = 1;
    private int metaXp = 0;
    private boolean recs;
    private static final String LAST_REWARD_DATE_KEY = "lastRewardDate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortalecimento_do_corpo);

        // Inicializa o GameStateManager
        GameStateManager.initialize(this);
        atualizarMeta();
        // Inicializa os botões existentes
        btnFlexoes = findViewById(R.id.btn_flexoes);
        btnAbdominais = findViewById(R.id.btn_abdominais);
        btnAgachamentos = findViewById(R.id.btn_agachamentos);
        btnCorrer = findViewById(R.id.btn_correr);
        btnBack = findViewById(R.id.btn_back);
        btnReco2 = findViewById(R.id.btn_reco2);
        btnAtalho = findViewById(R.id.btn_atalho);

        // Inicializa os botões de menu (btn1 a btn7)
        menuButtons = new ImageButton[]{
                findViewById(R.id.btn1),
                findViewById(R.id.btn2),
                findViewById(R.id.btn3),
                findViewById(R.id.btn4),
                findViewById(R.id.btn5),
                findViewById(R.id.btn6),
                findViewById(R.id.btn7)
        };

        // Inicializa o botão de toggle (btn_back_main)
        btnToggleMenu = findViewById(R.id.btn_back_main);

        // Oculta botões de menu inicialmente sem animação
        setButtonsVisibility(View.GONE, false);

        // Carrega os dados salvos
        loadExercicios();

        // Verifica se é um novo dia
        if (isNewDay()) {
            verificarPenalidade();
            resetMissoesDiarias();
        }

        // Atualiza o texto dos botões
        updateButtonTexts();

        // Configura o toggle do menu
        btnToggleMenu.setOnClickListener(v -> {
            areButtonsVisible = !areButtonsVisible;
            setButtonsVisibility(areButtonsVisible ? View.VISIBLE : View.GONE, true);
        });

        // Configura os cliques dos botões existentes
        btnFlexoes.setOnClickListener(v -> {
            Intent intent = new Intent(FortalecimentoDoCorpo.this, MainActivity.class);
            startActivity(intent);
            GameStateManager.setMissao("Flexao");
        });

        btnAbdominais.setOnClickListener(v -> {
            Intent intent = new Intent(FortalecimentoDoCorpo.this, MainActivity.class);
            startActivity(intent);
            GameStateManager.setMissao("Abdominal");
        });

        btnAgachamentos.setOnClickListener(v -> {
            Intent intent = new Intent(FortalecimentoDoCorpo.this, MainActivity.class);
            startActivity(intent);
            GameStateManager.setMissao("Agachamento");
        });

        btnCorrer.setOnClickListener(v -> {
            Intent intent = new Intent(FortalecimentoDoCorpo.this, RunningTracker.class);
            startActivity(intent);
            GameStateManager.setMissao("");
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(FortalecimentoDoCorpo.this, Missao.class);
            startActivity(intent);
            finish();
            GameStateManager.setMissao("");
        });

        recs = true;

        if ((corr >= 2 && flex == 40 && agac == 40 && abdo == 40 && recs) ||
                (corr >= 1 && flex == 20 && agac == 20 && abdo == 20 && recs)) {
            btnReco2.setVisibility(View.VISIBLE);
        }

        // Coleta de recompensa
        btnReco2.setOnClickListener(v -> {
            if (!canCollectReward()) {
                showToast("Você já coletou a recompensa hoje! Tente novamente amanhã.");
                return;
            }

            if ((corr >= 2 && flex == 40 && agac == 40 && abdo == 40 && recs) ||
                    (corr >= 1 && flex == 20 && agac == 20 && abdo == 20 && recs)) {
                GameStateManager.setPontos(GameStateManager.getPontos() + 3);
                GameStateManager.adicionarXp(200);
                recs = false;
                saveRewardDate();
                saveExercicios();
                updateButtonTexts();
                showToast("Recompensa coletada com sucesso!");
            } else {
                showToast("Complete todas as missões para coletar a recompensa!");
            }
        });

        btnAtalho.setOnClickListener(v -> {
            flex = 40;
            abdo = 40;
            agac = 40;
            corr = 2;
            recs = true;
            saveExercicios();
            updateButtonTexts();
        });

        menuButtons[3].setOnClickListener(v -> {
            Intent intent = new Intent(FortalecimentoDoCorpo.this, Status.class);
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

    private void updateButtonTexts() {
        metaXp = (GameStateManager.getLevel() * GameStateManager.getLevel()) * 100;
        if (GameStateManager.getXp() >= metaXp) {
            GameStateManager.setLevel(GameStateManager.getLevel() + 1);
        }

        String est1 = (flex >= meta) ? "COMPLETO" : "INCOMPLETO";
        String est2 = (abdo >= meta) ? "COMPLETO" : "INCOMPLETO";
        String est3 = (agac >= meta) ? "COMPLETO" : "INCOMPLETO";
        String est4 = (corr >= metaC) ? "COMPLETO" : "INCOMPLETO";

        btnFlexoes.setText("FLEXÕES [" + est1 + "] [" + flex + "/" + meta + "]");
        btnAbdominais.setText("ABDOMINAIS [" + est2 + "] [" + abdo + "/" + meta + "]");
        btnAgachamentos.setText("AGACHAMENTOS [" + est3 + "] [" + agac + "/" + meta + "]");
        btnCorrer.setText("CORRER [" + est4 + "] [" + GameStateManager.getCorr() + "/" + metaC + "Km]");

        btnReco2.setVisibility(((corr == 2 && flex == 40 && agac == 40 && abdo == 40 && recs) ||
                (corr == 1 && flex == 20 && agac == 20 && abdo == 20 && recs)) ? View.VISIBLE : View.GONE);
    }

    private void loadExercicios() {
        flex = GameStateManager.getFlex();
        abdo = GameStateManager.getAbdo();
        agac = GameStateManager.getAgac();
        corr = GameStateManager.getCorr();
        recs = true;
    }

    private void saveExercicios() {
        GameStateManager.setFlex(flex);
        GameStateManager.setAbdo(abdo);
        GameStateManager.setAgac(agac);
        GameStateManager.setCorr(corr);
    }

    private boolean isNewDay() {
        Calendar currentCalendar = Calendar.getInstance();
        String currentDate = String.format("%d-%02d-%02d",
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH) + 1,
                currentCalendar.get(Calendar.DAY_OF_MONTH));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String lastResetDate = prefs.getString("lastResetDate", "");
        if (!currentDate.equals(lastResetDate)) {
            prefs.edit().putString("lastResetDate", currentDate).apply();
            recs = true; // Reseta a flag de recompensa para permitir nova coleta
            return true;
        }
        return false;
    }

    private void resetMissoesDiarias() {
        flex = 0;
        abdo = 0;
        agac = 0;
        corr = 0;
        recs = true; // Garante que a recompensa pode ser coletada no novo dia
        saveExercicios();
    }

    private void verificarPenalidade() {
        boolean missoesCumpridas = (flex >= meta && abdo >= meta && agac >= meta && corr >= metaC);
        if (!missoesCumpridas) {
            GameStateManager.setForca(Math.max(0, GameStateManager.getForca() - 1));
            GameStateManager.setAgilidade(Math.max(0, GameStateManager.getAgilidade() - 1));
            GameStateManager.setSentidos(Math.max(0, GameStateManager.getSentidos() - 1));
            GameStateManager.setVitalidade(Math.max(0, GameStateManager.getVitalidade() - 1));
            GameStateManager.setInteligencia(Math.max(0, GameStateManager.getInteligencia() - 1));
            GameStateManager.saveAttributes();
        }
    }

    private boolean canCollectReward() {
        Calendar currentCalendar = Calendar.getInstance();
        String currentDate = String.format("%d-%02d-%02d",
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH) + 1,
                currentCalendar.get(Calendar.DAY_OF_MONTH));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String lastRewardDate = prefs.getString(LAST_REWARD_DATE_KEY, "");

        // Se a data da última coleta for diferente da data atual, a coleta é permitida
        return !currentDate.equals(lastRewardDate);
    }

    private void saveRewardDate() {
        Calendar currentCalendar = Calendar.getInstance();
        String currentDate = String.format("%d-%02d-%02d",
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH) + 1,
                currentCalendar.get(Calendar.DAY_OF_MONTH));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(LAST_REWARD_DATE_KEY, currentDate).apply();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void atualizarMeta() {
        int almento = GameStateManager.getLevel() / 2; // Divisão inteira para incrementar a cada 2 níveis
        meta = 20 + almento;
    }
}