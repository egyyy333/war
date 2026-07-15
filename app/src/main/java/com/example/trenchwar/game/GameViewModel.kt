package com.example.trenchwar.game

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("trench_war_prefs", Context.MODE_PRIVATE)
    private val random = Random()

    // Screen Dimensions
    val virtualWidth = 1000f
    val virtualHeight = 450f

    // Positions of Trenches & HQ
    val alliesTrenchX = 180f
    val axisTrenchX = 820f
    val alliesHQEndX = 110f
    val axisHQStartX = 890f

    // Standard road bounds where soldiers walk
    val roadTopY = 160f
    val roadBottomY = 280f

    // Main Game State Variables
    private val _currentStage = MutableStateFlow(1)
    val currentStage: StateFlow<Int> = _currentStage.asStateFlow()

    private val _highestStage = MutableStateFlow(1)
    val highestStage: StateFlow<Int> = _highestStage.asStateFlow()

    // Gameplay UI properties
    private val _alliesHQHealth = MutableStateFlow(1000f)
    val alliesHQHealth = _alliesHQHealth.asStateFlow()
    val alliesHQMaxHealth = 1000f

    private val _axisHQHealth = MutableStateFlow(1000f)
    val axisHQHealth = _axisHQHealth.asStateFlow()
    val axisHQMaxHealth = 1000f

    // Trench Slot state lists (Up to 3 slots for Allies and Axis)
    private val _alliesTrenchGroups = MutableStateFlow<List<SoldierGroup>>(emptyList())
    val alliesTrenchGroups = _alliesTrenchGroups.asStateFlow()

    private val _axisTrenchGroups = MutableStateFlow<List<SoldierGroup>>(emptyList())
    val axisTrenchGroups = _axisTrenchGroups.asStateFlow()

    // Units currently active and fighting on the battlefield
    private val _activeUnits = MutableStateFlow<List<SoldierUnit>>(emptyList())
    val activeUnits = _activeUnits.asStateFlow()

    // Particles and bullets for visuals
    private val _particles = MutableStateFlow<List<Particle>>(emptyList())
    val particles = _particles.asStateFlow()

    private val _bullets = MutableStateFlow<List<Bullet>>(emptyList())
    val bullets = _bullets.asStateFlow()

    // Game Control State
    enum class GameState {
        MENU,
        STAGE_SELECT,
        PLAYING,
        PAUSED,
        VICTORY,
        DEFEAT
    }

    private val _gameState = MutableStateFlow(GameState.MENU)
    val gameState = _gameState.asStateFlow()

    // Floating text items for damage numbers
    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts = _floatingTexts.asStateFlow()

    // Toggle for Automatic deployment
    private val _autoDeploy = MutableStateFlow(false)
    val autoDeploy = _autoDeploy.asStateFlow()

    // Screen Shake state (decreases to 0f)
    private val _screenShakeTime = MutableStateFlow(0f)
    val screenShakeTime = _screenShakeTime.asStateFlow()

    fun triggerScreenShake(duration: Float) {
        _screenShakeTime.value = duration
    }

    fun toggleAutoDeploy() {
        _autoDeploy.value = !_autoDeploy.value
        if (_autoDeploy.value) {
            val currentTrench = _alliesTrenchGroups.value.toList()
            for (g in currentTrench) {
                deployAlliesGroup(g.id)
            }
        }
    }

    fun mergeTrenchGroups() {
        if (_gameState.value != GameState.PLAYING) return
        val currentGroups = _alliesTrenchGroups.value.toMutableList()
        if (currentGroups.size < 2) {
            val listTexts = _floatingTexts.value.toMutableList()
            listTexts.add(
                FloatingText(
                    text = "⚠️ تحتاج فرقتين على الأقل بالخندق لدمجهما!",
                    x = alliesTrenchX,
                    y = roadTopY + 50f,
                    color = Color(0xFFFFCC00),
                    life = 1.5f
                )
            )
            _floatingTexts.value = listTexts
            return
        }

        var merged = false
        for (i in 0 until currentGroups.size) {
            for (j in i + 1 until currentGroups.size) {
                val g1 = currentGroups[i]
                val g2 = currentGroups[j]
                if (g1.type == g2.type && g1.level == g2.level) {
                    val newLevel = g1.level + 1
                    g1.level = newLevel

                    // Upgrade first group's soldiers
                    val activeList = _activeUnits.value.map { it.copy() }.toMutableList()
                    activeList.removeAll { it.groupId == g2.id }

                    for (unit in activeList) {
                        if (unit.groupId == g1.id) {
                            unit.level = newLevel
                            unit.health = unit.maxHealth * (1f + 0.8f * (newLevel - 1))
                        }
                    }
                    _activeUnits.value = activeList

                    // Remove combined group
                    currentGroups.removeAt(j)

                    // Spawns golden sparks
                    val listParticles = _particles.value.toMutableList()
                    spawnSparks(alliesTrenchX, roadTopY + 40f, listParticles, Color(0xFFFFD700), 20)
                    spawnSparks(alliesTrenchX, roadBottomY - 40f, listParticles, Color(0xFFFFD700), 20)
                    _particles.value = listParticles

                    // Pop text
                    val listTexts = _floatingTexts.value.toMutableList()
                    listTexts.add(
                        FloatingText(
                            text = "⚡ دمج نخبة لـ ${g1.type.titleAr} (مستوى $newLevel) ⚡",
                            x = alliesTrenchX,
                            y = roadTopY + 30f,
                            color = Color(0xFFFFD700),
                            life = 1.8f,
                            isCritical = true
                        )
                    )
                    _floatingTexts.value = listTexts

                    _alliesTrenchGroups.value = currentGroups
                    SoundManager.playVictory()
                    merged = true
                    break
                }
            }
            if (merged) break
        }

        if (!merged) {
            val listTexts = _floatingTexts.value.toMutableList()
            listTexts.add(
                FloatingText(
                    text = "❌ لا توجد مجموعات متشابهة لدمجها!",
                    x = alliesTrenchX,
                    y = roadTopY + 50f,
                    color = Color(0xFFFF8A80),
                    life = 1.2f
                )
            )
            _floatingTexts.value = listTexts
        }
    }

    // Deployment stats
    private val _deployedUnitsCount = MutableStateFlow(0)
    val deployedUnitsCount = _deployedUnitsCount.asStateFlow()

    private val _survivedUnitsCount = MutableStateFlow(0)
    val survivedUnitsCount = _survivedUnitsCount.asStateFlow()

    // Card Recruitment Cooldowns (Progress 0f to 1f, where 1f is fully cooled down/ready)
    private val _infantryCooldown = MutableStateFlow(1f)
    val infantryCooldown = _infantryCooldown.asStateFlow()

    private val _riflemanCooldown = MutableStateFlow(1f)
    val riflemanCooldown = _riflemanCooldown.asStateFlow()

    private val _armoredCooldown = MutableStateFlow(1f)
    val armoredCooldown = _armoredCooldown.asStateFlow()

    // CoolDown Trackers
    private var infantryLastRecruitTime = 0L
    private var riflemanLastRecruitTime = 0L
    private var armoredLastRecruitTime = 0L

    // Game Loop Job
    private var gameLoopJob: Job? = null

    // AI logic state
    private var aiLastSpawnTime = 0L
    private var aiLastDecisionTime = 0L

    init {
        // Load highest unlocked stage
        _highestStage.value = sharedPrefs.getInt("highest_stage", 1)
        _currentStage.value = _highestStage.value
    }

    fun setGameState(state: GameState) {
        _gameState.value = state
        if (state == GameState.PLAYING) {
            startGameLoop()
        } else {
            stopGameLoop()
        }
    }

    fun selectStage(stage: Int) {
        _currentStage.value = stage
        setGameState(GameState.PLAYING)
        resetBattle()
    }

    fun nextStage() {
        val next = _currentStage.value + 1
        _currentStage.value = next
        if (next > _highestStage.value) {
            _highestStage.value = next
            sharedPrefs.edit().putInt("highest_stage", next).apply()
        }
        resetBattle()
        setGameState(GameState.PLAYING)
    }

    fun retryStage() {
        resetBattle()
        setGameState(GameState.PLAYING)
    }

    private fun resetBattle() {
        _alliesHQHealth.value = alliesHQMaxHealth
        _axisHQHealth.value = axisHQMaxHealth
        _alliesTrenchGroups.value = emptyList()
        _axisTrenchGroups.value = emptyList()
        _activeUnits.value = emptyList()
        _particles.value = emptyList()
        _bullets.value = emptyList()
        _floatingTexts.value = emptyList()
        _screenShakeTime.value = 0f
        _deployedUnitsCount.value = 0
        _survivedUnitsCount.value = 0

        // Reset cooldowns
        _infantryCooldown.value = 1f
        _riflemanCooldown.value = 1f
        _armoredCooldown.value = 1f
        infantryLastRecruitTime = 0L
        riflemanLastRecruitTime = 0L
        armoredLastRecruitTime = 0L

        aiLastSpawnTime = System.currentTimeMillis()
        aiLastDecisionTime = System.currentTimeMillis()
    }

    fun pauseGame() {
        if (_gameState.value == GameState.PLAYING) {
            setGameState(GameState.PAUSED)
        }
    }

    fun resumeGame() {
        if (_gameState.value == GameState.PAUSED) {
            setGameState(GameState.PLAYING)
        }
    }

    // --- RECRUITMENT LOGIC ---
    fun recruitUnit(type: UnitType) {
        if (_gameState.value != GameState.PLAYING) return

        val now = System.currentTimeMillis()
        val lastTime = when (type) {
            UnitType.INFANTRY -> infantryLastRecruitTime
            UnitType.RIFLEMAN -> riflemanLastRecruitTime
            UnitType.ARMORED -> armoredLastRecruitTime
        }

        if (now - lastTime < type.cooldownMs) {
            // Still on cooldown
            return
        }

        // Trigger recruitment
        when (type) {
            UnitType.INFANTRY -> infantryLastRecruitTime = now
            UnitType.RIFLEMAN -> riflemanLastRecruitTime = now
            UnitType.ARMORED -> armoredLastRecruitTime = now
        }

        // Spawn a group and add to Allies Trench
        spawnGroup(type, Team.ALLIES)
        _deployedUnitsCount.value += type.groupSize

        if (_autoDeploy.value) {
            val currentTrench = _alliesTrenchGroups.value
            if (currentTrench.isNotEmpty()) {
                deployAlliesGroup(currentTrench.last().id)
            }
        }
    }

    private fun spawnGroup(type: UnitType, team: Team) {
        val group = SoldierGroup(type = type, team = team)
        val listUnits = mutableListOf<SoldierUnit>()

        // Generate Y positions neatly separated inside the road
        val startX = if (team == Team.ALLIES) alliesTrenchX else axisTrenchX
        val ySpacing = (roadBottomY - roadTopY) / (type.groupSize + 1)

        for (i in 0 until type.groupSize) {
            val soldierY = roadTopY + (i + 1) * ySpacing + (random.nextFloat() * 10f - 5f)
            val unit = SoldierUnit(
                groupId = group.id,
                type = type,
                team = team,
                x = startX,
                y = soldierY,
                health = type.maxHealth,
                maxHealth = type.maxHealth,
                state = UnitState.TRENCH
            )
            listUnits.add(unit)
            group.soldierIds.add(unit.id)
        }

        // Add units to active collection
        val currentActive = _activeUnits.value.toMutableList()
        currentActive.addAll(listUnits)
        _activeUnits.value = currentActive

        // Add group to Trench
        if (team == Team.ALLIES) {
            val currentTrench = _alliesTrenchGroups.value.toMutableList()
            if (currentTrench.size >= 3) {
                // Force oldest group out (index 0)
                deployAlliesGroup(currentTrench[0].id)
                // Re-read current trench list since deployAlliesGroup removes it
                val updatedTrench = _alliesTrenchGroups.value.toMutableList()
                updatedTrench.add(group)
                _alliesTrenchGroups.value = updatedTrench
            } else {
                currentTrench.add(group)
                _alliesTrenchGroups.value = currentTrench
            }
        } else {
            // Axis AI Trench
            val currentTrench = _axisTrenchGroups.value.toMutableList()
            if (currentTrench.size >= 3) {
                deployAxisGroup(currentTrench[0].id)
                val updatedTrench = _axisTrenchGroups.value.toMutableList()
                updatedTrench.add(group)
                _axisTrenchGroups.value = updatedTrench
            } else {
                currentTrench.add(group)
                _axisTrenchGroups.value = currentTrench
            }
        }
    }

    fun deployAlliesGroup(groupId: String) {
        val currentTrench = _alliesTrenchGroups.value.toMutableList()
        val index = currentTrench.indexOfFirst { it.id == groupId }
        if (index != -1) {
            val group = currentTrench.removeAt(index)
            _alliesTrenchGroups.value = currentTrench

            // Set all soldiers in this group to CHARGING
            val active = _activeUnits.value.map { unit ->
                if (unit.groupId == group.id) {
                    unit.copy().apply { state = UnitState.CHARGING }
                } else {
                    unit
                }
            }
            _activeUnits.value = active
            SoundManager.playStep()
        }
    }

    private fun deployAxisGroup(groupId: String) {
        val currentTrench = _axisTrenchGroups.value.toMutableList()
        val index = currentTrench.indexOfFirst { it.id == groupId }
        if (index != -1) {
            val group = currentTrench.removeAt(index)
            _axisTrenchGroups.value = currentTrench

            val active = _activeUnits.value.map { unit ->
                if (unit.groupId == group.id) {
                    unit.copy().apply { state = UnitState.CHARGING }
                } else {
                    unit
                }
            }
            _activeUnits.value = active
            SoundManager.playStep()
        }
    }

    // --- GAME LOOP ---
    private fun startGameLoop() {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (_gameState.value == GameState.PLAYING) {
                val now = System.currentTimeMillis()
                val delta = (now - lastTime) / 1000f // time elapsed in seconds
                lastTime = now

                updateGame(delta)
                updateCooldowns(now)
                handleAI(now)

                delay(16) // Target ~60 FPS
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun updateCooldowns(now: Long) {
        _infantryCooldown.value = ((now - infantryLastRecruitTime).toFloat() / UnitType.INFANTRY.cooldownMs).coerceIn(0f, 1f)
        _riflemanCooldown.value = ((now - riflemanLastRecruitTime).toFloat() / UnitType.RIFLEMAN.cooldownMs).coerceIn(0f, 1f)
        _armoredCooldown.value = ((now - armoredLastRecruitTime).toFloat() / UnitType.ARMORED.cooldownMs).coerceIn(0f, 1f)
    }

    private fun updateGame(dt: Float) {
        if (dt <= 0f) return

        // Update Screen Shake
        if (_screenShakeTime.value > 0f) {
            _screenShakeTime.value = (_screenShakeTime.value - dt).coerceAtLeast(0f)
        }

        // Update Floating Texts
        val currentTexts = _floatingTexts.value.map { it.copy() }.toMutableList()
        val textIterator = currentTexts.iterator()
        while (textIterator.hasNext()) {
            val t = textIterator.next()
            t.life -= dt
            if (t.life <= 0f) {
                textIterator.remove()
            } else {
                t.y -= 30f * dt // Float upward
            }
        }
        _floatingTexts.value = currentTexts

        // Copies of the list for safe mutation
        val listUnits = _activeUnits.value.map { it.copy() }.toMutableList()
        val listBullets = _bullets.value.toMutableList()
        val listParticles = _particles.value.toMutableList()

        // 1. Update existing Particles
        val particleIterator = listParticles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.life -= dt
            if (p.life <= 0f) {
                particleIterator.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
            }
        }

        // 2. Update Bullets
        val bulletIterator = listBullets.iterator()
        while (bulletIterator.hasNext()) {
            val b = bulletIterator.next()
            // Move bullet towards target
            val dx = b.targetX - b.x
            val dy = b.targetY - b.y
            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val travel = b.speed * dt

            if (travel >= dist) {
                // Bullet hit its target coordinate
                bulletIterator.remove()
                // Deal damage to any enemy near target X/Y
                val targetUnit = listUnits.firstOrNull { u ->
                    u.team != b.team && u.state != UnitState.TRENCH && u.state != UnitState.DEAD &&
                            Math.abs(u.x - b.targetX) < 40f
                }
                if (targetUnit != null) {
                    damageUnit(targetUnit, b.damage, listParticles)
                }
            } else {
                b.x += (dx / dist) * travel
                b.y += (dy / dist) * travel
            }
        }

        // 3. Update Units
        val livingAllies = listUnits.filter { it.team == Team.ALLIES && it.state != UnitState.DEAD }
        val livingAxis = listUnits.filter { it.team == Team.AXIS && it.state != UnitState.DEAD }

        for (u in listUnits) {
            if (u.state == UnitState.TRENCH) {
                continue // Waiting in the trench, do nothing
            }

            if (u.state == UnitState.DEAD) {
                u.deathTimer += dt * 4f // Die in 0.25 seconds
                if (u.deathTimer >= 1.0f) {
                    // Handled later during removal
                }
                continue
            }

            // Update timers
            if (u.attackTimer > 0f) u.attackTimer -= dt
            if (u.hitFlashTimer > 0f) u.hitFlashTimer -= dt

            // Check if walking animation frame needs incrementing
            if (u.state == UnitState.CHARGING) {
                u.animFrame += dt * 8f * u.type.speed
                if (u.animFrame >= 2 * Math.PI) {
                    u.animFrame -= (2 * Math.PI).toFloat()
                    if (u.team == Team.ALLIES && random.nextFloat() < 0.05f) {
                        SoundManager.playStep()
                    }
                }
                // Spawn dynamic kicking footstep dust/mud particles!
                if (random.nextFloat() < 0.12f) {
                    listParticles.add(
                        Particle(
                            x = u.x + (random.nextFloat() * 12f - 6f),
                            y = u.y + (random.nextFloat() * 6f - 3f),
                            vx = if (u.team == Team.ALLIES) -20f - random.nextFloat() * 20f else 20f + random.nextFloat() * 20f,
                            vy = -15f - random.nextFloat() * 20f,
                            color = Color(0xFF8D6E63).copy(alpha = 0.5f), // dusty light brown
                            size = 2f + random.nextFloat() * 3f,
                            life = 0.2f + random.nextFloat() * 0.2f,
                            maxLife = 0.4f
                        )
                    )
                }
            }

            // Find targets and act
            if (u.team == Team.ALLIES) {
                // Look for closest living Axis unit that has climbed out of the trench
                val enemiesAhead = livingAxis.filter { it.state != UnitState.TRENCH && it.x > u.x }
                val closestEnemy = enemiesAhead.minByOrNull { it.x - u.x }

                if (closestEnemy != null && (closestEnemy.x - u.x) <= u.type.attackRange) {
                    // Attack enemy unit
                    u.state = UnitState.ATTACKING
                    u.targetUnitId = closestEnemy.id
                    u.isAttackingHQ = false

                    if (u.attackTimer <= 0f) {
                        performAttack(u, closestEnemy, listBullets, listParticles)
                    }
                } else if ((axisHQStartX - u.x) <= u.type.attackRange) {
                    // Attack enemy HQ
                    u.state = UnitState.ATTACKING
                    u.targetUnitId = null
                    u.isAttackingHQ = true

                    if (u.attackTimer <= 0f) {
                        val finalDmg = u.type.damage * (1f + 0.8f * (u.level - 1))
                        _axisHQHealth.value = (_axisHQHealth.value - finalDmg).coerceAtLeast(0f)
                        u.attackTimer = 1.0f // attack delay
                        SoundManager.playHit()
                        triggerScreenShake(0.3f)
                        spawnSparks(axisHQStartX, u.y, listParticles, Color(0xFFE91E63), 10)

                        val listTexts = _floatingTexts.value.toMutableList()
                        listTexts.add(
                            FloatingText(
                                text = "-${finalDmg.toInt()}",
                                x = axisHQStartX,
                                y = u.y - 30f,
                                color = Color(0xFFE91E63),
                                life = 1.2f,
                                isCritical = u.level > 1
                            )
                        )
                        _floatingTexts.value = listTexts

                        if (_axisHQHealth.value <= 0f) {
                            handleVictory()
                        }
                    }
                } else {
                    // Keep marching forward
                    u.state = UnitState.CHARGING
                    u.targetUnitId = null
                    u.isAttackingHQ = false
                    u.x += u.type.speed * 45f * dt // move right
                }
            } else {
                // Team.AXIS
                val enemiesAhead = livingAllies.filter { it.state != UnitState.TRENCH && it.x < u.x }
                val closestEnemy = enemiesAhead.maxByOrNull { it.x } // Max X is closest to Axis unit moving left

                if (closestEnemy != null && (u.x - closestEnemy.x) <= u.type.attackRange) {
                    // Attack enemy unit
                    u.state = UnitState.ATTACKING
                    u.targetUnitId = closestEnemy.id
                    u.isAttackingHQ = false

                    if (u.attackTimer <= 0f) {
                        performAttack(u, closestEnemy, listBullets, listParticles)
                    }
                } else if ((u.x - alliesHQEndX) <= u.type.attackRange) {
                    // Attack allies HQ
                    u.state = UnitState.ATTACKING
                    u.targetUnitId = null
                    u.isAttackingHQ = true

                    if (u.attackTimer <= 0f) {
                        val finalDmg = u.type.damage * (1f + 0.8f * (u.level - 1))
                        _alliesHQHealth.value = (_alliesHQHealth.value - finalDmg).coerceAtLeast(0f)
                        u.attackTimer = 1.0f
                        SoundManager.playHit()
                        triggerScreenShake(0.3f)
                        spawnSparks(alliesHQEndX, u.y, listParticles, Color(0xFF4CAF50), 10)

                        val listTexts = _floatingTexts.value.toMutableList()
                        listTexts.add(
                            FloatingText(
                                text = "-${finalDmg.toInt()}",
                                x = alliesHQEndX,
                                y = u.y - 30f,
                                color = Color(0xFF4CAF50),
                                life = 1.2f,
                                isCritical = u.level > 1
                            )
                        )
                        _floatingTexts.value = listTexts

                        if (_alliesHQHealth.value <= 0f) {
                            handleDefeat()
                        }
                    }
                } else {
                    // Keep marching forward
                    u.state = UnitState.CHARGING
                    u.targetUnitId = null
                    u.isAttackingHQ = false
                    u.x -= u.type.speed * 45f * dt // move left
                }
            }
        }

        // Apply physical unit-separation and organic walk sways to prevent ugly clusters
        for (i in 0 until listUnits.size) {
            val u1 = listUnits[i]
            if (u1.state == UnitState.TRENCH || u1.state == UnitState.DEAD) continue
            var pushX = 0f
            var pushY = 0f
            for (j in 0 until listUnits.size) {
                if (i == j) continue
                val u2 = listUnits[j]
                if (u2.state == UnitState.TRENCH || u2.state == UnitState.DEAD) continue

                val dx = u1.x - u2.x
                val dy = u1.y - u2.y
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < 26f && dist > 0f) {
                    val force = (26f - dist) / 26f
                    pushX += (dx / dist) * force * 18f
                    pushY += (dy / dist) * force * 18f
                }
            }

            // Gently push units apart
            u1.x = (u1.x + pushX * dt).coerceIn(alliesHQEndX + 15f, axisHQStartX - 15f)
            u1.y = (u1.y + pushY * dt).coerceIn(roadTopY + 10f, roadBottomY - 10f)

            // Add slight tactical path weaving as they advance
            if (u1.state == UnitState.CHARGING) {
                val weave = Math.sin((u1.animFrame * 0.8f).toDouble()).toFloat() * 1.5f
                u1.y = (u1.y + weave * 12f * dt).coerceIn(roadTopY + 10f, roadBottomY - 10f)
            }
        }

        // 4. Remove completely dead and finished units
        val cleanUnits = listUnits.filter { !(it.state == UnitState.DEAD && it.deathTimer >= 1.0f) }
        _activeUnits.value = cleanUnits
        _bullets.value = listBullets
        _particles.value = listParticles

        // Update survival counts for scoring
        _survivedUnitsCount.value = cleanUnits.count { it.team == Team.ALLIES && it.state != UnitState.TRENCH && it.state != UnitState.DEAD }
    }

    private fun performAttack(attacker: SoldierUnit, target: SoldierUnit, bullets: MutableList<Bullet>, particles: MutableList<Particle>) {
        attacker.attackTimer = if (attacker.type.isRanged) 1.2f else 0.8f // weapon delay
        val finalDmg = attacker.type.damage * (1f + 0.8f * (attacker.level - 1))

        if (attacker.type.isRanged) {
            // Shoot ranged bullet
            SoundManager.playShoot()
            bullets.add(
                Bullet(
                    x = attacker.x,
                    y = attacker.y - 20f, // fire from chest height
                    targetX = target.x,
                    targetY = target.y - 15f,
                    speed = 650f, // bullet travel speed
                    team = attacker.team,
                    damage = finalDmg
                )
            )
            // Tiny muzzle flash spark at gun tip
            val flashX = if (attacker.team == Team.ALLIES) attacker.x + 15f else attacker.x - 15f
            spawnSparks(flashX, attacker.y - 20f, particles, Color.Yellow, 4)
        } else {
            // Melee swing damage directly
            SoundManager.playHit()
            damageUnit(target, finalDmg, particles)
            // Melee clash spark
            val sparkX = (attacker.x + target.x) / 2f
            spawnSparks(sparkX, target.y - 15f, particles, Color.White, 3)
        }
    }

    private fun damageUnit(target: SoldierUnit, damage: Float, particles: MutableList<Particle>) {
        target.health = (target.health - damage).coerceAtLeast(0f)
        target.hitFlashTimer = 0.15f // flash red for 150ms

        // Spawn dynamic floating text popups for damage
        val listTexts = _floatingTexts.value.toMutableList()
        val isCrit = target.level > 1 && random.nextFloat() < 0.4f
        val displayDmg = damage.toInt()
        val popupColor = if (target.team == Team.ALLIES) Color(0xFFFF8A80) else Color(0xFFECEFF1)
        
        listTexts.add(
            FloatingText(
                text = if (isCrit) "🔥 $displayDmg!" else "$displayDmg",
                x = target.x + (random.nextFloat() * 16f - 8f),
                y = target.y - 40f,
                color = if (isCrit) Color(0xFFFFD700) else popupColor,
                life = 1.0f,
                isCritical = isCrit
            )
        )
        _floatingTexts.value = listTexts

        if (isCrit) {
            triggerScreenShake(0.12f)
        }

        // Blood splatters (very short, small, dark red, disappearing fast)
        spawnBloodSplatter(target.x, target.y - 15f, particles)

        if (target.health <= 0f) {
            target.state = UnitState.DEAD
            target.deathTimer = 0f
            SoundManager.playDeath()
        } else {
            SoundManager.playHit()
        }
    }

    // --- EFFECTS SPREAD ---
    private fun spawnSparks(x: Float, y: Float, particles: MutableList<Particle>, color: Color, count: Int = 8) {
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2 * Math.PI
            val speed = 50f + random.nextFloat() * 100f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.sin(angle) * speed).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    color = color,
                    size = 3f + random.nextFloat() * 3f,
                    life = 0.15f + random.nextFloat() * 0.15f,
                    maxLife = 0.3f
                )
            )
        }
    }

    private fun spawnBloodSplatter(x: Float, y: Float, particles: MutableList<Particle>) {
        val darkRed = Color(0xFF8B0000)
        for (i in 0 until 5) {
            val angle = random.nextFloat() * 2 * Math.PI
            val speed = 30f + random.nextFloat() * 60f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.sin(angle) * speed).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    color = darkRed,
                    size = 4f + random.nextFloat() * 4f,
                    life = 0.12f + random.nextFloat() * 0.1f, // disappears very quickly
                    maxLife = 0.25f
                )
            )
        }
    }

    // --- WIN / LOSS CONDITIONS ---
    private fun handleVictory() {
        stopGameLoop()
        SoundManager.playVictory()
        setGameState(GameState.VICTORY)
    }

    private fun handleDefeat() {
        stopGameLoop()
        SoundManager.playDefeat()
        setGameState(GameState.DEFEAT)
    }

    // --- AI LOGIC ENGINE ---
    private fun handleAI(now: Long) {
        // AI spawns groups based on stages and timer
        val spawnInterval = if (_currentStage.value == 1) 13000L else 8500L
        if (now - aiLastSpawnTime > spawnInterval) {
            aiLastSpawnTime = now

            // Determine AI selection
            val type = decideAiUnitType()
            spawnGroup(type, Team.AXIS)
        }

        // AI deploys from its trench periodically or when triggered
        if (now - aiLastDecisionTime > 1500L) {
            aiLastDecisionTime = now

            val trenchList = _axisTrenchGroups.value
            if (trenchList.isNotEmpty()) {
                val shouldDeploy = shouldAiDeploy(trenchList.size)
                if (shouldDeploy) {
                    deployAxisGroup(trenchList[0].id)
                }
            }
        }
    }

    private fun decideAiUnitType(): UnitType {
        val r = random.nextFloat()
        if (_currentStage.value == 1) {
            // Stage 1 AI: Simpler unit distributions
            return if (r < 0.6f) UnitType.INFANTRY else if (r < 0.9f) UnitType.RIFLEMAN else UnitType.ARMORED
        } else {
            // Stage 2 AI: More aggressive with Armored tanks
            val playerUnits = _activeUnits.value.filter { it.team == Team.ALLIES && it.state != UnitState.TRENCH }
            val hasPlayerArmored = playerUnits.any { it.type == UnitType.ARMORED }

            return if (hasPlayerArmored) {
                // Counter armored with Armored or Riflemen
                if (r < 0.4f) UnitType.ARMORED else if (r < 0.8f) UnitType.RIFLEMAN else UnitType.INFANTRY
            } else {
                if (r < 0.4f) UnitType.INFANTRY else if (r < 0.75f) UnitType.RIFLEMAN else UnitType.ARMORED
            }
        }
    }

    private fun shouldAiDeploy(trenchSize: Int): Boolean {
        if (trenchSize >= 3) return true // Forced overflow

        // Assess if player units are pushing deep into our territory
        val activePlayerUnits = _activeUnits.value.filter { it.team == Team.ALLIES && it.state != UnitState.TRENCH }
        val playerIsDeep = activePlayerUnits.any { it.x > 500f }

        if (playerIsDeep) {
            // High urge to deploy defensively
            return random.nextFloat() < 0.85f
        }

        // Regular random deployment pressure
        return random.nextFloat() < 0.35f
    }
}
