package com.example.onboarding

import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * =========================================================================
 * ONBOARDING CONFIGURATION & CONTENT
 * =========================================================================
 * Todos los textos, colores, imágenes y opciones están centralizados aquí
 * para que puedas editar cualquier elemento fácilmente sin tocar la lógica.
 * =========================================================================
 */
object OnboardingConfig {

    // --- PALETA DE COLORES PERSONALIZABLE ---
    val BackgroundDark = Color(0xFF09041A)
    val BackgroundGradientEnd = Color(0xFF160B32)
    val CardBackground = Color(0xFF1B1038)
    val CardBackgroundSelected = Color(0xFF281654)
    val CardBorder = Color(0xFF2E1B56)
    
    // Color naranja (#eb5637) unificado para botones y todos los acentos (reemplaza al magenta)
    val OrangeAccent = Color(0xFFEB5637)
    val ButtonColor = OrangeAccent
    val Magenta = OrangeAccent
    val NeonPink = OrangeAccent            // Ahora naranja #eb5637 en todas las pantallas
    val CardBorderSelected = OrangeAccent
    val NeonPinkGlow = Color(0xFFFF7A59)   // Halo resplandeciente en gama naranja #eb5637
    
    val NeonCyan = Color(0xFF00E5FF)       // Azul cian tecnológico
    val TextWhite = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA8A0C2)  // Púrpura grisáceo para subtítulos
    val RadioUnselected = Color(0xFF4A3B6B)

    // --- PANTALLA 1: BIENVENIDA (1.jpg) ---
    const val Step1Badge = "LEVEL UP BASKETBALL"
    const val Step1TitlePart1 = "Conviértete en el\njugador que "
    const val Step1TitleHighlight = "no\npueden defender."
    const val Step1Subtitle = "Tu coach IA crea un plan de entrenamiento personalizado para tu juego. Gratis para empezar."
    const val Step1ButtonText = "EMPEZAR"
    const val Step1LoginPrompt = "¿Ya tienes cuenta? "
    const val Step1LoginLink = "Iniciar sesión"
    val Step1HeroImageRes: Int = R.drawable.img_onboarding_player

    // --- PANTALLA 2: COACH IA PERSONAL (3.jpg) ---
    val Step3CoachAvatarRes: Int = R.drawable.img_coach_avatar
    const val Step3CategoryBadge = "DISEÑADO PARA TU JUEGO"
    const val Step3TitlePrefix = "Tu "
    const val Step3TitleHighlight = "Coach IA personal"
    const val Step3Description = "No es una biblioteca de vídeos, es un entrenador real que estudia tu juego y crea tu plan."
    
    data class FeatureItem(
        val id: String,
        val emoji: String,
        val titlePrefix: String,
        val titleHighlight: String? = null,
        val description: String
    )

    val Step3Features = listOf(
        FeatureItem(
            id = "gemini",
            emoji = "🧠",
            titlePrefix = "Impulsado por ",
            titleHighlight = "Gemini 3.5",
            description = "El modelo de IA más capaz de Google: razona sobre tu juego"
        ),
        FeatureItem(
            id = "pose",
            emoji = "⛹️‍♂️",
            titlePrefix = "Detección de pose",
            description = "Analiza tu técnica de tiro, fotograma a fotograma"
        ),
        FeatureItem(
            id = "memory",
            emoji = "💾",
            titlePrefix = "Memoria completa",
            description = "Guarda cada sesión y todo tu progreso"
        ),
        FeatureItem(
            id = "skills",
            emoji = "🛠️",
            titlePrefix = "Herramientas y habilidades",
            description = "Crea planes, elige ejercicios, graba y programa tu semana"
        )
    )

    const val Step3ButtonText = "CONSTRUYAMOS MI PLAN →"

    // --- PANTALLA 4: ¿QUÉ QUIERES HACER HOY? (SELECCIÓN DE ENTRENAMIENTO) ---
    const val Step4CategoryBadge = "ELIGE TU MODALIDAD"
    const val Step4TitlePrefix = "¿Qué quieres "
    const val Step4TitleHighlight = "hacer hoy?"
    const val Step4Subtitle = "Selecciona cómo deseas entrenar para configurar la visión artificial en tiempo real."

    enum class TrainingModeChoice {
        KIDS_MINI_BASKET,  // Nuevo: Minibasket infantil en casa con calibración guiada
        DEFEND_ZONE,       // Nuevo: Esquiva a los Defensores Fantasma (Manos o Lásers, 3 vidas)
        DRIBBLE_CHALLENGE, // Analizar el bote de balón con combos LV3
        DRIBBLE_REACTION_POINTS, // Bote con Reaction Points
        SHOOTING_TUTORIAL, // Colocar móvil con tutorial para crear sesión de tiro
        UPLOAD_VIDEO       // Subir un vídeo para analizarlo
    }

    data class TrainingOptionItem(
        val mode: TrainingModeChoice,
        val title: String,
        val badge: String,
        val emoji: String,
        val description: String,
        val iconBg: Color
    )

    val Step4Options = listOf(
        TrainingOptionItem(
            mode = TrainingModeChoice.KIDS_MINI_BASKET,
            title = "Kids Mini Basket Arcade (Tiro Infantil)",
            badge = "🏀 ¡NUEVO! • CALIBRACIÓN FOTO Y PELOTA",
            emoji = "🎉",
            description = "Tiro infantil en casa con 3 pasos guiados: escaneo 360º de pelota en mano, apoyo de móvil fijo y ajuste de aro sobre foto.",
            iconBg = Color(0xFFE65100)
        ),
        TrainingOptionItem(
            mode = TrainingModeChoice.DEFEND_ZONE,
            title = "Defend the Zone (Defensores Fantasma)",
            badge = "👻 ¡JUEGO! • 3 VIDAS",
            emoji = "🛡️",
            description = "Esquiva manos virtuales o lásers que intentan robar tu bote desde los laterales. Entrena bote de protección, crossover y uso del cuerpo como escudo.",
            iconBg = Color(0xFF5E17EB)
        ),
        TrainingOptionItem(
            mode = TrainingModeChoice.DRIBBLE_CHALLENGE,
            title = "Dribble Combo (LV3 & Slick Moves)",
            badge = "🔥 COMBO • LV3",
            emoji = "⚡",
            description = "Medidor vertical LV3 dinámico, combos 'Slick Moves' y efecto de humo estilo dibujo animado en los pies al cambiar de mano.",
            iconBg = Color(0xFF00695C)
        ),
        TrainingOptionItem(
            mode = TrainingModeChoice.DRIBBLE_REACTION_POINTS,
            title = "Dribble Rush (Reaction Points)",
            badge = "AGILITY",
            emoji = "🎯",
            description = "Bota el balón de forma controlada y alcanza los points con la mano y el balón a izquierda y derecha.",
            iconBg = Color(0xFF4A1E5C)
        ),
        TrainingOptionItem(
            mode = TrainingModeChoice.SHOOTING_TUTORIAL,
            title = "Sesión de Tiro en Canasta",
            badge = "TUTORIAL + ARO",
            emoji = "🏀",
            description = "Coloca tu móvil con el tutorial guiado para calibrar el aro y medir tiros, aciertos y mecánica.",
            iconBg = Color(0xFF2B1D56)
        ),
        TrainingOptionItem(
            mode = TrainingModeChoice.UPLOAD_VIDEO,
            title = "Subir y Analizar Vídeo",
            badge = "VÍDEO / GALERÍA",
            emoji = "🎬",
            description = "Sube un vídeo grabado (.mp4) de tu galería para descomponer la técnica fotograma a fotograma.",
            iconBg = Color(0xFF1E1E28)
        )
    )

    const val Step4ButtonText = "COMENZAR ENTRENAMIENTO"
}
