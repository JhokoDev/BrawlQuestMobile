package com.google.mediapipe.examples.poselandmarker.detectors

import android.util.Log
import com.google.mediapipe.examples.poselandmarker.GameStateManager
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.sqrt

class AbdominalDetector {
    private val TAG = "AbdominalDetector"

    private var lastStableState = State.NONE
    private var tempState = State.NONE
    private var stateCounter = 0
    private var readyToCount = false
    var abdominalCount = GameStateManager.getAbdo()
        private set

    private val HIGH_D = 0.5f // Distância para "alto"
    private val LOW_D = 0.5f  // Distância para "baixo"
    private val STABLE_FRAMES = 3 // Frames estáveis para transição

    fun detectAbdominal(result: PoseLandmarkerResult): Int {
        if (result.worldLandmarks().isEmpty()) return abdominalCount

        val landmarks = result.worldLandmarks()[0]

        // Landmarks relevantes para abdominais
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]

        // Calcular pontos médios
        val shoulderMid = Vector3D(
            (leftShoulder.x() + rightShoulder.x()) / 2,
            (leftShoulder.y() + rightShoulder.y()) / 2,
            (leftShoulder.z() + rightShoulder.z()) / 2
        )
        val hipMid = Vector3D(
            (leftHip.x() + rightHip.x()) / 2,
            (leftHip.y() + rightHip.y()) / 2,
            (leftHip.z() + rightHip.z()) / 2
        )

        // Calcular distância entre ombros e quadris
        val dx = shoulderMid.x - hipMid.x
        val dy = shoulderMid.y - hipMid.y
        val dz = shoulderMid.z - hipMid.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        // Determinar o estado atual com base na distância
        val currentState = when {
            distance > HIGH_D -> State.HIGH
            distance < LOW_D -> State.LOW
            else -> State.NONE
        }

        // Log para depuração
        Log.i(TAG, "Distância: %.2f, Estado: %s".format(distance, currentState))

        // Atualizar tempState e stateCounter
        if (currentState == tempState) {
            stateCounter++
        } else {
            tempState = currentState
            stateCounter = 1
        }

        // Verificar transição estável
        if (stateCounter >= STABLE_FRAMES && currentState != lastStableState) {
            if (currentState == State.LOW) {
                readyToCount = true
            }
            if (currentState == State.HIGH && readyToCount) {
                abdominalCount++
                readyToCount = false
                Log.i(TAG, "Abdominal contado! Total: $abdominalCount")
                GameStateManager.setAbdo(abdominalCount)
                GameStateManager.saveState()
            }
            lastStableState = currentState
        }

        return abdominalCount
    }

    fun reset() {
        lastStableState = State.NONE
        tempState = State.NONE
        stateCounter = 0
        readyToCount = false
    }
}

