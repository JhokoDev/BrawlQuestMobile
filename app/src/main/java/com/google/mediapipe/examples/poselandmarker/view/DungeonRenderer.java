package com.google.mediapipe.examples.poselandmarker.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.mediapipe.examples.poselandmarker.DataNest.AppDatabase;
import com.google.mediapipe.examples.poselandmarker.R;
import com.google.mediapipe.examples.poselandmarker.model.DungeonEntity;
import com.google.mediapipe.examples.poselandmarker.model.DungeonType;
import com.google.mediapipe.examples.poselandmarker.model.Tile;

import org.json.JSONArray;

import java.util.Random;

public class DungeonRenderer extends View {
    private static final int WIDTH = 50;  // Largura do mapa em tiles
    private static final int HEIGHT = 50;  // Altura do mapa em tiles
    private static final float TILE_SIZE = 32f;  // Tamanho fixo de cada tile (float para compatibilidade com drawRect)

    private DungeonType type;
    private String rank;
    private Tile[][] map;
    private Random random;
    private String dungeonId;

    public DungeonRenderer(Context context, DungeonType type, String rank, long seed) {
        super(context);
        this.type = type;
        this.rank = rank;
        this.random = new Random(seed);
        this.dungeonId = type.toString() + "_" + seed;
        loadMapAsync();  // Carrega ou gera o mapa de forma assíncrona
    }

    public DungeonRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Método para carregar/gerar o mapa em background
    private void loadMapAsync() {
        new Thread(() -> {
            if (!loadFromStorage(dungeonId)) {
                generateMap();
                saveToStorage(dungeonId);
            }
            // Atualiza a UI na thread principal após o carregamento
            postInvalidate();
        }).start();
    }

