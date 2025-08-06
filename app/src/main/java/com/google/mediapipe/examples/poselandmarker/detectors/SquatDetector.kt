package com.google.mediapipe.examples.poselandmarker.detectors

import android.util.Log
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.examples.poselandmarker.detectors.Vector3D
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.PI
import com.google.mediapipe.examples.poselandmarker.GameStateManager

enum class State { HIGH, LOW, NONE }

class SquatDetector {
    private val TAG = "SquatDetector"

    private var lastStableState = State.NONE
    private var tempState = State.NONE
    private var stateCounter = 0
    private var readyToCount = false
    var squatCount = GameStateManager.getAgac()
        private set

    private val HIGH_ANGLE_THRESHOLD = 140f
    private val LOW_ANGLE_THRESHOLD = 90f
    private val STABLE_FRAME_THRESHOLD = 3

    fun detectSquat(result: PoseLandmarkerResult): Int {
        if (result.worldLandmarks().isEmpty()) return squatCount

        val landmarks = result.worldLandmarks()[0]

        val leftHip = landmarks[23]
        val leftKnee = landmarks[25]
        val leftAnkle = landmarks[27]

        val thigh = toVector(leftHip, leftKnee)
        val lowerLeg = toVector(leftKnee, leftAnkle)

        val phi = angleBetween(thigh, lowerLeg)
        val theta = 180f - phi

        val currentState = when {
            theta > HIGH_ANGLE_THRESHOLD -> State.HIGH
            theta < LOW_ANGLE_THRESHOLD -> State.LOW
            else -> State.NONE
        }

        Log.i(TAG, String.format("Ângulo do Joelho (theta): %.2f°, Estado: %s", theta, currentState))

        if (currentState == tempState) {
            stateCounter++
        } else {
            tempState = currentState
            stateCounter = 1
        }

        if (stateCounter >= STABLE_FRAME_THRESHOLD && currentState != lastStableState) {
            if (currentState == State.LOW) {
                readyToCount = true
            }
            if (currentState == State.HIGH && readyToCount) {
                squatCount++
                readyToCount = false
                Log.i(TAG, "Agachamento contado! Total: $squatCount")
                GameStateManager.setAgac(squatCount)
                GameStateManager.saveState()
            }
            lastStableState = currentState
        }

        return squatCount
    }

    private fun toVector(p1: Landmark, p2: Landmark): Vector3D {
        return Vector3D(p2.x() - p1.x(), p2.y() - p1.y(), p2.z() - p1.z())
    }

    private fun angleBetween(v1: Vector3D, v2: Vector3D): Float {
        val dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
        val mag1 = sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z)
        val mag2 = sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z)
        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return (acos(cosTheta) * (180f / PI)).toFloat()
    }

    fun reset() {
        lastStableState = State.NONE
        tempState = State.NONE
        stateCounter = 0
        readyToCount = false
    }
}