package com.google.mediapipe.examples.poselandmarker.detectors

import android.util.Log
import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.PI
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.examples.poselandmarker.GameStateManager

data class Vector3D(val x: Float, val y: Float, val z: Float)

class PushUpDetector {
    private val TAG = "PushUpDetector"

    private enum class State { ALTO, BAIXO, NENHUM }

    private var lastStableState = State.NENHUM
    private var tempState = State.NENHUM
    private var stateCounter = 0
    public var Cuntflex = 0

    private var readyToCount = false

    var pushUpCount = GameStateManager.getFlex()
        private set

    // Thresholds ajustados com base nos valores observados
    private val EXTENDED_THRESHOLD = 55f // Para ALTO (braços estendidos)
    private val BENT_THRESHOLD = 60f // Para BAIXO (braços dobrados)
    private val STABLE_FRAME_THRESHOLD = 3

    fun detectPushUp(result: PoseLandmarkerResult): Int {
        if (result.worldLandmarks().isEmpty()) return pushUpCount

        val landmarks = result.worldLandmarks()[0]

        val leftShoulder = landmarks[11]
        val leftElbow = landmarks[13]
        val leftWrist = landmarks[15]

        val leftUpperArm = toVector(leftShoulder, leftElbow)
        val leftForearm = toVector(leftElbow, leftWrist)

        val leftAngle = angleBetween(leftUpperArm, leftForearm)

        // Classificação dos estados com base nos novos thresholds
        val currentState = when {
            leftAngle < EXTENDED_THRESHOLD -> State.ALTO
            leftAngle > BENT_THRESHOLD -> State.BAIXO
            else -> State.NENHUM
        }

        // Log para depuração
        Log.i(TAG, "Ângulo Esquerdo: %.2f°, Estado: %s".format(leftAngle, currentState))

        if (currentState == tempState) {
            stateCounter++
        } else {
            tempState = currentState
            stateCounter = 1
        }

        if (stateCounter >= STABLE_FRAME_THRESHOLD && currentState != lastStableState) {
            if (currentState == State.BAIXO) {
                readyToCount = true
            }
            if (currentState == State.ALTO && readyToCount) {
                pushUpCount++
                readyToCount = false
                Log.i(TAG, "Flexão contada! Total: $pushUpCount")
                GameStateManager.setFlex(pushUpCount)
                GameStateManager.saveState()
            }
            lastStableState = currentState
        }
        Cuntflex = pushUpCount
        com.google.mediapipe.examples.poselandmarker.session.PushUpSession.pushUpCount = pushUpCount

        return pushUpCount
    }

    private fun toVector(p1: Landmark, p2: Landmark): Vector3D {
        return Vector3D(p2.x() - p1.x(), p2.y() - p1.y(), p2.z() - p1.z())
    }

    private fun angleBetween(v1: Vector3D, v2: Vector3D): Float {
        val dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
        val mag1 = sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z)
        val mag2 = sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z)
        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return acos(cosTheta) * (180f / PI.toFloat())
    }

    fun reset() {
        lastStableState = State.NENHUM
        tempState = State.NENHUM
        stateCounter = 0
        readyToCount = false
    }
}