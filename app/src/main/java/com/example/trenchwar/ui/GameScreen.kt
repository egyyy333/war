package com.example.trenchwar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.trenchwar.game.*
import com.example.trenchwar.game.GameViewModel.GameState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    val context = LocalContext.current

    val currentStage by viewModel.currentStage.collectAsState()
    val alliesHQHealth by viewModel.alliesHQHealth.collectAsState()
    val axisHQHealth by viewModel.axisHQHealth.collectAsState()

    val alliesTrenchGroups by viewModel.alliesTrenchGroups.collectAsState()
    val activeUnits by viewModel.activeUnits.collectAsState()
    val particles by viewModel.particles.collectAsState()
    val bullets by viewModel.bullets.collectAsState()

    val gameState by viewModel.gameState.collectAsState()
    val deployedCount by viewModel.deployedUnitsCount.collectAsState()
    val survivedCount by viewModel.survivedUnitsCount.collectAsState()

    val infantryCooldown by viewModel.infantryCooldown.collectAsState()
    val riflemanCooldown by viewModel.riflemanCooldown.collectAsState()
    val armoredCooldown by viewModel.armoredCooldown.collectAsState()

    val floatingTexts by viewModel.floatingTexts.collectAsState()
    val autoDeploy by viewModel.autoDeploy.collectAsState()
    val screenShakeTime by viewModel.screenShakeTime.collectAsState()

    // Screen shaking factor if either base is under severe attack or triggered via game events
    val isAlliesBaseDamaged = alliesHQHealth < 300f
    val isAxisBaseDamaged = axisHQHealth < 300f

    val shakeX = if (screenShakeTime > 0f) (sin(System.currentTimeMillis() * 0.15) * 6f).dp else 0.dp
    val shakeY = if (screenShakeTime > 0f) (cos(System.currentTimeMillis() * 0.18) * 6f).dp else 0.dp

    // Load background image safely
    val bgImage = remember {
        try {
            ImageBitmap.imageResource(context.resources, R.drawable.trench_battlefield_bg_1784154185945)
        } catch (e: Exception) {
            null
        }
    }

    // Sky clouds moving offsets
    var skyTime by remember { mutableStateOf(0f) }
    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            while (true) {
                skyTime += 0.05f
                delay(16)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1510))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP HEADER (Stats & Level info)
            GameTopHeader(
                currentStage = currentStage,
                alliesHealth = alliesHQHealth,
                alliesMax = viewModel.alliesHQMaxHealth,
                axisHealth = axisHQHealth,
                axisMax = viewModel.axisHQMaxHealth,
                onPause = { viewModel.pauseGame() },
                onBack = onBackToMenu
            )

            var showBattleGuide by remember { mutableStateOf(true) }
            if (showBattleGuide) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x3B1B261D)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "طريقة اللعب البسيطة والشيقة:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "١. اضغط على البطاقات بالأسفل لتوظيف فرقة انتظار بالخندق.\n٢. اضغط على الفرقة الجاهزة بالخندق (الزر الأخضر) لتنطلق للهجوم الكاسح!",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = { showBattleGuide = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("✕", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. THE MAIN BATTLEFIELD CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(shakeX, shakeY)
                    .background(Color(0xFF2A2218))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("battlefield_canvas")
                ) {
                    val scaleX = size.width / viewModel.virtualWidth
                    val scaleY = size.height / viewModel.virtualHeight

                    // Draw Background
                    if (bgImage != null) {
                        drawImage(
                            image = bgImage,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.Low
                        )
                    } else {
                        // Fallback gorgeous dirt gradient representing the mud ground
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                            )
                        )
                    }

                    // Slowly moving clouds sky overlay
                    drawSkyClouds(skyTime, size.width, size.height * 0.35f)

                    // Draw 3D battlefield ground features (craters, lines, dirt tracks)
                    drawBattlefieldGroundFeatures(
                        virtualWidth = viewModel.virtualWidth,
                        virtualHeight = viewModel.virtualHeight,
                        screenWidth = size.width,
                        screenHeight = size.height
                    )

                    // Draw 3D Trench Background (trench cavity & floor wooden planks)
                    drawTrenchBackground3D(
                        trenchVX = viewModel.alliesTrenchX,
                        virtualWidth = viewModel.virtualWidth,
                        virtualHeight = viewModel.virtualHeight,
                        screenWidth = size.width,
                        screenHeight = size.height
                    )
                    drawTrenchBackground3D(
                        trenchVX = viewModel.axisTrenchX,
                        virtualWidth = viewModel.virtualWidth,
                        virtualHeight = viewModel.virtualHeight,
                        screenWidth = size.width,
                        screenHeight = size.height
                    )

                    // Draw 3D Headquarters / Bunkers in structural perspective
                    drawMilitaryBases(
                        virtualWidth = viewModel.virtualWidth,
                        virtualHeight = viewModel.virtualHeight,
                        screenWidth = size.width,
                        screenHeight = size.height,
                        alliesHealthRatio = alliesHQHealth / viewModel.alliesHQMaxHealth,
                        axisHealthRatio = axisHQHealth / viewModel.axisHQMaxHealth
                    )

                    // Draw Active 3D Soldiers
                    for (u in activeUnits) {
                        drawSoldier(
                            u = u,
                            virtualWidth = viewModel.virtualWidth,
                            virtualHeight = viewModel.virtualHeight,
                            screenWidth = size.width,
                            screenHeight = size.height
                        )
                    }

                    // Draw 3D Trench Foreground (wood poles, steps, protective sandbags on lips) over soldiers
                    drawTrenchForeground3D(
                        trenchVX = viewModel.alliesTrenchX,
                        virtualWidth = viewModel.virtualWidth,
                        virtualHeight = viewModel.virtualHeight,
                        screenWidth = size.width,
                        screenHeight = size.height
                    )
                    drawTrenchForeground3D(
                        trenchVX = viewModel.axisTrenchX,
                        virtualWidth = viewModel.virtualWidth,
                        virtualHeight = viewModel.virtualHeight,
                        screenWidth = size.width,
                        screenHeight = size.height
                    )

                    // Draw 3D Bullets with slanted trajectories in perspective
                    for (b in bullets) {
                        val tailX = if (b.team == Team.ALLIES) b.x - 15f else b.x + 15f
                        val pStart = project3D(b.x, b.y, viewModel.virtualWidth, viewModel.virtualHeight, size.width, size.height)
                        val pEnd = project3D(tailX, b.y, viewModel.virtualWidth, viewModel.virtualHeight, size.width, size.height)
                        drawLine(
                            color = Color(0xFFFFEB3B),
                            start = pStart.offset,
                            end = pEnd.offset,
                            strokeWidth = 2.8f * pStart.scale * scaleX,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw 3D Particles (Sparks & blood splatters)
                    for (p in particles) {
                        val proj = project3D(p.x, p.y, viewModel.virtualWidth, viewModel.virtualHeight, size.width, size.height)
                        drawCircle(
                            color = p.color.copy(alpha = p.life / p.maxLife),
                            center = proj.offset,
                            radius = p.size * proj.scale * scaleX
                        )
                    }
                }

                // 3D FLOATING DAMAGE TEXT OVERLAY (REFINED FOR TRUE PERSPECTIVE)
                val density = LocalDensity.current.density
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight
                    val screenWidthPx = w.value * density
                    val screenHeightPx = h.value * density

                    for (t in floatingTexts) {
                        val proj = project3D(t.x, t.y, viewModel.virtualWidth, viewModel.virtualHeight, screenWidthPx, screenHeightPx)
                        val posX = proj.offset.x / density
                        val posY = proj.offset.y / density
                        val textScale = proj.scale * (if (t.isCritical) 1.25f else 1.0f)

                        Text(
                            text = t.text,
                            color = t.color,
                            fontSize = if (t.isCritical) 14.sp else 11.sp,
                            fontWeight = if (t.isCritical) FontWeight.ExtraBold else FontWeight.Bold,
                            modifier = Modifier
                                .offset(posX.dp, posY.dp)
                                .graphicsLayer {
                                    alpha = t.life.coerceIn(0f, 1f)
                                    scaleX = textScale
                                    scaleY = textScale
                                },
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }

                // Shaking or red border effect if bases are endangered
                if (isAlliesBaseDamaged) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, Color(0x66D32F2F))
                    )
                }
                if (isAxisBaseDamaged) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, Color(0x66FFD700))
                    )
                }
            }

            // 3. TRENCH STATUS & RECRUITMENT BAR
            GameControlPanel(
                alliesTrenchGroups = alliesTrenchGroups,
                infantryCD = infantryCooldown,
                riflemanCD = riflemanCooldown,
                armoredCD = armoredCooldown,
                autoDeploy = autoDeploy,
                onToggleAutoDeploy = { viewModel.toggleAutoDeploy() },
                onMerge = { viewModel.mergeTrenchGroups() },
                onRecruit = { type -> viewModel.recruitUnit(type) },
                onDeployGroup = { id -> viewModel.deployAlliesGroup(id) }
            )
        }

        // --- OVERLAYS: PAUSED, VICTORY, DEFEAT ---
        if (gameState == GameState.PAUSED) {
            OverlayContainer {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xED1B261D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(340.dp)
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لعبة موقوفة مؤقتاً",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resumeGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استئناف القتال")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBackToMenu,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("العودة للقائمة الرئيسية")
                        }
                    }
                }
            }
        }

        if (gameState == GameState.VICTORY) {
            OverlayContainer {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xF21B261D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(380.dp)
                        .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.victory),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700), // Gold
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "لقد تم تطهير خنادق العدو والاستيلاء على المقر!",
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        HorizontalDivider(color = Color(0x3DFFFFFF), modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الوحدات المستدعاة:", color = Color(0xFFA0B0A0))
                            Text("$deployedCount", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الوحدات التي نجت:", color = Color(0xFFA0B0A0))
                            Text("$survivedCount", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackToMenu,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("القائمة")
                            }
                            Button(
                                onClick = { viewModel.nextStage() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("next_stage_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text(stringResource(R.string.next_stage))
                            }
                        }
                    }
                }
            }
        }

        if (gameState == GameState.DEFEAT) {
            OverlayContainer {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xF22B1818)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(360.dp)
                        .border(2.dp, Color(0xFFF44336), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.defeat),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF44336),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "سقط مقرّك تحت غزو العدو الغاشم. حاول وضع تكتيك أفضل!",
                            fontSize = 13.sp,
                            color = Color(0xFFE0C0C0),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackToMenu,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("القائمة")
                            }
                            Button(
                                onClick = { viewModel.retryStage() },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("retry_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun GameTopHeader(
    currentStage: Int,
    alliesHealth: Float,
    alliesMax: Float,
    axisHealth: Float,
    axisMax: Float,
    onPause: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131A14))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Allies HQ Health (Left)
        Column(modifier = Modifier.width(180.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المقر (الحلفاء)", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${alliesHealth.toInt()}/${alliesMax.toInt()}", color = Color.White, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { alliesHealth / alliesMax },
                color = Color(0xFF4CAF50),
                trackColor = Color(0x334CAF50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
        }

        // Stage Title (Middle)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${stringResource(R.string.stage)} $currentStage",
                color = Color(0xFFFFD700),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (currentStage == 1) "تدريب أساسي" else "خندق متقدم",
                color = Color.LightGray,
                fontSize = 9.sp
            )
        }

        // Axis HQ Health (Right)
        Column(modifier = Modifier.width(180.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${axisHealth.toInt()}/${axisMax.toInt()}", color = Color.White, fontSize = 11.sp)
                Text("المقر (المحور)", color = Color(0xFFE91E63), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { axisHealth / axisMax },
                color = Color(0xFFE91E63),
                trackColor = Color(0x33E91E63),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPause,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("إيقاف مؤقت", fontSize = 10.sp)
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

@Composable
fun GameControlPanel(
    alliesTrenchGroups: List<SoldierGroup>,
    infantryCD: Float,
    riflemanCD: Float,
    armoredCD: Float,
    autoDeploy: Boolean,
    onToggleAutoDeploy: () -> Unit,
    onMerge: () -> Unit,
    onRecruit: (UnitType) -> Unit,
    onDeployGroup: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1510))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // UNIT CARDS BAR (Left side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            UnitRecruitmentCard(
                type = UnitType.INFANTRY,
                cdProgress = infantryCD,
                onClick = { onRecruit(UnitType.INFANTRY) }
            )
            UnitRecruitmentCard(
                type = UnitType.RIFLEMAN,
                cdProgress = riflemanCD,
                onClick = { onRecruit(UnitType.RIFLEMAN) }
            )
            UnitRecruitmentCard(
                type = UnitType.ARMORED,
                cdProgress = armoredCD,
                onClick = { onRecruit(UnitType.ARMORED) }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // TRENCH STATUS/DEPLOY SLOTS & MERGE/AUTO CONTROLS (Right side)
        Column(
            modifier = Modifier
                .width(310.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x24FFFFFF))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قوات الخندق (أقصى ٣):",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // ⚡ MERGE BUTTON (Golden gradient compact button)
                Button(
                    onClick = onMerge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(25.dp)
                ) {
                    Text("دمج ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (slotIdx in 0..2) {
                    val group = alliesTrenchGroups.getOrNull(slotIdx)
                    val levelLabel = if (group != null && group.level > 1) "مستوى ${group.level}" else ""
                    val slotBgColor = if (group != null) {
                        when (group.level) {
                            2 -> Color(0xFF2E3B1E) // Olive green
                            3 -> Color(0xFF3E2C10) // Bronze/gold hue
                            else -> Color(0xFF1B261D)
                        }
                    } else Color(0x1A000000)

                    val slotBorderColor = if (group != null) {
                        when (group.level) {
                            2 -> Color(0xFF81C784)
                            3 -> Color(0xFFFFB74D)
                            else -> Color(0xFF4CAF50)
                        }
                    } else Color(0x3DFFFFFF)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(slotBgColor)
                            .border(1.dp, slotBorderColor, RoundedCornerShape(6.dp))
                            .clickable(enabled = group != null) {
                                if (group != null) {
                                    onDeployGroup(group.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (group != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = group.type.titleAr,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (levelLabel.isNotEmpty()) {
                                        Text(
                                            text = "⭐",
                                            color = Color(0xFFFFD700),
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (group.level > 1) "انطلاق! (مستوى ${group.level})" else "انطلاق! (x${group.soldierIds.size})",
                                    color = if (group.level > 1) Color(0xFFFFD700) else Color(0xFF4CAF50),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "فارغ",
                                color = Color(0x4DFFFFFF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 🤖 AUTO-DEPLOY SWITCH ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x15000000))
                    .clickable { onToggleAutoDeploy() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (autoDeploy) "🤖 هجوم تلقائي: مفعّل" else "🤖 هجوم تلقائي: معطّل",
                        color = if (autoDeploy) Color(0xFF81C784) else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(إرسال تلقائي للخنادق)",
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                }

                // Custom toggle handle
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 15.dp)
                        .clip(CircleShape)
                        .background(if (autoDeploy) Color(0xFF4CAF50) else Color.DarkGray)
                        .padding(2.dp),
                    contentAlignment = if (autoDeploy) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun UnitRecruitmentCard(
    type: UnitType,
    cdProgress: Float,
    onClick: () -> Unit
) {
    val ready = cdProgress >= 1f

    Card(
        modifier = Modifier
            .size(width = 90.dp, height = 62.dp)
            .testTag("recruit_card_${type.name}")
            .clickable(enabled = ready, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) Color(0xFF131A14) else Color(0x14000000)
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (ready) Color(0xFFFFD700) else Color(0x1F000000))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = type.titleAr,
                    color = if (ready) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = "العدد: x${type.groupSize}",
                    color = if (ready) Color(0xFFA0B0A0) else Color.DarkGray,
                    fontSize = 9.sp
                )

                if (ready) {
                    Text(
                        text = "جاهز",
                        color = Color(0xFF4CAF50),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val waitSec = ((1f - cdProgress) * type.cooldownMs / 1000f)
                    Text(
                        text = String.format("%.1fs", waitSec),
                        color = Color.Red,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cooldown dark overlay sweeping down
            if (!ready) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(1f - cdProgress)
                        .background(Color(0x7F000000))
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun OverlayContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// --- CANVAS PROCEDURAL DRAWING ASSISTANTS ---

data class ProjectResult(val offset: Offset, val scale: Float)

fun project3D(
    vx: Float,
    vy: Float,
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float
): ProjectResult {
    // A slightly higher horizon (110f) gives a deep, sweeping panoramic perspective
    val horizonY = 110f
    val t = ((vy - horizonY) / (virtualHeight - horizonY)).coerceIn(0f, 1f)
    
    // Near elements are full scale (1.1x), far elements recede gracefully (0.62x)
    val scale = 0.62f + t * 0.48f
    
    // Horizontal convergence towards the center of the viewport (500f virtual)
    val centerX = 500f
    val projX = centerX + (vx - centerX) * scale
    
    // Non-linear projection squashes distance and expands foreground for immersive depth
    val projY = horizonY + (vy - horizonY) * (0.32f * t + 0.68f * t * t)
    
    val scaleX = screenWidth / virtualWidth
    val scaleY = screenHeight / virtualHeight
    
    return ProjectResult(
        offset = Offset(projX * scaleX, projY * scaleY),
        scale = scale
    )
}

fun DrawScope.drawSkyClouds(time: Float, width: Float, height: Float) {
    // Draw slowly drifting clouds
    val count = 3
    for (i in 0 until count) {
        val speed = 15f + i * 10f
        var cx = (time * speed + i * 350f) % (width + 200f) - 100f
        val cy = 15f + i * 18f
        val rx = 60f + i * 15f
        val ry = 18f + i * 4f

        drawOval(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2, ry * 2)
        )
    }
}

fun DrawScope.drawBattlefieldGroundFeatures(
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float
) {
    val windSway = sin(System.currentTimeMillis() / 600.0).toFloat() * 4f
    val scaleX = screenWidth / virtualWidth
    val scaleY = screenHeight / virtualHeight

    // 1. Muddy Track Ruts (horizontal vehicle tread lines running down the road in 3D perspective)
    val rutsColor = Color(0xFF332211).copy(alpha = 0.28f)
    for (offsetY in listOf(170f, 190f, 250f, 270f)) {
        val steps = 10
        val dx = virtualWidth / steps
        for (i in 0 until steps) {
            val vx1 = i * dx
            val vx2 = (i + 1) * dx
            val p1 = project3D(vx1, offsetY, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
            val p2 = project3D(vx2, offsetY, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
            drawLine(
                color = rutsColor,
                start = p1,
                end = p2,
                strokeWidth = 5f * project3D(vx1, offsetY, virtualWidth, virtualHeight, screenWidth, screenHeight).scale * scaleY,
                cap = StrokeCap.Round
            )
        }
    }

    // 2. High-Quality Tufts of Grass
    // Top Grass Line
    val grassPositions = listOf(150f, 240f, 380f, 480f, 620f, 750f, 850f)
    for (gx in grassPositions) {
        val proj = project3D(gx, 155f, virtualWidth, virtualHeight, screenWidth, screenHeight)
        drawGrassTuft(proj.offset.x, proj.offset.y, windSway, proj.scale * scaleX, proj.scale * scaleY)
    }
    // Bottom Grass Line
    val bottomGrassPositions = listOf(110f, 200f, 310f, 450f, 580f, 710f, 830f, 920f)
    for (gx in bottomGrassPositions) {
        val proj = project3D(gx, 290f, virtualWidth, virtualHeight, screenWidth, screenHeight)
        drawGrassTuft(proj.offset.x, proj.offset.y, windSway, proj.scale * scaleX, proj.scale * scaleY)
    }

    // 3. Scattered Pebbles and Small Debris (perfectly flat in perspective)
    val pebbles = listOf(
        Pair(220f, 180f), Pair(380f, 280f), Pair(520f, 145f),
        Pair(610f, 290f), Pair(780f, 190f), Pair(830f, 270f)
    )
    for (pebble in pebbles) {
        val proj = project3D(pebble.first, pebble.second, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val px = proj.offset.x
        val py = proj.offset.y
        val scale = proj.scale
        val r = (3.5f + (pebble.first % 4)) * scale * scaleX

        // Pebble Shadow (lying flat on ground)
        drawOval(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(px - r, py - r * 0.4f + 2f * scale),
            size = Size(r * 2f, r * 0.8f)
        )
        // Pebble Body
        drawOval(
            color = Color(0xFF8A7F73),
            topLeft = Offset(px - r, py - r * 0.4f),
            size = Size(r * 2f, r * 0.8f)
        )
        // Highlight
        drawOval(
            color = Color(0xFFD4CBBF),
            topLeft = Offset(px - r * 0.6f, py - r * 0.3f),
            size = Size(r * 1f, r * 0.4f)
        )
    }

    // 4. Stylized Czech Hedgehog Tank Obstacles
    val obstaclePositions = listOf(290f, 720f)
    for (ox in obstaclePositions) {
        val proj = project3D(ox, 220f, virtualWidth, virtualHeight, screenWidth, screenHeight)
        drawCzechHedgehog(proj.offset.x, proj.offset.y, proj.scale * scaleX, proj.scale * scaleY)
    }

    // 5. Epic Artillery Craters with glowing hot inner embers
    val craters = listOf(
        Pair(340f, 180f),
        Pair(500f, 260f),
        Pair(460f, 140f),
        Pair(680f, 220f)
    )
    for (crater in craters) {
        val proj = project3D(crater.first, crater.second, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val cx = proj.offset.x
        val cy = proj.offset.y
        val scale = proj.scale
        val cr = 32f * scale * scaleX
        val crH = cr * 0.5f // Vertically squashed to lay flat in 3D perspective!

        // Crater Outer Lip (raised charred soil with dynamic gradient)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8D6E63).copy(alpha = 0.5f), Color.Transparent),
                center = Offset(cx, cy),
                radius = cr * 1.5f
            ),
            topLeft = Offset(cx - cr * 1.5f, cy - crH * 1.5f),
            size = Size(cr * 3f, crH * 3f)
        )

        // Crater Cavity Shadow
        drawOval(
            color = Color(0xFF1E1510),
            topLeft = Offset(cx - cr, cy - crH),
            size = Size(cr * 2f, crH * 2f)
        )

        // Crater Inner Glowing Hot Hole
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF000000), Color(0xFF3E2723), Color(0xFFC62828).copy(alpha = 0.5f)),
                center = Offset(cx, cy),
                radius = cr * 0.7f
            ),
            topLeft = Offset(cx - cr * 0.7f, cy - crH * 0.7f),
            size = Size(cr * 1.4f, crH * 1.4f)
        )

        // Deep black core
        drawOval(
            color = Color(0xFF0C0705),
            topLeft = Offset(cx - cr * 0.35f, cy - crH * 0.35f),
            size = Size(cr * 0.7f, crH * 0.7f)
        )

        // Charred Burn Ring
        drawOval(
            color = Color(0xFF212121).copy(alpha = 0.8f),
            topLeft = Offset(cx - cr * 0.8f, cy - crH * 0.8f),
            size = Size(cr * 1.6f, crH * 1.6f),
            style = Stroke(width = 2.5f * scaleX * scale)
        )
    }

    // 6. Barbed Wire Obstacles (running vertically, slanting in 3D!)
    val wireBarriers = listOf(360f, 640f)
    for (wx in wireBarriers) {
        val startY = 140f
        val endY = 290f
        val spacing = 38f

        var currentY = startY
        while (currentY < endY) {
            val proj = project3D(wx, currentY, virtualWidth, virtualHeight, screenWidth, screenHeight)
            val rx = proj.offset.x
            val ry = proj.offset.y
            val scale = proj.scale

            // Stake shadow (lying flat on ground)
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(rx, ry),
                end = Offset(rx + 15f * scale * scaleX, ry + 8f * scale * scaleY),
                strokeWidth = 2.5f * scale * scaleX
            )

            // Wooden stake (stands vertically, i.e., straight UP on screen, not projected!)
            val stakeH = 24f * scale * scaleY
            drawRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(rx - 3f * scale * scaleX, ry - stakeH),
                size = Size(6f * scale * scaleX, stakeH)
            )

            // Barbed wire loops (oval drawn vertically standing/leaning)
            val loopW = 22f * scale * scaleX
            val loopH = 14f * scale * scaleY
            drawOval(
                color = Color(0xFFB0BEC5),
                topLeft = Offset(rx - loopW / 2, ry - stakeH + 4f * scale * scaleY),
                size = Size(loopW, loopH),
                style = Stroke(width = 1.8f * scale * scaleX)
            )

            // Tiny barbed spikes
            for (angle in listOf(45f, 135f, 225f, 315f)) {
                val rad = Math.toRadians(angle.toDouble())
                val sx = rx + (cos(rad) * (loopW / 2)).toFloat()
                val sy = (ry - stakeH + 4f * scale * scaleY + loopH / 2) + (sin(rad) * (loopH / 2)).toFloat()
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(sx - 3f * scale * scaleX, sy - 3f * scale * scaleY),
                    end = Offset(sx + 3f * scale * scaleX, sy + 3f * scale * scaleY),
                    strokeWidth = 2.5f * scale * scaleX
                )
            }
            currentY += spacing
        }
    }
}

fun DrawScope.drawGrassTuft(x: Float, y: Float, windOffset: Float, scaleX: Float, scaleY: Float) {
    val blades = 3
    val grassColor = Color(0xFF5D7A2B) // Muddy military green
    for (b in 0 until blades) {
        val height = (11f + b * 5f) * scaleY
        val startX = x + (b - 1) * 5f * scaleX
        val endX = startX + windOffset * (0.8f + b * 0.3f)
        drawLine(
            color = grassColor,
            start = Offset(startX, y),
            end = Offset(endX, y - height),
            strokeWidth = 3f * scaleX,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawCzechHedgehog(x: Float, y: Float, scaleX: Float, scaleY: Float) {
    val steelColor = Color(0xFF263238)
    val lightSteel = Color(0xFF546E7A)
    val w = 35f * scaleX
    val h = 28f * scaleY

    // Beam 1: Backslash diagonal
    drawLine(
        color = steelColor,
        start = Offset(x - w / 2, y - h / 2),
        end = Offset(x + w / 2, y + h / 2),
        strokeWidth = 6.5f * scaleX,
        cap = StrokeCap.Square
    )
    drawLine(
        color = lightSteel,
        start = Offset(x - w / 2 + 1.5f * scaleX, y - h / 2),
        end = Offset(x + w / 2 + 1.5f * scaleX, y + h / 2),
        strokeWidth = 2f * scaleX,
        cap = StrokeCap.Square
    )

    // Beam 2: Forward-slash diagonal
    drawLine(
        color = steelColor,
        start = Offset(x - w / 2, y + h / 2),
        end = Offset(x + w / 2, y - h / 2),
        strokeWidth = 6.5f * scaleX,
        cap = StrokeCap.Square
    )

    // Beam 3: Supporting center vertical
    drawLine(
        color = Color(0xFF151D20),
        start = Offset(x, y + h / 2 + 3f * scaleY),
        end = Offset(x, y - h / 2 - 3f * scaleY),
        strokeWidth = 6.5f * scaleX,
        cap = StrokeCap.Square
    )

    // Highlight Bolt
    drawCircle(
        color = Color(0xFFB0BEC5),
        radius = 2.5f * scaleX,
        center = Offset(x, y)
    )
}

fun DrawScope.drawTrenchBackground3D(
    trenchVX: Float,
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float
) {
    val topY = 145f
    val bottomY = 295f

    // Calculate left and right lips of the trench throughout its vertical span
    val leftLipPoints = mutableListOf<Offset>()
    val rightLipPoints = mutableListOf<Offset>()

    val steps = 8
    val dy = (bottomY - topY) / steps

    for (i in 0..steps) {
        val vy = topY + i * dy
        val pl = project3D(trenchVX - 21f, vy, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
        val pr = project3D(trenchVX + 21f, vy, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
        leftLipPoints.add(pl)
        rightLipPoints.add(pr)
    }

    // 1. Draw the Deep Dark Trench Cavity
    val cavityPath = Path().apply {
        moveTo(leftLipPoints.first().x, leftLipPoints.first().y)
        for (i in 1..steps) {
            lineTo(leftLipPoints[i].x, leftLipPoints[i].y)
        }
        for (i in steps downTo 0) {
            lineTo(rightLipPoints[i].x, rightLipPoints[i].y)
        }
        close()
    }

    // Draw with an ambient occlusion vertical gradient (extremely dark towards bottom, muddy/wood-toned)
    drawPath(
        path = cavityPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E140F), Color(0xFF040302), Color(0xFF000000))
        )
    )

    // 2. Draw 3D Wooden Floor Planks inside the trench
    val plankHeightV = 10f
    for (i in 0 until steps step 2) {
        val pvy = topY + i * dy + 6f
        val proj = project3D(trenchVX, pvy, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val scale = proj.scale

        val p1 = project3D(trenchVX - 18f, pvy, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
        val p2 = project3D(trenchVX + 18f, pvy, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
        val p3 = project3D(trenchVX + 18f, pvy + plankHeightV, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
        val p4 = project3D(trenchVX - 18f, pvy + plankHeightV, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

        val plankPath = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            close()
        }

        // Wooden plank base
        drawPath(
            path = plankPath,
            color = Color(0xFF3E2723)
        )

        // Wood plank shadow line
        drawLine(
            color = Color(0xFF1B0C07),
            start = p4,
            end = p3,
            strokeWidth = 2f * scale
        )

        // Silver nails on left and right of wood planks
        drawCircle(
            color = Color(0xFF90A4AE),
            radius = 1.5f * scale,
            center = Offset(p1.x + (p2.x - p1.x) * 0.15f, p1.y + (p4.y - p1.y) * 0.5f)
        )
        drawCircle(
            color = Color(0xFF90A4AE),
            radius = 1.5f * scale,
            center = Offset(p1.x + (p2.x - p1.x) * 0.85f, p1.y + (p4.y - p1.y) * 0.5f)
        )
    }
}

fun DrawScope.drawTrenchForeground3D(
    trenchVX: Float,
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float
) {
    val topY = 145f
    val bottomY = 295f

    // Draw vertical wooden support logs that slant in 3D
    val steps = 8
    val dy = (bottomY - topY) / steps

    val pLeftTop = project3D(trenchVX - 21f, topY, virtualWidth, virtualHeight, screenWidth, screenHeight)
    val pLeftBot = project3D(trenchVX - 21f, bottomY, virtualWidth, virtualHeight, screenWidth, screenHeight)

    val pRightTop = project3D(trenchVX + 21f, topY, virtualWidth, virtualHeight, screenWidth, screenHeight)
    val pRightBot = project3D(trenchVX + 21f, bottomY, virtualWidth, virtualHeight, screenWidth, screenHeight)

    // Left support log
    drawLine(
        color = Color(0xFF4E342E),
        start = pLeftTop.offset,
        end = pLeftBot.offset,
        strokeWidth = 6.5f * pLeftBot.scale
    )
    drawLine(
        color = Color(0xFF8D6E63).copy(alpha = 0.4f),
        start = Offset(pLeftTop.offset.x + 1.5f, pLeftTop.offset.y),
        end = Offset(pLeftBot.offset.x + 1.5f, pLeftBot.offset.y),
        strokeWidth = 2f * pLeftBot.scale
    )

    // Right support log
    drawLine(
        color = Color(0xFF4E342E),
        start = pRightTop.offset,
        end = pRightBot.offset,
        strokeWidth = 6.5f * pRightBot.scale
    )
    drawLine(
        color = Color(0xFF8D6E63).copy(alpha = 0.4f),
        start = Offset(pRightTop.offset.x - 1.5f, pRightTop.offset.y),
        end = Offset(pRightBot.offset.x - 1.5f, pRightBot.offset.y),
        strokeWidth = 2f * pRightBot.scale
    )

    // Diagonal support struts
    for (i in listOf(2, 5)) {
        val y1 = topY + i * dy
        val y2 = y1 + 15f

        val p1 = project3D(trenchVX - 21f, y1, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val p2 = project3D(trenchVX - 21f + 12f, y2, virtualWidth, virtualHeight, screenWidth, screenHeight)

        val p3 = project3D(trenchVX + 21f, y1, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val p4 = project3D(trenchVX + 21f - 12f, y2, virtualWidth, virtualHeight, screenWidth, screenHeight)

        drawLine(
            color = Color(0xFF3E2723),
            start = p1.offset,
            end = p2.offset,
            strokeWidth = 4.5f * p1.scale
        )
        drawLine(
            color = Color(0xFF3E2723),
            start = p3.offset,
            end = p4.offset,
            strokeWidth = 4.5f * p3.scale
        )
    }

    // 3D ladder steps
    for (i in listOf(1, 3, 5, 7)) {
        val yStep = topY + i * dy
        val pL = project3D(trenchVX - 8f, yStep, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val pR = project3D(trenchVX + 8f, yStep, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val scale = pL.scale

        drawLine(
            color = Color(0xFF8D6E63),
            start = pL.offset,
            end = pR.offset,
            strokeWidth = 4.5f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(pL.offset.x, pL.offset.y + 1.5f * scale),
            end = Offset(pR.offset.x, pR.offset.y + 1.5f * scale),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round
        )
    }

    // Pile beautiful 3D sandbags over the lips of the trenches
    drawSandbagStack3D(
        vcx = trenchVX - 21f - 8f,
        vcy = topY + 4f,
        virtualWidth = virtualWidth,
        virtualHeight = virtualHeight,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        count = 3,
        isLeft = true
    )
    drawSandbagStack3D(
        vcx = trenchVX + 21f + 8f,
        vcy = topY + 4f,
        virtualWidth = virtualWidth,
        virtualHeight = virtualHeight,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        count = 3,
        isLeft = false
    )
}

fun DrawScope.drawSandbagStack3D(
    vcx: Float,
    vcy: Float,
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float,
    count: Int,
    isLeft: Boolean
) {
    val baseBagW = 28f
    val baseBagH = 11f

    for (i in 0 until count) {
        val xOffset = if (isLeft) -i * 2.5f else i * 2.5f
        val yOffset = -i * 7.5f

        val bagVCX = vcx + xOffset
        val bagVCY = vcy + yOffset

        val proj = project3D(bagVCX, bagVCY, virtualWidth, virtualHeight, screenWidth, screenHeight)
        val px = proj.offset.x
        val py = proj.offset.y
        val scale = proj.scale

        val bagW = baseBagW * scale * (screenWidth / virtualWidth)
        val bagH = baseBagH * scale * (screenHeight / virtualHeight)

        val bagLeft = px - bagW / 2
        val bagTop = py - bagH / 2

        // 1. Drop shadow beneath the bag
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(bagLeft + 2f * scale, bagTop + 2f * scale),
            size = Size(bagW, bagH)
        )

        // 2. 3D Jute Fabric body (radial gradient)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8D7662), Color(0xFF5D4E41)),
                center = Offset(px - bagW * 0.15f, py - bagH * 0.2f),
                radius = bagW * 0.6f
            ),
            topLeft = Offset(bagLeft, bagTop),
            size = Size(bagW, bagH)
        )

        // 3. Highlight line along the top crest
        drawOval(
            color = Color(0xFFBCAAA4).copy(alpha = 0.4f),
            topLeft = Offset(bagLeft + 1.5f * scale, bagTop + 1.2f * scale),
            size = Size(bagW - 3f * scale, bagH * 0.4f),
            style = Stroke(width = 1.2f * scale)
        )

        // 4. Middle crease seam
        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(bagLeft + 3f * scale, py + bagH * 0.05f),
            end = Offset(px + bagW / 2 - 3f * scale, py + bagH * 0.05f),
            strokeWidth = 1.8f * scale
        )

        // 5. Tied knot detail
        drawCircle(
            color = Color(0xFF3E2723),
            radius = 2.5f * scale,
            center = Offset(if (isLeft) px + bagW / 2 - 1.5f * scale else bagLeft + 1.5f * scale, py + bagH * 0.05f)
        )
    }
}

fun DrawScope.drawMilitaryBases(
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float,
    alliesHealthRatio: Float,
    axisHealthRatio: Float
) {
    val windSway = sin(System.currentTimeMillis() / 400.0).toFloat() * 5f

    // --- 1. ALLIES CONCRETE BUNKER (LEFT SIDE) ---
    val alliesBaseColor = if (alliesHealthRatio < 0.3f) Color(0xFFC62828) else Color(0xFF607D8B)
    val alliesDarkColor = Color(0xFF263238)

    val pFrontTopLeft = project3D(0f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pFrontTopRight = project3D(95f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pFrontBotLeft = project3D(0f, 300f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pFrontBotRight = project3D(95f, 300f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

    val pSideTopRight = project3D(125f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pSideBotRight = project3D(125f, 300f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

    // Drop shadow beneath Allies bunker
    val alliesShadowPath = Path().apply {
        moveTo(pFrontBotLeft.x, pFrontBotLeft.y)
        lineTo(pSideBotRight.x + 25f, pSideBotRight.y + 10f)
        lineTo(pSideBotRight.x, pSideBotRight.y)
        close()
    }
    drawPath(path = alliesShadowPath, color = Color.Black.copy(alpha = 0.4f))

    // Draw 3D Side Concrete Wall (protruding towards the battlefield, receives shadow)
    val alliesSidePath = Path().apply {
        moveTo(pFrontTopRight.x, pFrontTopRight.y)
        lineTo(pSideTopRight.x, pSideTopRight.y)
        lineTo(pSideBotRight.x, pSideBotRight.y)
        lineTo(pFrontBotRight.x, pFrontBotRight.y)
        close()
    }
    drawPath(
        path = alliesSidePath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF37474F), Color(0xFF21272B)),
            start = pFrontTopRight,
            end = pSideBotRight
        )
    )

    // Draw 3D Front Concrete Wall
    val alliesFrontPath = Path().apply {
        moveTo(pFrontTopLeft.x, pFrontTopLeft.y)
        lineTo(pFrontTopRight.x, pFrontTopRight.y)
        lineTo(pFrontBotRight.x, pFrontBotRight.y)
        lineTo(pFrontBotLeft.x, pFrontBotLeft.y)
        close()
    }
    drawPath(
        path = alliesFrontPath,
        brush = Brush.linearGradient(
            colors = listOf(alliesBaseColor, alliesBaseColor.copy(red = alliesBaseColor.red * 0.8f, green = alliesBaseColor.green * 0.8f, blue = alliesBaseColor.blue * 0.8f)),
            start = pFrontTopLeft,
            end = pFrontBotRight
        )
    )

    // Draw horizontal armor seams and rivets on the front concrete wall
    for (i in 1..3) {
        val fY = pFrontTopLeft.y + i * (pFrontBotLeft.y - pFrontTopLeft.y) * 0.25f
        val fYRight = pFrontTopRight.y + i * (pFrontBotRight.y - pFrontTopRight.y) * 0.25f
        drawLine(
            color = alliesDarkColor,
            start = Offset(pFrontTopLeft.x, fY),
            end = Offset(pFrontTopRight.x, fYRight),
            strokeWidth = 2f
        )
        for (rRatio in listOf(0.2f, 0.5f, 0.8f)) {
            val rx = pFrontTopLeft.x + (pFrontTopRight.x - pFrontTopLeft.x) * rRatio
            val ry = fY - 4f
            drawCircle(color = Color.LightGray, radius = 2.5f, center = Offset(rx, ry))
        }
    }

    // Machine gun port on the front wall
    val slitX = pFrontTopLeft.x + (pFrontTopRight.x - pFrontTopLeft.x) * 0.25f
    val slitY = pFrontTopLeft.y + (pFrontBotLeft.y - pFrontTopLeft.y) * 0.35f
    val slitW = (pFrontTopRight.x - pFrontTopLeft.x) * 0.5f
    val slitH = (pFrontBotLeft.y - pFrontTopLeft.y) * 0.12f
    drawRoundRect(
        color = Color(0xFF101416),
        topLeft = Offset(slitX, slitY),
        size = Size(slitW, slitH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRect(
        color = Color(0xFF212121),
        topLeft = Offset(slitX + slitW - 2f, slitY + slitH * 0.3f),
        size = Size(22f, 6f)
    )

    // Draw 3D Overhanging Concrete Roof plate
    val pRoofTopLeft = project3D(-10f, 138f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pRoofTopRight = project3D(100f, 138f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pRoofSideRight = project3D(130f, 138f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

    val alliesRoofPath = Path().apply {
        moveTo(pRoofTopLeft.x, pRoofTopLeft.y)
        lineTo(pRoofTopRight.x, pRoofTopRight.y)
        lineTo(pRoofSideRight.x, pRoofSideRight.y)
        lineTo(pSideTopRight.x, pSideTopRight.y)
        lineTo(pFrontTopLeft.x, pFrontTopLeft.y)
        close()
    }
    drawPath(
        path = alliesRoofPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF455A64), Color(0xFF37474F))
        )
    )

    // Sandbags piled in front of Allies bunker
    drawSandbagStack3D(105f, 280f, virtualWidth, virtualHeight, screenWidth, screenHeight, count = 4, isLeft = true)

    // Flagpole and Banner
    val pFlagPoleBase = project3D(40f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val flagPoleTopX = pFlagPoleBase.x
    val flagPoleTopY = pFlagPoleBase.y - 65f

    drawLine(
        color = Color(0xFFCFD8DC),
        start = Offset(flagPoleTopX, flagPoleTopY),
        end = pFlagPoleBase,
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFFFFD700), radius = 4f, center = Offset(flagPoleTopX, flagPoleTopY))

    val flagW = 32f
    val flagH = 20f
    val flagPath = Path().apply {
        moveTo(flagPoleTopX, flagPoleTopY)
        cubicTo(
            flagPoleTopX + flagW * 0.33f, flagPoleTopY + windSway,
            flagPoleTopX + flagW * 0.66f, flagPoleTopY - windSway,
            flagPoleTopX + flagW, flagPoleTopY + windSway * 0.5f
        )
        lineTo(flagPoleTopX + flagW, flagPoleTopY + flagH + windSway * 0.5f)
        cubicTo(
            flagPoleTopX + flagW * 0.66f, flagPoleTopY + flagH - windSway,
            flagPoleTopX + flagW * 0.33f, flagPoleTopY + flagH + windSway,
            flagPoleTopX, flagPoleTopY + flagH
        )
        close()
    }
    drawPath(path = flagPath, color = Color(0xFF2E7D32))
    drawCircle(color = Color(0xFFFFD700), radius = 4f, center = Offset(flagPoleTopX + flagW * 0.45f, flagPoleTopY + flagH * 0.5f))

    // --- 2. AXIS BRUTALIST STEEL BUNKER (RIGHT SIDE) ---
    val axisBaseColor = if (axisHealthRatio < 0.3f) Color(0xFFC62828) else Color(0xFF455A64)
    val axisDarkColor = Color(0xFF212121)

    val pAxisFrontTopLeft = project3D(905f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pAxisFrontTopRight = project3D(1000f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pAxisFrontBotLeft = project3D(905f, 300f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pAxisFrontBotRight = project3D(1000f, 300f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

    val pAxisSideTopLeft = project3D(875f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pAxisSideBotLeft = project3D(875f, 300f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

    // Shadow beneath Axis bunker
    val axisShadowPath = Path().apply {
        moveTo(pAxisFrontBotRight.x, pAxisFrontBotRight.y)
        lineTo(pAxisSideBotLeft.x - 25f, pAxisSideBotLeft.y + 10f)
        lineTo(pAxisSideBotLeft.x, pAxisSideBotLeft.y)
        close()
    }
    drawPath(path = axisShadowPath, color = Color.Black.copy(alpha = 0.4f))

    // Draw 3D Side Steel Wall (protruding towards the battlefield, receives shadow)
    val axisSidePath = Path().apply {
        moveTo(pAxisFrontTopLeft.x, pAxisFrontTopLeft.y)
        lineTo(pAxisSideTopLeft.x, pAxisSideTopLeft.y)
        lineTo(pAxisSideBotLeft.x, pAxisSideBotLeft.y)
        lineTo(pAxisFrontBotLeft.x, pAxisFrontBotLeft.y)
        close()
    }
    drawPath(
        path = axisSidePath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF263238), Color(0xFF10171A)),
            start = pAxisFrontTopLeft,
            end = pAxisSideBotLeft
        )
    )

    // Draw 3D Front Wall
    val axisFrontPath = Path().apply {
        moveTo(pAxisFrontTopLeft.x, pAxisFrontTopLeft.y)
        lineTo(pAxisFrontTopRight.x, pAxisFrontTopRight.y)
        lineTo(pAxisFrontBotRight.x, pAxisFrontBotRight.y)
        lineTo(pAxisFrontBotLeft.x, pAxisFrontBotLeft.y)
        close()
    }
    drawPath(
        path = axisFrontPath,
        brush = Brush.linearGradient(
            colors = listOf(axisBaseColor, axisBaseColor.copy(red = axisBaseColor.red * 0.75f, green = axisBaseColor.green * 0.75f, blue = axisBaseColor.blue * 0.75f)),
            start = pAxisFrontTopLeft,
            end = pAxisFrontBotRight
        )
    )

    // Iron panels, seams and rivets on the front wall
    for (i in 1..3) {
        val fY = pAxisFrontTopLeft.y + i * (pAxisFrontBotLeft.y - pAxisFrontTopLeft.y) * 0.25f
        val fYRight = pAxisFrontTopRight.y + i * (pAxisFrontBotRight.y - pAxisFrontTopRight.y) * 0.25f
        drawLine(
            color = axisDarkColor,
            start = Offset(pAxisFrontTopLeft.x, fY),
            end = Offset(pAxisFrontTopRight.x, fYRight),
            strokeWidth = 2f
        )
        for (rRatio in listOf(0.2f, 0.5f, 0.8f)) {
            val rx = pAxisFrontTopLeft.x + (pAxisFrontTopRight.x - pAxisFrontTopLeft.x) * rRatio
            val ry = fY - 4f
            drawCircle(color = Color(0xFF37474F), radius = 2.5f, center = Offset(rx, ry))
        }
    }

    // Machine gun viewpoint on the front wall
    val slitAX = pAxisFrontTopLeft.x + (pAxisFrontTopRight.x - pAxisFrontTopLeft.x) * 0.25f
    val slitAY = pAxisFrontTopLeft.y + (pAxisFrontBotLeft.y - pAxisFrontTopLeft.y) * 0.35f
    val slitAW = (pAxisFrontTopRight.x - pAxisFrontTopLeft.x) * 0.5f
    val slitAH = (pAxisFrontBotLeft.y - pAxisFrontTopLeft.y) * 0.12f
    drawRoundRect(
        color = Color(0xFF0D0D0D),
        topLeft = Offset(slitAX, slitAY),
        size = Size(slitAW, slitAH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRect(
        color = Color(0xFF151515),
        topLeft = Offset(slitAX - 20f, slitAY + slitAH * 0.3f),
        size = Size(22f, 6f)
    )

    // Overhanging Roof with hazard warning lines
    val pAxisRoofTopLeft = project3D(895f, 138f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pAxisRoofTopRight = project3D(1010f, 138f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val pAxisRoofSideLeft = project3D(865f, 138f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset

    val axisRoofPath = Path().apply {
        moveTo(pAxisRoofTopRight.x, pAxisRoofTopRight.y)
        lineTo(pAxisRoofTopLeft.x, pAxisRoofTopLeft.y)
        lineTo(pAxisRoofSideLeft.x, pAxisRoofSideLeft.y)
        lineTo(pAxisSideTopLeft.x, pAxisSideTopLeft.y)
        lineTo(pAxisFrontTopRight.x, pAxisFrontTopRight.y)
        close()
    }
    drawPath(path = axisRoofPath, color = Color(0xFF212121))

    // Sandbags piled in front of Axis bunker
    drawSandbagStack3D(895f, 280f, virtualWidth, virtualHeight, screenWidth, screenHeight, count = 4, isLeft = false)

    // Flag and flagpole for Axis
    val pAxisFlagPoleBase = project3D(960f, 150f, virtualWidth, virtualHeight, screenWidth, screenHeight).offset
    val axisFlagPoleTopX = pAxisFlagPoleBase.x
    val axisFlagPoleTopY = pAxisFlagPoleBase.y - 65f

    drawLine(
        color = Color(0xFF9E9E9E),
        start = Offset(axisFlagPoleTopX, axisFlagPoleTopY),
        end = pAxisFlagPoleBase,
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFFC62828), radius = 4f, center = Offset(axisFlagPoleTopX, axisFlagPoleTopY))

    val flagAW = 32f
    val flagAH = 20f
    val axisFlagPath = Path().apply {
        moveTo(axisFlagPoleTopX, axisFlagPoleTopY)
        cubicTo(
            axisFlagPoleTopX - flagAW * 0.33f, axisFlagPoleTopY - windSway,
            axisFlagPoleTopX - flagAW * 0.66f, axisFlagPoleTopY + windSway,
            axisFlagPoleTopX - flagAW, axisFlagPoleTopY - windSway * 0.5f
        )
        lineTo(axisFlagPoleTopX - flagAW, axisFlagPoleTopY + flagAH - windSway * 0.5f)
        cubicTo(
            axisFlagPoleTopX - flagAW * 0.66f, axisFlagPoleTopY + flagAH + windSway,
            axisFlagPoleTopX - flagAW * 0.33f, axisFlagPoleTopY + flagAH - windSway,
            axisFlagPoleTopX, axisFlagPoleTopY + flagAH
        )
        close()
    }
    drawPath(path = axisFlagPath, color = Color(0xFFC62828))
    drawCircle(color = Color.White, radius = 4f, center = Offset(axisFlagPoleTopX - flagAW * 0.45f, axisFlagPoleTopY + flagAH * 0.5f))
}

fun DrawScope.drawSoldier(
    u: SoldierUnit,
    virtualWidth: Float,
    virtualHeight: Float,
    screenWidth: Float,
    screenHeight: Float
) {
    val proj = project3D(u.x, u.y, virtualWidth, virtualHeight, screenWidth, screenHeight)
    val rawX = proj.offset.x
    val rawY = proj.offset.y
    val scaleX = (screenWidth / virtualWidth) * proj.scale
    val scaleY = (screenHeight / virtualHeight) * proj.scale

    // If dead, apply tilting falling and fading
    val alpha = if (u.state == UnitState.DEAD) (1f - u.deathTimer).coerceIn(0f, 1f) else 1f
    val rotation = if (u.state == UnitState.DEAD) u.deathTimer * 90f else 0f

    // Scale and tint factor based on state and merge level
    val inTrench = u.state == UnitState.TRENCH
    val baseScale = if (inTrench) 0.85f else 1.0f
    val levelMultiplier = when (u.level) {
        2 -> 1.25f
        3 -> 1.55f
        else -> 1.0f
    }
    val soldierScale = baseScale * levelMultiplier

    rotate(degrees = if (u.team == Team.ALLIES) rotation else -rotation, pivot = Offset(rawX, rawY)) {
        translate(left = 0f, top = 0f) {
            // Glowing elite aura beneath feet
            if (u.level > 1 && u.state != UnitState.DEAD && !inTrench) {
                val auraColor = if (u.level == 2) Color(0x77FFD700) else Color(0xAAFF5722)
                drawCircle(
                    color = auraColor,
                    radius = 16f * scaleX * soldierScale,
                    center = Offset(rawX, rawY),
                    style = Stroke(width = 2.5f * scaleX)
                )
            }

            // 1. Double-Layer Ambient Drop Shadow
            if (u.state != UnitState.DEAD) {
                val shadowAlpha = if (inTrench) 0.15f else 0.3f
                // Outer soft faint shadow (fuzzier)
                drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha * alpha),
                    topLeft = Offset(rawX - 18f * scaleX * soldierScale, rawY - 5f * scaleY * soldierScale + (if (inTrench) 0f else 2f * scaleY)),
                    size = Size(36f * scaleX * soldierScale, 10f * scaleY * soldierScale)
                )
                // Inner darker contact shadow
                drawOval(
                    color = Color.Black.copy(alpha = (shadowAlpha * 1.5f) * alpha),
                    topLeft = Offset(rawX - 12f * scaleX * soldierScale, rawY - 3f * scaleY * soldierScale + (if (inTrench) 0f else 2f * scaleY)),
                    size = Size(24f * scaleX * soldierScale, 6f * scaleY * soldierScale)
                )
            }

            // Walking / charging leg & body swing animation
            val legSwing = if (u.state == UnitState.CHARGING) sin(u.animFrame) * 12f else 0f
            val leftLegOffset = legSwing
            val rightLegOffset = -legSwing

            // Body walk bounce
            val walkBounce = if (u.state == UnitState.CHARGING) abs(sin(u.animFrame * 2f)) * 4f else 0f

            // Shading colors
            val uniformColor = when (u.team) {
                Team.ALLIES -> Color(0xFF4E5D30) // Muddy Olive Drab Green
                Team.AXIS -> Color(0xFF5E6E7A)   // Field Grey/Slate Grey
            }

            val helmetColor = when (u.team) {
                Team.ALLIES -> Color(0xFF7E8965) // Light Olive
                Team.AXIS -> Color(0xFF3F4D55)   // Dark Steel Grey
            }

            // Draw Legs (Stretched slightly for better proportion)
            val legStartY = rawY - 8f * scaleY * soldierScale - walkBounce * scaleY
            val legEndY = rawY - walkBounce * scaleY
            drawLine(
                color = Color(0xFF271510).copy(alpha = alpha), // Muddy brown combat boots
                start = Offset(rawX - 4f * scaleX * soldierScale, legStartY),
                end = Offset(rawX - 5f * scaleX * soldierScale + leftLegOffset * scaleX * soldierScale, legEndY),
                strokeWidth = 4.5f * scaleX * soldierScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF271510).copy(alpha = alpha),
                start = Offset(rawX + 4f * scaleX * soldierScale, legStartY),
                end = Offset(rawX + 5f * scaleX * soldierScale + rightLegOffset * scaleX * soldierScale, legEndY),
                strokeWidth = 4.5f * scaleX * soldierScale,
                cap = StrokeCap.Round
            )

            // Draw Torso
            val bodyWidth = (if (u.type == UnitType.ARMORED) 24f else 18f) * soldierScale
            val bodyHeight = (if (u.type == UnitType.ARMORED) 32f else 26f) * soldierScale
            val bodyTopLeftY = rawY - bodyHeight * scaleY - walkBounce * scaleY

            // Red flash when hit, else shaded torso
            val torsoColor = if (u.hitFlashTimer > 0f) Color.Red else uniformColor
            
            drawRoundRect(
                color = torsoColor.copy(alpha = alpha),
                topLeft = Offset(rawX - (bodyWidth / 2) * scaleX, bodyTopLeftY),
                size = Size(bodyWidth * scaleX, (bodyHeight - 6f) * scaleY),
                cornerRadius = CornerRadius(5f * scaleX * soldierScale, 5f * scaleY * soldierScale)
            )

            // Dynamic detail layers: Backpack & Belt
            if (!inTrench && u.state != UnitState.DEAD) {
                // Backpack / Bedroll
                val packColor = if (u.team == Team.ALLIES) Color(0xFF8D6E63) else Color(0xFF455A64)
                val packWidth = 6f * scaleX * soldierScale
                val packHeight = 16f * scaleY * soldierScale
                val packX = if (u.team == Team.ALLIES) rawX - (bodyWidth/2 + 2f) * scaleX else rawX + (bodyWidth/2 - 4f) * scaleX
                drawRoundRect(
                    color = packColor.copy(alpha = alpha),
                    topLeft = Offset(packX, bodyTopLeftY + 3f * scaleY),
                    size = Size(packWidth, packHeight),
                    cornerRadius = CornerRadius(2f * scaleX, 2f * scaleY)
                )

                // Leather belt & buckle
                val beltY = bodyTopLeftY + (bodyHeight - 12f) * scaleY
                drawRect(
                    color = Color(0xFF3E2723).copy(alpha = alpha), // Brown leather belt
                    topLeft = Offset(rawX - (bodyWidth / 2) * scaleX, beltY),
                    size = Size(bodyWidth * scaleX, 3f * scaleY)
                )
                drawRect(
                    color = Color(0xFFFFD700).copy(alpha = alpha), // Golden buckle
                    topLeft = Offset(rawX - 2f * scaleX, beltY - 0.5f * scaleY),
                    size = Size(4f * scaleX, 4f * scaleY)
                )

                // Harness straps (diagonal)
                drawLine(
                    color = Color(0xFF3E2723).copy(alpha = alpha),
                    start = Offset(rawX - (bodyWidth / 2) * scaleX, bodyTopLeftY),
                    end = Offset(rawX + 2f * scaleX, beltY),
                    strokeWidth = 1.5f * scaleX
                )
            }

            // Armored heavy chestplate details
            if (u.type == UnitType.ARMORED) {
                drawRoundRect(
                    color = Color(0xFF37474F).copy(alpha = alpha),
                    topLeft = Offset(rawX - 8f * scaleX * soldierScale, bodyTopLeftY + 4f * scaleY),
                    size = Size(16f * scaleX * soldierScale, 12f * scaleY * soldierScale),
                    cornerRadius = CornerRadius(3f * scaleX * soldierScale, 3f * scaleY * soldierScale)
                )
                // Metal bolts on chestplate
                drawCircle(color = Color.LightGray.copy(alpha = alpha), radius = 1.2f * scaleX, center = Offset(rawX - 5f * scaleX, bodyTopLeftY + 7f * scaleY))
                drawCircle(color = Color.LightGray.copy(alpha = alpha), radius = 1.2f * scaleX, center = Offset(rawX + 5f * scaleX, bodyTopLeftY + 7f * scaleY))
            }

            // Head Bobbing calculation
            val headBob = if (u.state == UnitState.CHARGING) abs(sin(u.animFrame)) * 3f else 0f

            // Draw Head
            val headRadius = (if (u.type == UnitType.ARMORED) 7f else 6f) * soldierScale
            val headY = rawY - bodyHeight * scaleY - headBob * scaleY - walkBounce * scaleY
            drawCircle(
                color = Color(0xFFFCD0A1).copy(alpha = alpha), // Skin peach
                radius = headRadius * scaleX,
                center = Offset(rawX, headY)
            )

            // Draw Helmet
            if (u.type == UnitType.ARMORED) {
                // Steel heavy visor / gasmask look
                drawArc(
                    color = Color(0xFF455A64).copy(alpha = alpha),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(rawX - 9f * scaleX * soldierScale, headY - 10f * scaleY * soldierScale),
                    size = Size(18f * scaleX * soldierScale, 18f * scaleY * soldierScale)
                )
                // Visor slit
                drawRect(
                    color = Color(0xFFFFB300).copy(alpha = alpha), // glowing orange slit
                    topLeft = Offset(rawX - 5f * scaleX, headY - 4f * scaleY),
                    size = Size(10f * scaleX, 2f * scaleY)
                )
            } else {
                // Regular military dome helmet (with chin straps!)
                drawArc(
                    color = helmetColor.copy(alpha = alpha),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(rawX - 8f * scaleX * soldierScale, headY - 8f * scaleY * soldierScale),
                    size = Size(16f * scaleX * soldierScale, 16f * scaleY * soldierScale)
                )
                // Chin strap
                drawLine(
                    color = Color(0xFF3E2723).copy(alpha = alpha),
                    start = Offset(rawX - 5f * scaleX * soldierScale, headY),
                    end = Offset(rawX + 5f * scaleX * soldierScale, headY),
                    strokeWidth = 1.2f * scaleX
                )
            }

            // Draw Level Starburst above helmet
            if (u.level > 1 && u.state != UnitState.DEAD) {
                val starY = headY - (headRadius + 14f) * scaleY * soldierScale
                val starColor = if (u.level == 2) Color(0xFFFFD700) else Color(0xFFFF5722) // Gold for level 2, Vibrant Orange for level 3
                val starSize = (if (u.level == 2) 5f else 7f) * scaleX * soldierScale

                // Sparkle starburst cross lines
                drawLine(color = starColor.copy(alpha = alpha), start = Offset(rawX - starSize, starY), end = Offset(rawX + starSize, starY), strokeWidth = 2.5f * scaleX)
                drawLine(color = starColor.copy(alpha = alpha), start = Offset(rawX, starY - starSize), end = Offset(rawX, starY + starSize), strokeWidth = 2.5f * scaleX)
                drawLine(color = starColor.copy(alpha = alpha), start = Offset(rawX - starSize * 0.7f, starY - starSize * 0.7f), end = Offset(rawX + starSize * 0.7f, starY + starSize * 0.7f), strokeWidth = 1.8f * scaleX)
                drawLine(color = starColor.copy(alpha = alpha), start = Offset(rawX + starSize * 0.7f, starY - starSize * 0.7f), end = Offset(rawX - starSize * 0.7f, starY + starSize * 0.7f), strokeWidth = 1.8f * scaleX)
                
                // Draw a small bright white center core
                drawCircle(color = Color.White.copy(alpha = alpha), radius = starSize * 0.35f, center = Offset(rawX, starY))

                // If level 3, draw two smaller side sparkles
                if (u.level >= 3) {
                    val sideOffset = 10f * scaleX * soldierScale
                    val sideSize = starSize * 0.6f
                    for (sideX in listOf(rawX - sideOffset, rawX + sideOffset)) {
                        drawLine(color = starColor.copy(alpha = alpha), start = Offset(sideX - sideSize, starY + 2f * scaleY), end = Offset(sideX + sideSize, starY + 2f * scaleY), strokeWidth = 1.5f * scaleX)
                        drawLine(color = starColor.copy(alpha = alpha), start = Offset(sideX, starY + 2f * scaleY - sideSize), end = Offset(sideX, starY + 2f * scaleY + sideSize), strokeWidth = 1.5f * scaleX)
                        drawCircle(color = Color.White.copy(alpha = alpha), radius = sideSize * 0.35f, center = Offset(sideX, starY + 2f * scaleY))
                    }
                }
            }

            // Weapons details (detailed rifles / bayonets)
            val gunXOffset = if (u.team == Team.ALLIES) 10f else -10f
            val weaponColor = Color(0xFF5D4037) // Wood stock
            val steelColor = Color(0xFF37474F)  // Gun steel

            if (u.type == UnitType.ARMORED) {
                // Large ballistic heavy shield in front
                val shieldX = if (u.team == Team.ALLIES) rawX + 11f * scaleX * soldierScale else rawX - 17f * scaleX * soldierScale
                // Outer glow shadow for shield
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.2f * alpha),
                    topLeft = Offset(shieldX + 1f * scaleX, rawY - 27f * scaleY - walkBounce * scaleY),
                    size = Size(6f * scaleX, 24f * scaleY),
                    cornerRadius = CornerRadius(2.5f * scaleX, 2.5f * scaleY)
                )
                // Shield body
                drawRoundRect(
                    color = Color(0xFF263238).copy(alpha = alpha),
                    topLeft = Offset(shieldX, rawY - 28f * scaleY - walkBounce * scaleY),
                    size = Size(6f * scaleX, 24f * scaleY),
                    cornerRadius = CornerRadius(2f * scaleX, 2f * scaleY)
                )
                // Viewport on the shield
                drawRect(
                    color = Color(0xFF00E5FF).copy(alpha = alpha),
                    topLeft = Offset(shieldX + 1.5f * scaleX, rawY - 24f * scaleY - walkBounce * scaleY),
                    size = Size(3f * scaleX, 4f * scaleY)
                )
                // Shield hazard striping or logo (yellow stripes)
                for (s in 0..1) {
                    val sy = rawY - 18f * scaleY - walkBounce * scaleY + s * 6f * scaleY
                    drawRect(
                        color = Color(0xFFFFD54F).copy(alpha = alpha),
                        topLeft = Offset(shieldX + 1f * scaleX, sy),
                        size = Size(4f * scaleX, 2.5f * scaleY)
                    )
                }
            } else if (u.type == UnitType.RIFLEMAN) {
                // Rifle barrel and stock
                val gunAngle = if (u.state == UnitState.ATTACKING) -10f else 15f
                val rotationPivot = Offset(rawX, rawY - 16f * scaleY - walkBounce * scaleY)
                rotate(
                    degrees = if (u.team == Team.ALLIES) gunAngle else -gunAngle,
                    pivot = rotationPivot
                ) {
                    val startX = rawX - 4f * scaleX * soldierScale
                    val startY = rawY - 14f * scaleY - walkBounce * scaleY
                    val midX = rawX + gunXOffset * scaleX * soldierScale
                    val midY = rawY - 16f * scaleY - walkBounce * scaleY
                    val tipX = rawX + gunXOffset * 1.8f * scaleX * soldierScale
                    val tipY = rawY - 17f * scaleY - walkBounce * scaleY

                    // Gun Stock (Brown wood)
                    drawLine(
                        color = weaponColor.copy(alpha = alpha),
                        start = Offset(startX, startY),
                        end = Offset(midX, midY),
                        strokeWidth = 3f * scaleX * soldierScale
                    )
                    // Gun Barrel (Black iron)
                    drawLine(
                        color = Color.Black.copy(alpha = alpha),
                        start = Offset(midX, midY),
                        end = Offset(tipX, tipY),
                        strokeWidth = 1.8f * scaleX * soldierScale
                    )

                    // GORGEOUS PROCEDURAL MUZZLE FLASH WHEN SHOOTING!
                    if (u.state == UnitState.ATTACKING && u.attackTimer > 0.8f) {
                        // Drawing a flashy star/spark at the muzzle tip
                        drawCircle(
                            color = Color(0xFFFFEB3B).copy(alpha = alpha), // bright yellow
                            radius = 6f * scaleX,
                            center = Offset(tipX, tipY)
                        )
                        drawCircle(
                            color = Color(0xFFFF9800).copy(alpha = alpha), // bright orange
                            radius = 3.2f * scaleX,
                            center = Offset(tipX, tipY)
                        )
                        // Tiny spark lines radiating outwards
                        for (angleDeg in listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)) {
                            val rad = Math.toRadians(angleDeg.toDouble())
                            val dx = (cos(rad) * 12f * scaleX).toFloat()
                            val dy = (sin(rad) * 12f * scaleX).toFloat()
                            drawLine(
                                color = Color(0xFFFFEB3B).copy(alpha = alpha),
                                start = Offset(tipX, tipY),
                                end = Offset(tipX + dx, tipY + dy),
                                strokeWidth = 1.6f * scaleX
                            )
                        }
                    }
                }
            } else {
                // Infantry Bayonet / Sword
                val swordAngle = if (u.state == UnitState.ATTACKING) -45f else 45f
                val rotationPivot = Offset(rawX, rawY - 16f * scaleY - walkBounce * scaleY)
                rotate(
                    degrees = if (u.team == Team.ALLIES) swordAngle else -swordAngle,
                    pivot = rotationPivot
                ) {
                    val tipX = rawX + gunXOffset * 1.5f * scaleX * soldierScale
                    val tipY = rawY - 26f * scaleY - walkBounce * scaleY

                    // Blade
                    drawLine(
                        color = Color(0xFFCFD8DC).copy(alpha = alpha), // shiny steel
                        start = Offset(rawX, rawY - 16f * scaleY - walkBounce * scaleY),
                        end = Offset(tipX, tipY),
                        strokeWidth = 2f * scaleX * soldierScale
                    )
                    // Golden hilt
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha),
                        radius = 2f * scaleX * soldierScale,
                        center = Offset(rawX + gunXOffset * 0.3f * scaleX * soldierScale, rawY - 18f * scaleY - walkBounce * scaleY)
                    )

                    // BEAUTIFUL TRANSPARENT WIND SLASH SWIPE WHEN ATTACKING!
                    if (u.state == UnitState.ATTACKING && u.attackTimer > 0.4f) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.5f * alpha),
                            startAngle = if (u.team == Team.ALLIES) -60f else 120f,
                            sweepAngle = if (u.team == Team.ALLIES) 110f else -110f,
                            useCenter = false,
                            topLeft = Offset(rawX - 8f * scaleX, rawY - 26f * scaleY),
                            size = Size(30f * scaleX, 30f * scaleY),
                            style = Stroke(width = 2.2f * scaleX)
                        )
                    }
                }
            }

            // Depth/shadow overlay if inside the trench cut to blend them beautifully
            if (inTrench) {
                // Draw a matching dark shadow mask over the soldier's entire body
                drawRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(rawX - (bodyWidth / 2) * scaleX - 4f * scaleX, headY - 10f * scaleY),
                    size = Size(bodyWidth * scaleX + 8f * scaleX, rawY - headY + 12f * scaleY)
                )
            }

            // Health bar above units if they aren't waiting in the trench or dead
            if (u.state != UnitState.TRENCH && u.state != UnitState.DEAD) {
                val barW = 20f * scaleX
                val barH = 3f * scaleY
                val barX = rawX - barW / 2
                val barY = headY - 12f * scaleY

                // Red track
                drawRect(
                    color = Color.Red.copy(alpha = 0.5f),
                    topLeft = Offset(barX, barY),
                    size = Size(barW, barH)
                )
                // Green health filled
                val fillRatio = u.health / u.maxHealth
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(barX, barY),
                    size = Size(barW * fillRatio, barH)
                )
            }
        }
    }
}
