package com.example.vision

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.example.stats.PlayerStatsManager
import com.example.supabase.SupabaseAuthManager
import java.util.Locale
import kotlin.random.Random

/**
 * Entrenador de Voz Inteligente con entonación deportiva, enérgica y expresiva.
 * 
 * - Modulación de tono (pitch) y velocidad (speechRate) en tiempo real para eliminar la voz plana.
 * - Selección automática de la mejor voz de alta calidad (Neural / HD) disponible en el dispositivo.
 * - El nombre del jugador se menciona de forma esporádica y natural (~15-20% de las veces) para no cansar.
 * - Interjecciones deportivas con fonética acentuada ("¡¡Toma!!", "¡¡Esoooo!!", "¡¡Vaaamos!!").
 * - 100% nativo, offline y a coste 0€.
 */
object VoiceCoachManager : TextToSpeech.OnInitListener {
    private const val TAG = "VoiceCoachManager"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Control de silencio / activación de la voz del entrenador
    var isVoiceEnabled: Boolean = true

    private var lastSpeechTime = 0L
    private var lastMissSpeechTime = 0L
    private var lastNameMentionTime = 0L

    fun init(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar TextToSpeech: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val spanish = Locale("es", "ES")
            val langResult = engine.setLanguage(spanish)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale("es"))
            }

            // Buscar la voz de mayor calidad/naturalidad disponible en el sistema
            try {
                val availableVoices = engine.voices
                val bestVoice = availableVoices?.filter { voice ->
                    val lang = voice.locale.language
                    lang.equals("es", ignoreCase = true) &&
                        !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }?.maxByOrNull { voice ->
                    var score = 0
                    val name = voice.name.lowercase()
                    if (name.contains("neural") || name.contains("network")) score += 15
                    if (name.contains("es-es")) score += 6
                    if (voice.quality >= Voice.QUALITY_VERY_HIGH) score += 8
                    else if (voice.quality >= Voice.QUALITY_HIGH) score += 4
                    if (voice.latency <= Voice.LATENCY_NORMAL) score += 2
                    score
                }

                if (bestVoice != null) {
                    engine.voice = bestVoice
                    Log.d(TAG, "Voz seleccionada: ${bestVoice.name} (calidad=${bestVoice.quality})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo seleccionar voz avanzada: ${e.message}")
            }

            // Configuración enérgica por defecto
            engine.setPitch(1.22f)
            engine.setSpeechRate(1.22f)
            isInitialized = true
            Log.d(TAG, "VoiceCoachManager inicializado con configuración deportiva expresiva")
        } else {
            Log.e(TAG, "Error inicializando TextToSpeech: status=$status")
        }
    }

    /**
     * Obtiene el nombre del usuario solo si es un nombre real y limpio.
     */
    fun getPlayerName(): String {
        val authName = SupabaseAuthManager.currentUser.value?.username?.trim().orEmpty()
        val statsName = PlayerStatsManager.stats.value.playerName.trim()
        return when {
            authName.isNotBlank() && !authName.startsWith("user_") -> authName
            statsName.isNotBlank() && !statsName.equals("Jugador", ignoreCase = true) -> statsName
            else -> ""
        }
    }

    /**
     * Decide si usar el nombre del jugador en esta frase (muy ocasional: ~15% y dejando espacio de tiempo).
     */
    private fun shouldUseName(): Boolean {
        val name = getPlayerName()
        if (name.isBlank()) return false
        val now = System.currentTimeMillis()
        if (now - lastNameMentionTime > 18000L && Random.nextFloat() < 0.22f) {
            lastNameMentionTime = now
            return true
        }
        return false
    }

    /**
     * Pronuncia una frase ajustando dinámicamente el tono (pitch) y la velocidad (rate)
     * para que suene emotiva, enérgica y con gancho.
     */
    fun speak(
        text: String,
        pitch: Float = 1.22f,
        rate: Float = 1.22f,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ) {
        if (!isVoiceEnabled || !isInitialized) return
        try {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
            tts?.speak(text, queueMode, null, "coach_${System.currentTimeMillis()}")
            lastSpeechTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error al reproducir voz: ${e.message}")
        }
    }

    /**
     * 1. CUENTA REGRESIVA CON ENERGÍA MÁXIMA (3, 2, 1, ¡¡Vaaamos!!)
     */
    fun speakCountdown(sec: Int) {
        if (!isVoiceEnabled) return
        when (sec) {
            3 -> speak("¡Tres!", pitch = 1.15f, rate = 1.12f)
            2 -> speak("¡Dos!", pitch = 1.22f, rate = 1.18f)
            1 -> speak("¡Uno!", pitch = 1.28f, rate = 1.24f)
            0 -> {
                val name = getPlayerName()
                val useName = name.isNotBlank() && Random.nextBoolean()
                val startPhrases = if (useName) {
                    listOf(
                        "¡¡Vaaamos $name, a por todas!!",
                        "¡¡Dale con todo, $name!!",
                        "¡¡A volar, $name!!"
                    )
                } else {
                    listOf(
                        "¡¡VAAAAMOS, a por todas!!",
                        "¡¡A jugar!!",
                        "¡¡Dale con todo!!",
                        "¡¡A volar!!"
                    )
                }
                speak(startPhrases.random(), pitch = 1.26f, rate = 1.26f)
            }
        }
    }

    /**
     * 2. ACIERTO DE PUNTOS / ANIMACIÓN EXPRESIVA
     */
    fun onReactionHit(score: Int) {
        if (!isVoiceEnabled) return
        val now = System.currentTimeMillis()
        val name = getPlayerName()

        // HITO DE RACHA O MULTIPLICADOR (Cada 5 puntos: 5, 10, 15, 20, 25...) -> Máxima euforia
        if (score > 0 && score % 5 == 0) {
            val withName = shouldUseName()
            val streakPhrases = if (withName) {
                listOf(
                    "¡¡$name, estás que te sales!!",
                    "¡¡Vaya reflejos tienes, $name!!",
                    "¡¡$score puntos, tremendo $name!!",
                    "¡¡Qué nivel tienes hoy, $name!!"
                )
            } else {
                listOf(
                    "¡¡Estás que te sales!!",
                    "¡¡En llamas, modo bestia activado!!",
                    "¡¡Qué velocidad, imparable!!",
                    "¡¡$score puntos ya, qué locura!!",
                    "¡¡Booooom!! ¡¡Brutal!!"
                )
            }
            speak(streakPhrases.random(), pitch = 1.28f, rate = 1.25f)
            return
        }

        // ACIERTOS REGULARES (Intervalo natural de ~3.5 segundos para no saturar)
        if (now - lastSpeechTime > 3400L) {
            val withName = shouldUseName()
            val quickHype = if (withName) {
                listOf(
                    "¡¡Buena, $name!!",
                    "¡¡Esa es, $name!!",
                    "¡¡Grande, $name!!"
                )
            } else {
                listOf(
                    "¡¡Toma!!",
                    "¡¡Esoooo!!",
                    "¡¡Qué rápida!!",
                    "¡¡Esa es!!",
                    "¡¡Punto!!",
                    "¡¡Bien visto!!",
                    "¡¡Sigue así!!",
                    "¡¡Pum!!",
                    "¡¡Vuela!!"
                )
            }
            speak(quickHype.random(), pitch = 1.22f, rate = 1.24f)
        }
    }

    /**
     * 3. CUANDO EL JUGADOR HA FALLADO / DIANA EXPIRADA
     */
    fun onReactionMiss() {
        if (!isVoiceEnabled) return
        val now = System.currentTimeMillis()
        // Control estricto para no hablar si se fallan varias dianas seguidas
        if (now - lastMissSpeechTime > 5000L && now - lastSpeechTime > 2800L) {
            lastMissSpeechTime = now
            val withName = shouldUseName()
            val name = getPlayerName()
            val missPhrases = if (withName) {
                listOf(
                    "¡¡No pasa nada $name, a la siguiente!!",
                    "¡¡Concéntrate $name, tú puedes!!",
                    "¡¡Rápido $name, busca la diana!!"
                )
            } else {
                listOf(
                    "¡¡Ufff, casi!! ¡¡A por la siguiente!!",
                    "¡¡Rápido, busca la nueva!!",
                    "¡¡No pares, sigue el bote!!",
                    "¡¡Ojo a la pantalla, tú puedes!!",
                    "¡¡Arriba esa cabeza, vamos!!"
                )
            }
            speak(missPhrases.random(), pitch = 1.16f, rate = 1.22f)
        }
    }

    /**
     * 4. AVISO DE TIEMPO RESTANTE (Urgencia de partido)
     */
    fun onTimeRemaining(seconds: Int) {
        if (!isVoiceEnabled) return
        when (seconds) {
            20 -> speak("¡¡Últimos veinte segundos, aprieta!!", pitch = 1.24f, rate = 1.25f)
            10 -> speak("¡¡Diez segundos, dale con todo!!", pitch = 1.28f, rate = 1.28f)
            5 -> speak("¡¡Cinco segundos, los últimos!!", pitch = 1.30f, rate = 1.30f)
        }
    }

    /**
     * CANASTA EN MODO KIDS MINI BASKET (Infantil, alegre y enérgica)
     */
    fun onKidsBasketMade(score: Int, streak: Int) {
        if (!isVoiceEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastSpeechTime < 1800L) return

        if (streak >= 3) {
            val streakPhrases = listOf(
                "¡¡Racha de tres, estás en llamas!!",
                "¡¡Increíble, otra canasta seguida!!",
                "¡¡Vaya triplazo, qué puntería!!"
            )
            speak(streakPhrases.random(), pitch = 1.28f, rate = 1.25f)
            return
        }

        val makePhrases = listOf(
            "¡¡Canastón!!",
            "¡¡Chof, limpia!!",
            "¡¡Qué canasta tan buena!!",
            "¡¡Dos puntos a la saca!!",
            "¡¡Directa a la red!!",
            "¡¡Brutal, esa entró perfecta!!"
        )
        speak(makePhrases.random(), pitch = 1.24f, rate = 1.22f)
    }

    fun onKidsSessionFinished(finalScore: Int, makes: Int) {
        if (!isVoiceEnabled) return
        val name = getPlayerName()
        val greeting = if (name.isNotBlank()) " $name" else ""
        val finishPhrases = listOf(
            "¡¡Bocina final!! ¡¡Gran partido$greeting!! Has metido $makes canastas y $finalScore puntos.",
            "¡¡Tiempo completado$greeting!! ¡¡Vaya puntería con $finalScore puntos conseguidos!!",
            "¡¡Se acabó el tiempo!! ¡¡Tremendo partido$greeting, $makes canastas dentro!!"
        )
        speak(finishPhrases.random(), pitch = 1.20f, rate = 1.15f)
    }

    /**
     * 5. FINAL DE SESIÓN TRAS AGOTARSE LOS 60 SEGUNDOS
     */
    fun onSessionFinished(finalScore: Int) {
        if (!isVoiceEnabled) return
        val name = getPlayerName()
        val greeting = if (name.isNotBlank()) " $name" else ""
        val finishPhrases = listOf(
            "¡¡Bocina final!! ¡¡Sesión completada$greeting, has sumado $finalScore puntos!!",
            "¡¡Tiempo!! ¡¡Gran entrenamiento$greeting!! $finalScore puntos conseguidos.",
            "¡¡Se acabó el tiempo!! ¡¡Muy bien jugado$greeting, $finalScore puntos a la saca!!"
        )
        speak(finishPhrases.random(), pitch = 1.18f, rate = 1.15f)
    }

    /**
     * Detiene la reproducción en curso de forma inmediata.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener TTS: ${e.message}")
        }
    }

    /**
     * Libera recursos al cerrar la app.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar TTS: ${e.message}")
        }
    }
}

