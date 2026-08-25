package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppLockManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinLockScreen(
    appLockManager: AppLockManager,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Shake offset animation for invalid PIN
    val shakeOffset = remember { Animatable(0f) }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < 4) {
            isError = false
            errorMessage = null
            val newPin = enteredPin + digit
            enteredPin = newPin

            if (newPin.length == 4) {
                // Verify PIN
                scope.launch {
                    delay(80L)
                    val success = appLockManager.verifyPin(newPin)
                    if (!success) {
                        isError = true
                        errorMessage = "Incorrect PIN. Please try again."
                        // Trigger shake animation
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                0f at 0
                                (-20f) at 50
                                20f at 100
                                (-15f) at 150
                                15f at 200
                                (-8f) at 250
                                8f at 300
                                0f at 400
                            }
                        )
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            isError = false
            errorMessage = null
            enteredPin = enteredPin.dropLast(1)
        }
    }

    fun onClear() {
        enteredPin = ""
        isError = false
        errorMessage = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkSurface,
                        Color(0xFF0F172A)
                    )
                )
            )
            .testTag("pin_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER: Security Icon & Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // Shield / Lock Icon Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    PrimaryEmerald.copy(alpha = 0.25f),
                                    Color(0xFF0D9488).copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Locked",
                        tint = PrimaryLightEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "App Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SmoothWhite
                )

                Text(
                    text = "Enter your 4-digit PIN to access your financial ledger and bank balances",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // PIN DOTS ROW (with shake animation)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        val dotColor = when {
                            isError -> RedExpense
                            isFilled -> PrimaryLightEmerald
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) dotColor else Color.Transparent)
                                .then(
                                    if (!isFilled) {
                                        Modifier.background(
                                            color = DarkSurfaceVariant,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isFilled) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedExpense,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // MIDDLE / BOTTOM: CUSTOM NUMPAD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Row 1: 1, 2, 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NumpadKey(text = "1", onClick = { onDigitPress("1") })
                    NumpadKey(text = "2", onClick = { onDigitPress("2") })
                    NumpadKey(text = "3", onClick = { onDigitPress("3") })
                }

                // Row 2: 4, 5, 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NumpadKey(text = "4", onClick = { onDigitPress("4") })
                    NumpadKey(text = "5", onClick = { onDigitPress("5") })
                    NumpadKey(text = "6", onClick = { onDigitPress("6") })
                }

                // Row 3: 7, 8, 9
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NumpadKey(text = "7", onClick = { onDigitPress("7") })
                    NumpadKey(text = "8", onClick = { onDigitPress("8") })
                    NumpadKey(text = "9", onClick = { onDigitPress("9") })
                }

                // Row 4: Clear, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Clear Key
                    NumpadSpecialKey(
                        label = "Clear",
                        onClick = { onClear() },
                        testTag = "numpad_clear_key"
                    )

                    // 0 Key
                    NumpadKey(text = "0", onClick = { onDigitPress("0") })

                    // Backspace Key
                    NumpadIconKey(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        onClick = { onBackspace() },
                        testTag = "numpad_backspace_key"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Forgot PIN / Privacy Guarantee button
                TextButton(
                    onClick = { showForgotDialog = true },
                    modifier = Modifier.testTag("forgot_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Security & Reset Info",
                        color = MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = DarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryLightEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "App PIN Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your 4-digit PIN is stored strictly on-device using a cryptographic SHA-256 salted hash. It is never sent to any cloud server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "If you have permanently forgotten your PIN, you can reset app data via Android Settings > Apps > Personal Expense Manager > Clear Storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGold,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showForgotDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun NumpadKey(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .size(72.dp)
            .testTag("numpad_key_$text")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmoothWhite
            )
        }
    }
}

@Composable
private fun NumpadIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .size(72.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SmoothWhite,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NumpadSpecialKey(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(72.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MutedText
            )
        }
    }
}
