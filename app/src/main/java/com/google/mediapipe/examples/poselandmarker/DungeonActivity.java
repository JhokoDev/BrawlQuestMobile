package com.google.mediapipe.examples.poselandmarker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mediapipe.examples.poselandmarker.model.DungeonType;
import com.google.mediapipe.examples.poselandmarker.view
        .DungeonRenderer;

public class DungeonActivity extends AppCompatActivity {
    public static final String EXTRA_PORTAL_NAME = "portal_name";
    public static final String EXTRA_PORTAL_TYPE = "portal_type";
    public static final String EXTRA_PORTAL_RANK = "portal_rank";
    public static final String EXTRA_PORTAL_SEED = "portal_seed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dungeon);

        String portalName = getIntent().getStringExtra(EXTRA_PORTAL_NAME);
        DungeonType dungeonType = (DungeonType) getIntent().getSerializableExtra(EXTRA_PORTAL_TYPE);
        String rank = getIntent().getStringExtra(EXTRA_PORTAL_RANK);
        long seed = getIntent().getLongExtra(EXTRA_PORTAL_SEED, 0);

        FrameLayout container = findViewById(R.id.dungeon_container);
        DungeonRenderer renderer = new DungeonRenderer(this, dungeonType, rank, seed);
        container.addView(renderer);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }
}