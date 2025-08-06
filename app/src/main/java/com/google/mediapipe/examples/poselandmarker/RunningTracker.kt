package com.google.mediapipe.examples.poselandmarker

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.poselandmarker.GameStateManager

class RunningTracker : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount: Long = 0
    private val strideLength: Float = 0.77f // Calibrado em metros

    private lateinit var tvSteps: TextView
    private lateinit var tvDistance: TextView
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running_tracker)

        // Inicializa o GameStateManager
        GameStateManager.initialize(this)

        // Inicializa os elementos da UI
        tvSteps = findViewById(R.id.tv_steps)
        tvDistance = findViewById(R.id.tv_distance)
        btnReset = findViewById(R.id.btn_reset)

        // Carrega o stepCount e atualiza a UI
        stepCount = GameStateManager.getStepCount()
        val distanceKm = GameStateManager.getCorr()
        tvSteps.text = "Passos: $stepCount"
        tvDistance.text = String.format("Distância: %.2f km", distanceKm)

        // Configura o botão de reset
        btnReset.setOnClickListener {
            resetSteps()
            tvSteps.text = "Passos: 0"
            tvDistance.text = "Distância: 0.0 km"
            GameStateManager.setCorr(0f)
            GameStateManager.setStepCount(0)
        }

        // Inicializa o SensorManager
        sensorManager = getSystemService(SensorManager::class.java)
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Verifica permissão em tempo de execução
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 100)
        }

        // Verifica se o sensor está disponível
        if (stepSensor == null) {
            Toast.makeText(this, "Sensor de passos não disponível", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão concedida", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissão negada, o sensor não funcionará", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        // Salva o estado ao pausar a atividade
        GameStateManager.setStepCount(stepCount)
        GameStateManager.setCorr((stepCount * strideLength) / 1000)
        GameStateManager.saveState()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++
            val distanceKm = (stepCount * strideLength) / 1000
            tvSteps.text = "Passos: $stepCount"
            tvDistance.text = String.format("Distância: %.2f km", distanceKm)
            GameStateManager.setCorr(distanceKm)
            GameStateManager.setStepCount(stepCount)
            GameStateManager.saveState()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun resetSteps() {
        stepCount = 0
    }
}