    private void generateMap() {
        map = new Tile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                map[x][y] = generateTile(x, y);
            }
        }
        addFixedPath(); // Garante conectividade básica
    }

    private Tile generateTile(int x, int y) {
        Tile.Type tileType;
        int difficultyModifier = getDifficultyModifier();
        switch (type) {
            case CAVERNA:
                tileType = (random.nextInt(10) < (8 - difficultyModifier)) ? Tile.Type.PAREDE : Tile.Type.CHAO;
                break;
            case CAMPO:
                tileType = (random.nextInt(10) < (6 - difficultyModifier)) ? Tile.Type.GRAMA : Tile.Type.AGUA;
                break;
            case CIDADE:
                tileType = (random.nextInt(10) < (5 - difficultyModifier)) ? Tile.Type.EDIFICIO : Tile.Type.RUA;
                break;
            default:
                tileType = Tile.Type.CHAO;
        }
        // Carregar textura do recurso
        Drawable texture = getResources().getDrawable(
                tileType == Tile.Type.PAREDE ? R.drawable.wall :
                        tileType == Tile.Type.CHAO ? R.drawable.floor :
                                tileType == Tile.Type.GRAMA ? R.drawable.grass :
                                        tileType == Tile.Type.AGUA ? R.drawable.water :
                                                tileType == Tile.Type.EDIFICIO ? R.drawable.building :
                                                        tileType == Tile.Type.RUA ? R.drawable.road : R.drawable.floor, null);
        return new Tile(tileType, texture);
    }

    private int getDifficultyModifier() {
        switch (rank) {
            case "S": return 2;
            case "A": return 1;
            case "B": return 1;
            case "C": return 0;
            case "D": return 0;
            case "E": return -1;
            default: return 0;
        }
    }

    private void addFixedPath() {
        // Caminho simples do canto superior esquerdo ao inferior direito
        int x = 0, y = 0;
        while (x < WIDTH - 1 || y < HEIGHT - 1) {
            map[x][y] = new Tile(Tile.Type.CHAO, getResources().getDrawable(R.drawable.floor, null));
            if (x < WIDTH - 1 && random.nextBoolean()) x++;
            else if (y < HEIGHT - 1) y++;
        }
        map[WIDTH - 1][HEIGHT - 1] = new Tile(Tile.Type.CHAO, getResources().getDrawable(R.drawable.floor, null));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Se o mapa ainda não foi carregado, não desenha nada (ou mostre um loading)
        if (map == null) {
            return;
        }
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Drawable texture = map[x][y].getTexture();
                if (texture instanceof BitmapDrawable) {
                    // Redimensionar a textura para TILE_SIZE x TILE_SIZE
                    Bitmap originalBitmap = ((BitmapDrawable) texture).getBitmap();
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) TILE_SIZE, (int) TILE_SIZE, true);
                    canvas.drawBitmap(scaledBitmap, x * TILE_SIZE, y * TILE_SIZE, null);
                } else {
                    // Fallback para cores se a textura não estiver disponível
                    Paint paint = new Paint();
                    switch (map[x][y].getType()) {
                        case PAREDE:
                            paint.setColor(0xFF000000); // Preto
                            break;
                        case CHAO:
                            paint.setColor(0xFF808080); // Cinza
                            break;
                        case GRAMA:
                            paint.setColor(0xFF00FF00); // Verde
                            break;
                        case AGUA:
                            paint.setColor(0xFF00FFFF); // Ciano
                            break;
                        case EDIFICIO:
                            paint.setColor(0xFFFF0000); // Vermelho
                            break;
                        case RUA:
                            paint.setColor(0xFFAAAAAA); // Cinza claro
                            break;
                        default:
                            paint.setColor(0xFFFFFFFF); // Branco
                    }
                    canvas.drawRect(x * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE, (y + 1) * TILE_SIZE, paint);
                }
            }
        }
    }

    public void saveToStorage(String dungeonId) {
        new Thread(() -> {
            try {
                JSONArray jsonArray = new JSONArray();
                for (int x = 0; x < WIDTH; x++) {
                    JSONArray row = new JSONArray();
                    for (int y = 0; y < HEIGHT; y++) {
                        row.put(map[x][y].getType().toString());
                    }
                    jsonArray.put(row);
                }
                DungeonEntity entity = new DungeonEntity();
                entity.id = dungeonId;
                entity.layoutJson = jsonArray.toString();
                entity.generatedDate = System.currentTimeMillis();
                AppDatabase.getInstance(getContext()).dungeonDao().insert(entity);
                Log.d("DungeonRenderer", "Dungeon salva: " + dungeonId);
            } catch (Exception e) {
                Log.e("DungeonRenderer", "Erro ao salvar dungeon: " + e.getMessage());
            }
        }).start();
    }

    public boolean loadFromStorage(String dungeonId) {
        DungeonEntity entity = AppDatabase.getInstance(getContext()).dungeonDao().getDungeon(dungeonId);
        if (entity != null) {
            try {
                JSONArray jsonArray = new JSONArray(((DungeonEntity) entity).layoutJson);
                map = new Tile[WIDTH][HEIGHT];
                for (int x = 0; x < WIDTH; x++) {
                    JSONArray row = jsonArray.getJSONArray(x);
                    for (int y = 0; y < HEIGHT; y++) {
                        Tile.Type type = Tile.Type.valueOf(row.getString(y));
                        Drawable texture = getResources().getDrawable(
                                type == Tile.Type.PAREDE ? R.drawable.wall :
                                        type == Tile.Type.CHAO ? R.drawable.floor :
                                                type == Tile.Type.GRAMA ? R.drawable.grass :
                                                        type == Tile.Type.AGUA ? R.drawable.water :
                                                                type == Tile.Type.EDIFICIO ? R.drawable.building :
                                                                        type == Tile.Type.RUA ? R.drawable.road : R.drawable.floor, null);
                        map[x][y] = new Tile(type, texture);
                    }
                }
                Log.d("DungeonRenderer", "Dungeon carregada: " + dungeonId);
                return true;
            } catch (Exception e) {
                Log.e("DungeonRenderer", "Erro ao carregar dungeon: " + e.getMessage());
            }
        }
        return false;
    }
}