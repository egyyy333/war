package com.example.trenchwar.game

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class Team {
    ALLIES, // Player (Left)
    AXIS    // AI (Right)
}

enum class UnitType(
    val titleAr: String,
    val titleEn: String,
    val cost: Int,               // Gold or timer cost
    val cooldownMs: Long,        // Recruitment cooldown
    val groupSize: Int,          // 4 for infantry, 3 for riflemen, 1 for armored
    val maxHealth: Float,
    val damage: Float,
    val attackRange: Float,      // Attack range in visual X units
    val speed: Float,            // Movement speed multiplier
    val isRanged: Boolean
) {
    INFANTRY("مشاة", "Infantry", 100, 7000L, 4, 120f, 25f, 35f, 1.2f, false),
    RIFLEMAN("رماة", "Rifleman", 150, 11000L, 3, 90f, 18f, 220f, 1.0f, true),
    ARMORED("مدرع", "Armored", 250, 18000L, 1, 380f, 40f, 40f, 0.6f, false)
}

enum class UnitState {
    TRENCH,    // Waiting inside the trench
    CHARGING,  // Climbing out and advancing towards enemy
    ATTACKING, // Attacking an enemy unit or the enemy base
    DEAD       // Died, falling/fading animation
}

data class SoldierUnit(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val type: UnitType,
    val team: Team,
    var x: Float,                      // Position X on a 1000-unit virtual scale
    var y: Float,                      // Position Y (vertical offset inside road width, e.g. 150f to 250f)
    var health: Float,
    val maxHealth: Float,
    var state: UnitState = UnitState.TRENCH,
    var attackTimer: Float = 0f,       // Time until next attack
    var hitFlashTimer: Float = 0f,     // Red flash timer when hit
    var deathTimer: Float = 0f,        // Progress of death animation (0.0 to 1.0)
    var animFrame: Float = 0f,         // Progress of walking leg swing / animation
    var targetUnitId: String? = null,  // ID of unit currently attacking
    var isAttackingHQ: Boolean = false, // If true, attacking the enemy base
    var level: Int = 1                 // Level/Tier: 1 = Normal, 2 = Elite, 3 = Champion
) {
    fun copy(): SoldierUnit {
        return SoldierUnit(
            id = id,
            groupId = groupId,
            type = type,
            team = team,
            x = x,
            y = y,
            health = health,
            maxHealth = maxHealth,
            state = state,
            attackTimer = attackTimer,
            hitFlashTimer = hitFlashTimer,
            deathTimer = deathTimer,
            animFrame = animFrame,
            targetUnitId = targetUnitId,
            isAttackingHQ = isAttackingHQ,
            level = level
        )
    }
}

data class SoldierGroup(
    val id: String = UUID.randomUUID().toString(),
    val type: UnitType,
    val team: Team,
    var state: UnitState = UnitState.TRENCH,
    val soldierIds: MutableList<String> = mutableListOf(),
    var level: Int = 1                 // Group Level (1 = Normal, 2 = Elite)
)

data class FloatingText(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float,
    var y: Float,
    val color: Color,
    var life: Float = 1.0f,            // Duration in seconds
    val isCritical: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var life: Float,       // current life
    val maxLife: Float     // max life in seconds/frames
)

data class Bullet(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    val targetX: Float,
    val targetY: Float,
    val speed: Float,
    val team: Team,
    val damage: Float
)
