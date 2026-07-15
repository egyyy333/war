package com.example.trenchwar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun MainMenuScreen(
    highestStage: Int,
    onStageSelected: (Int) -> Unit
) {
    var showTutorial by remember { mutableStateOf(false) }

    // Pulsing animation for the main button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B261D), // Dark Olive Army Green
                        Color(0xFF0F1510)  // Very Dark Charcoal
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative grid or soft visual noise can be added here
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700), // Pure Gold
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "صراع التكتيك والخنادق العسكرية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA0B0A0), // Light Muted Green
                    textAlign = TextAlign.Center
                )
            }

            // Central Area: Stage Selection or Main Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tutorial card (Left side)
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1F000000))
                        .border(1.dp, Color(0x3DFFD700), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tutorial_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tutorial_text),
                        fontSize = 11.sp,
                        color = Color(0xFFD0DDD0),
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Right
                    )
                }

                // Stage Selection (Right side)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(320.dp)
                ) {
                    Text(
                        text = "اختر المرحلة:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stage 1 Button
                        StageCard(
                            stageNum = 1,
                            unlocked = true,
                            onClick = { onStageSelected(1) },
                            title = "المرحلة الأولى",
                            subtitle = "ساحة التدريب سهلة"
                        )

                        // Stage 2 Button
                        val stage2Unlocked = highestStage >= 2
                        StageCard(
                            stageNum = 2,
                            unlocked = stage2Unlocked,
                            onClick = { if (stage2Unlocked) onStageSelected(2) },
                            title = "المرحلة الثانية",
                            subtitle = "جحيم الخندق متقدم"
                        )
                    }
                }
            }

            // Footer credits or exit
            Text(
                text = "إصدار ١.٠ - بدون إنترنت بالكامل",
                fontSize = 12.sp,
                color = Color(0x7FFFFFFF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun StageCard(
    stageNum: Int,
    unlocked: Boolean,
    onClick: () -> Unit,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .size(width = 140.dp, height = 150.dp)
            .testTag("stage_card_$stageNum")
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (unlocked) Color(0xFF4CAF50) else Color(0x3DFFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = unlocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Color(0x2A1B261D) else Color(0x1F000000)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$stageNum",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (unlocked) Color(0xFF4CAF50) else Color(0x4DFFFFFF)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color(0xFFA0B0A0),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
            }

            if (!unlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "مغلق",
                    tint = Color(0x4DFFFFFF),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "لعب",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
