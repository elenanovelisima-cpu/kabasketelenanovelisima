package com.example.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla 4: Selección de Modalidad de Entrenamiento
 * "¿Qué quieres hacer hoy?"
 * Ofrece 3 opciones:
 * 1. Sesión de tiro con tutorial guiado para colocar el móvil y calibrar aro
 * 2. Subir y analizar un vídeo de la galería
 * 3. Analizar el bote de balón del jugador (sin canasta) con efecto +5 puntos por cambio de mano
 */
@Composable
fun Step4TrainingModeSelectorView(
    onSelectMode: (OnboardingConfig.TrainingModeChoice) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember {
        mutableStateOf(OnboardingConfig.TrainingModeChoice.DEFEND_ZONE)
    }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        OnboardingConfig.BackgroundDark,
                        OnboardingConfig.BackgroundGradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Botón volver
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x33FFFFFF), CircleShape)
                    .testTag("step4_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Badge superior
            Text(
                text = OnboardingConfig.Step4CategoryBadge,
                color = OnboardingConfig.OrangeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Título: ¿Qué quieres hacer hoy?
            val titleString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = OnboardingConfig.TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                ) {
                    append(OnboardingConfig.Step4TitlePrefix)
                }
                withStyle(
                    style = SpanStyle(
                        color = OnboardingConfig.OrangeAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                ) {
                    append(OnboardingConfig.Step4TitleHighlight)
                }
            }

            Text(
                text = titleString,
                lineHeight = 36.sp,
                modifier = Modifier.testTag("step4_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = OnboardingConfig.Step4Subtitle,
                color = OnboardingConfig.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de 3 modalidades
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OnboardingConfig.Step4Options.forEach { option ->
                    val isSelected = selectedMode == option.mode
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) OnboardingConfig.CardBorderSelected else OnboardingConfig.CardBorder,
                        animationSpec = tween(durationMillis = 200),
                        label = "modeBorderColor"
                    )
                    val cardBgColor by animateColorAsState(
                        targetValue = if (isSelected) OnboardingConfig.CardBackgroundSelected else OnboardingConfig.CardBackground,
                        animationSpec = tween(durationMillis = 200),
                        label = "modeCardBgColor"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBgColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.2.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedMode = option.mode
                            }
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                            .testTag("training_option_${option.mode.name.lowercase()}"),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Icono representativo
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(option.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.emoji,
                                fontSize = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Textos y badge
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Badge pequeño
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) OnboardingConfig.OrangeAccent.copy(alpha = 0.25f)
                                            else Color(0x33FFFFFF)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = option.badge,
                                        color = if (isSelected) OnboardingConfig.OrangeAccent else OnboardingConfig.TextSecondary,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = option.title,
                                color = OnboardingConfig.TextWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = option.description,
                                color = OnboardingConfig.TextSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Radio selector circular
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 2.dp,
                                    color = if (isSelected) OnboardingConfig.OrangeAccent else OnboardingConfig.RadioUnselected,
                                    shape = CircleShape
                                )
                                .background(if (isSelected) OnboardingConfig.OrangeAccent.copy(alpha = 0.2f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(OnboardingConfig.OrangeAccent)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botón inferior de confirmación
            Button(
                onClick = {
                    onSelectMode(selectedMode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("step4_start_training_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardingConfig.ButtonColor
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = OnboardingConfig.Step4ButtonText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
