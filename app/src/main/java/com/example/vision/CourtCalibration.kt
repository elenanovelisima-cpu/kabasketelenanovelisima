package com.example.vision

import kotlin.math.abs

enum class CourtPreset(val label: String, val shortLabel: String) {
    FRONTAL("Frontal (0°)", "FRONTAL 🎯"),
    SIDE_LEFT("Lateral Izquierdo (-30°)", "LAT. IZQ ◀"),
    SIDE_RIGHT("Lateral Derecho (+30°)", "LAT. DER ▶")
}

data class CourtKeyPoint(
    val id: String,
    val name: String,
    var xNorm: Float, // 0..1 en pantalla (coordenada horizontal)
    var yNorm: Float, // 0..1 en pantalla (coordenada vertical)
    val courtX: Float, // 0..1 en media cancha real (0 = banda izq, 0.5 = centro aro, 1 = banda der)
    val courtY: Float  // 0..1 en media cancha real (0 = línea de fondo, 0.60 = cabecera triple, 1 = mediocampo)
)

data class CourtCalibration(
    val points: List<CourtKeyPoint> = createDefaultCourtPoints(CourtPreset.FRONTAL),
    val isCalibrated: Boolean = false,
    val preset: CourtPreset = CourtPreset.FRONTAL
) {
    fun getPoint(id: String): CourtKeyPoint? = points.find { it.id == id }

    fun updatePoint(id: String, nx: Float, ny: Float): CourtCalibration {
        val updated = points.map { pt ->
            if (pt.id == id) pt.copy(xNorm = nx.coerceIn(0.02f, 0.98f), yNorm = ny.coerceIn(0.05f, 0.98f))
            else pt
        }
        return copy(points = updated, isCalibrated = true)
    }

    /**
     * Determina si la posición de los pies del jugador en pantalla corresponde a un TRIPLE (3PT).
     * En perspectiva de cámara, la línea de 3 puntos forma un arco en la parte inferior/media del encuadre.
     * Si los pies están más abajo (mayor Y) que el arco interpolado en ese X, el tiro está detrás de la línea de 3 puntos.
     */
    fun isThreePointer(screenX: Float, screenY: Float): Boolean {
        val threePoints = listOfNotNull(
            getPoint("3pt_corner_l"),
            getPoint("3pt_wing_l"),
            getPoint("3pt_top"),
            getPoint("3pt_wing_r"),
            getPoint("3pt_corner_r")
        ).sortedBy { it.xNorm }

        if (threePoints.size < 3) {
            // Fallback por defecto si no están los puntos
            return screenY > 0.70f
        }

        val clampedX = screenX.coerceIn(0f, 1f)

        // Interpolación por tramos del arco de triple
        val thresholdY = when {
            clampedX <= threePoints.first().xNorm -> threePoints.first().yNorm
            clampedX >= threePoints.last().xNorm -> threePoints.last().yNorm
            else -> {
                var yVal = 0.75f
                for (i in 0 until threePoints.size - 1) {
                    val p1 = threePoints[i]
                    val p2 = threePoints[i + 1]
                    if (clampedX in p1.xNorm..p2.xNorm) {
                        val fraction = if (abs(p2.xNorm - p1.xNorm) > 0.001f) {
                            (clampedX - p1.xNorm) / (p2.xNorm - p1.xNorm)
                        } else 0f
                        yVal = p1.yNorm + fraction * (p2.yNorm - p1.yNorm)
                        break
                    }
                }
                yVal
            }
        }

        // Si los pies están más abajo o igual que la línea de 3PT en la imagen, es triple
        return screenY >= (thresholdY - 0.02f)
    }

    /**
     * Mapea las coordenadas en pantalla (screenX, screenY) a la vista superior 2D de media cancha (0..1, 0..1).
     */
    fun mapScreenToCourt(screenX: Float, screenY: Float): Pair<Float, Float> {
        val keyBaseL = getPoint("key_base_l") ?: CourtKeyPoint("key_base_l", "", 0.38f, 0.44f, 0.36f, 0.02f)
        val keyBaseR = getPoint("key_base_r") ?: CourtKeyPoint("key_base_r", "", 0.62f, 0.44f, 0.64f, 0.02f)
        val threeTop = getPoint("3pt_top") ?: CourtKeyPoint("3pt_top", "", 0.50f, 0.86f, 0.50f, 0.60f)

        val yBase = (keyBaseL.yNorm + keyBaseR.yNorm) / 2f
        val y3pt = threeTop.yNorm

        // Profundidad de la cancha: 0 en la línea de fondo, 0.60 en el triple, 1.0 en mediocampo
        val depthSpan = (y3pt - yBase).coerceAtLeast(0.15f)
        val relDepth = ((screenY - yBase) / depthSpan).coerceIn(0f, 1.6f)
        val courtY = (relDepth * 0.60f).coerceIn(0.04f, 0.95f)

        // Anchura en función de la profundidad proyectada
        val wingL = getPoint("3pt_wing_l") ?: CourtKeyPoint("3pt_wing_l", "", 0.22f, 0.74f, 0.20f, 0.35f)
        val wingR = getPoint("3pt_wing_r") ?: CourtKeyPoint("3pt_wing_r", "", 0.78f, 0.74f, 0.80f, 0.35f)

        val leftX = keyBaseL.xNorm + (wingL.xNorm - keyBaseL.xNorm) * relDepth.coerceIn(0f, 1f)
        val rightX = keyBaseR.xNorm + (wingR.xNorm - keyBaseR.xNorm) * relDepth.coerceIn(0f, 1f)
        val widthSpan = (rightX - leftX).coerceAtLeast(0.15f)

        val courtX = (0.36f + ((screenX - leftX) / widthSpan) * (0.64f - 0.36f) * 2.8f).coerceIn(0.05f, 0.95f)

        return Pair(courtX, courtY)
    }

    /**
     * Devuelve la zona de tiro descriptiva para las estadísticas.
     */
    fun getShotZone(screenX: Float, screenY: Float): String {
        val isThree = isThreePointer(screenX, screenY)
        return if (isThree) {
            when {
                screenX < 0.28f -> "Triple Esquina / Lateral Izq"
                screenX > 0.72f -> "Triple Esquina / Lateral Der"
                else -> "Triple Cabecera (3PT Top)"
            }
        } else {
            val ftL = getPoint("ft_line_l")
            val ftR = getPoint("ft_line_r")
            val ftY = ftL?.yNorm ?: 0.64f
            val isInsideKey = screenX in (0.32f..0.68f) && screenY < ftY
            if (isInsideKey) {
                "Pintura / Bandeja"
            } else {
                "Media Distancia"
            }
        }
    }

    companion object {
        fun createDefaultCourtPoints(preset: CourtPreset): List<CourtKeyPoint> {
            return when (preset) {
                CourtPreset.FRONTAL -> listOf(
                    CourtKeyPoint("3pt_corner_l", "3PT Esquina Izq", 0.12f, 0.56f, 0.08f, 0.05f),
                    CourtKeyPoint("3pt_wing_l", "3PT wing L", 0.22f, 0.74f, 0.20f, 0.35f),
                    CourtKeyPoint("3pt_top", "3PT top", 0.50f, 0.86f, 0.50f, 0.60f),
                    CourtKeyPoint("3pt_wing_r", "3PT wing R", 0.78f, 0.74f, 0.80f, 0.35f),
                    CourtKeyPoint("3pt_corner_r", "3PT Esquina Der", 0.88f, 0.56f, 0.92f, 0.05f),
                    CourtKeyPoint("key_base_l", "Zona Base Izq", 0.38f, 0.44f, 0.36f, 0.02f),
                    CourtKeyPoint("key_base_r", "Zona Base Der", 0.62f, 0.44f, 0.64f, 0.02f),
                    CourtKeyPoint("ft_line_l", "Tiro Libre Izq", 0.34f, 0.64f, 0.36f, 0.38f),
                    CourtKeyPoint("ft_line_r", "Tiro Libre Der", 0.66f, 0.64f, 0.64f, 0.38f)
                )
                CourtPreset.SIDE_LEFT -> listOf(
                    // Cámara ubicada en la banda/lateral izquierda, canasta a la derecha
                    CourtKeyPoint("3pt_corner_l", "3PT Esquina Izq", 0.05f, 0.78f, 0.08f, 0.05f),
                    CourtKeyPoint("3pt_wing_l", "3PT wing L", 0.18f, 0.84f, 0.20f, 0.35f),
                    CourtKeyPoint("3pt_top", "3PT top", 0.46f, 0.88f, 0.50f, 0.60f),
                    CourtKeyPoint("3pt_wing_r", "3PT wing R", 0.74f, 0.70f, 0.80f, 0.35f),
                    CourtKeyPoint("3pt_corner_r", "3PT Esquina Der", 0.86f, 0.50f, 0.92f, 0.05f),
                    CourtKeyPoint("key_base_l", "Zona Base Izq", 0.46f, 0.42f, 0.36f, 0.02f),
                    CourtKeyPoint("key_base_r", "Zona Base Der", 0.68f, 0.40f, 0.64f, 0.02f),
                    CourtKeyPoint("ft_line_l", "Tiro Libre Izq", 0.38f, 0.62f, 0.36f, 0.38f),
                    CourtKeyPoint("ft_line_r", "Tiro Libre Der", 0.65f, 0.60f, 0.64f, 0.38f)
                )
                CourtPreset.SIDE_RIGHT -> listOf(
                    // Cámara ubicada en la banda/lateral derecha, canasta a la izquierda
                    CourtKeyPoint("3pt_corner_l", "3PT Esquina Izq", 0.14f, 0.50f, 0.08f, 0.05f),
                    CourtKeyPoint("3pt_wing_l", "3PT wing L", 0.26f, 0.70f, 0.20f, 0.35f),
                    CourtKeyPoint("3pt_top", "3PT top", 0.54f, 0.88f, 0.50f, 0.60f),
                    CourtKeyPoint("3pt_wing_r", "3PT wing R", 0.82f, 0.84f, 0.80f, 0.35f),
                    CourtKeyPoint("3pt_corner_r", "3PT Esquina Der", 0.95f, 0.78f, 0.92f, 0.05f),
                    CourtKeyPoint("key_base_l", "Zona Base Izq", 0.32f, 0.40f, 0.36f, 0.02f),
                    CourtKeyPoint("key_base_r", "Zona Base Der", 0.54f, 0.42f, 0.64f, 0.02f),
                    CourtKeyPoint("ft_line_l", "Tiro Libre Izq", 0.35f, 0.60f, 0.36f, 0.38f),
                    CourtKeyPoint("ft_line_r", "Tiro Libre Der", 0.62f, 0.62f, 0.64f, 0.38f)
                )
            }
        }

        fun adaptToHoop(points: List<CourtKeyPoint>, hoopNx: Float, hoopNy: Float): List<CourtKeyPoint> {
            val deltaX = hoopNx - 0.50f
            val baseShiftY = (hoopNy - 0.28f).coerceIn(-0.15f, 0.15f)
            return points.map { pt ->
                pt.copy(
                    xNorm = (pt.xNorm + deltaX * 0.8f).coerceIn(0.02f, 0.98f),
                    yNorm = (pt.yNorm + baseShiftY * 0.7f).coerceIn(0.05f, 0.98f)
                )
            }
        }
    }
}
