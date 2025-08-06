package com.google.mediapipe.examples.poselandmarker;

import android.content.Context;
import android.content.SharedPreferences;

public class GameStateManager {

    private static final String PREFS_NAME = "GameStatePreferences";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_XP = "xp";
    private static final String KEY_PONTOS = "pontos";
    private static final String KEY_FORCA = "forca";
    private static final String KEY_AGILIDADE = "agilidade";
    private static final String KEY_SENTIDOS = "sentidos";
    private static final String KEY_VITALIDADE = "vitalidade";
    private static final String KEY_INTELIGENCIA = "inteligencia";
    private static final String KEY_MISSAO = "missao";
    private static final String KEY_FLEX = "flex";
    private static final String KEY_ABDO = "abdo";
    private static final String KEY_AGAC = "agac";
    private static final String KEY_CORR = "corr";
    private static final String KEY_STEP_COUNT = "step_count";

    private static int level = 1;
    private static int xp = 0;
    private static int pontos = 0;
    private static int forca = 1;
    private static int agilidade = 1;
    private static int sentidos = 1;
    private static int vitalidade = 1;
    private static int inteligencia = 1;
    private static String missao = "";
    private static int flex = 0;
    private static int abdo = 0;
    private static int agac = 0;
    private static float corr = 0;
    private static long stepCount = 0;

    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;

    public static void initialize(Context context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            editor = prefs.edit();
            loadState();
        }
    }

    private static void loadState() {
        if (prefs != null) {
            level = prefs.getInt(KEY_LEVEL, 1);
            xp = prefs.getInt(KEY_XP, 0);
            pontos = prefs.getInt(KEY_PONTOS, 0);
            forca = prefs.getInt(KEY_FORCA, 1);
            agilidade = prefs.getInt(KEY_AGILIDADE, 1);
            sentidos = prefs.getInt(KEY_SENTIDOS, 1);
            vitalidade = prefs.getInt(KEY_VITALIDADE, 1);
            inteligencia = prefs.getInt(KEY_INTELIGENCIA, 1);
            missao = prefs.getString(KEY_MISSAO, "");
            flex = prefs.getInt(KEY_FLEX, 0);
            abdo = prefs.getInt(KEY_ABDO, 0);
            agac = prefs.getInt(KEY_AGAC, 0);
            corr = prefs.getFloat(KEY_CORR, 0);
            stepCount = prefs.getLong(KEY_STEP_COUNT, 0);
        }
    }

    public static void saveState() {
        if (editor != null) {
            editor.putInt(KEY_LEVEL, level);
            editor.putInt(KEY_XP, xp);
            editor.putInt(KEY_PONTOS, pontos);
            editor.putInt(KEY_FORCA, forca);
            editor.putInt(KEY_AGILIDADE, agilidade);
            editor.putInt(KEY_SENTIDOS, sentidos);
            editor.putInt(KEY_VITALIDADE, vitalidade);
            editor.putInt(KEY_INTELIGENCIA, inteligencia);
            editor.putString(KEY_MISSAO, missao);
            editor.putInt(KEY_FLEX, flex);
            editor.putInt(KEY_ABDO, abdo);
            editor.putInt(KEY_AGAC, agac);
            editor.putFloat(KEY_CORR, corr);
            editor.putLong(KEY_STEP_COUNT, stepCount);
            editor.apply();
        }
    }

    // Métodos getters
    public static int getLevel() { return level; }
    public static int getXp() { return xp; }
    public static int getPontos() { return pontos; }
    public static int getForca() { return forca; }
    public static int getAgilidade() { return agilidade; }
    public static int getSentidos() { return sentidos; }
    public static int getVitalidade() { return vitalidade; }
    public static int getInteligencia() { return inteligencia; }
    public static String getMissao() { return missao; }
    public static int getFlex() { return flex; }
    public static int getAbdo() { return abdo; }
    public static int getAgac() { return agac; }
    public static float getCorr() { return corr; }
    public static long getStepCount() { return stepCount; }

    // Métodos setters
    public static void setLevel(int level) { GameStateManager.level = level; saveState(); }
    public static void setXp(int xp) { GameStateManager.xp = xp; saveState(); }
    public static void setPontos(int pontos) { GameStateManager.pontos = pontos; saveState(); }
    public static void setForca(int forca) { GameStateManager.forca = forca; saveState(); }
    public static void setAgilidade(int agilidade) { GameStateManager.agilidade = agilidade; saveState(); }
    public static void setSentidos(int sentidos) { GameStateManager.sentidos = sentidos; saveState(); }
    public static void setVitalidade(int vitalidade) { GameStateManager.vitalidade = vitalidade; saveState(); }
    public static void setInteligencia(int inteligencia) { GameStateManager.inteligencia = inteligencia; saveState(); }
    public static void setMissao(String missao) { GameStateManager.missao = missao; saveState(); }
    public static void setFlex(int flex) { GameStateManager.flex = flex; saveState(); }
    public static void setAbdo(int abdo) { GameStateManager.abdo = abdo; saveState(); }
    public static void setAgac(int agac) { GameStateManager.agac = agac; saveState(); }
    public static void setCorr(float corr) { GameStateManager.corr = corr; saveState(); }
    public static void setStepCount(long stepCount) { GameStateManager.stepCount = stepCount; saveState(); }

    public static void saveAttributes() {
        saveState();
    }

    // Cálculo de XP necessário para o próximo nível (crescimento exponencial)
    public static int getXpNecessarioParaProximoNivel() {
        return (int) (100 * Math.pow(1.1, level - 1));
    }

    // Adiciona XP e verifica subida de nível automática
    public static void adicionarXp(int quantidade) {
        System.out.println("XP: Necessario antes do level: "+ getXpNecessarioParaProximoNivel());
        xp += quantidade;

        while (xp >= getXpNecessarioParaProximoNivel()) {
            xp -= getXpNecessarioParaProximoNivel();
            level++;
            pontos += 3;
            System.out.println("XP: Necessario depois do level: " + getXpNecessarioParaProximoNivel());
        }

        saveState();
    }
}