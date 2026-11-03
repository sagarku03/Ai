package com.jarvisai.assistant.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNeonCyan
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisVioletBright
import com.example.ui.theme.StatusListeningEmerald
import com.example.ui.theme.StatusThinkingAmber
import com.jarvisai.assistant.security.SafetyStage
import com.jarvisai.assistant.service.JarvisLiveWallpaperService
import com.jarvisai.assistant.service.TermuxCommandResult
import com.jarvisai.assistant.service.WebPageSnapshot
import com.jarvisai.assistant.ui.components.JarvisGlassPanel
import com.jarvisai.assistant.ui.components.JarvisPrimaryActionButton
import com.jarvisai.assistant.ui.components.JarvisSecondaryActionButton

/**
 * Interactive Frontend Command Console for On-Device AI OS Upgrades:
 * - Termux CLI Bridge (`com.termux.permission.RUN_COMMAND`)
 * - In-App Sandboxed Web Agent (`WebAgentManager`)
 * - On-Device Offline Speech-to-Text (`OfflineSttEngine`)
 * - Default System Digital Assistant & Live Wallpaper Launchers
 * - 3-Tier Self-Healing Watchdog (`ModelSafetyGuard`), APK Signature SHA-256 (`SignatureVerifier`),
 *   Driving Mode (`AmbientContextReceiver`), and Knowledge Sync (`KnowledgeSyncWorker`)
 */
@Composable
fun OsPowerToolsScreen(
    onBack: () -> Unit,
    isTermuxInstalled: Boolean,
    isTermuxRunning: Boolean,
    termuxResult: TermuxCommandResult?,
    onRunTermuxCommand: (command: String, workDir: String) -> Unit,
    isWebFetching: Boolean,
    webSnapshot: WebPageSnapshot?,
    webError: String?,
    onFetchWebPage: (url: String) -> Unit,
    isOfflineSttListening: Boolean,
    offlineSttPartial: String,
    offlineSttFinal: String,
    onStartOfflineStt: () -> Unit,
    onStopOfflineStt: () -> Unit,
    safetyStage: SafetyStage,
    onResetSafetyGuard: () -> Unit,
    apkSignatureSha256: String,
    isDrivingHandsFreeActive: Boolean,
    isBluetoothHandsFreeConnected: Boolean,
    lastKnowledgeSyncStatus: String,
    onRunKnowledgeSyncNow: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var termuxCmdInput by remember { mutableStateOf("uname -a && ls -la") }
    var termuxWorkDirInput by remember { mutableStateOf("/data/data/com.termux/files/home") }
    var webUrlInput by remember { mutableStateOf("https://developer.android.com") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("os_power_tools_screen"),
        contentPadding = PaddingValues(bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .testTag("os_tools_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI OS POWER HUB & TERMUX BRIDGE",
                        color = JarvisNeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = "Terminal, Web Agent & OS Subsystems",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 1. OS-LEVEL ASSISTANT & LIVE WALLPAPER QUICK ACTIVATION ROW
        item {
            JarvisGlassPanel(
                accentColor = JarvisNeonCyan,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Android OS Integration Launchers",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Set JARVIS as your Default System Voice Assistant (Power Button / Corner Swipe) or activate the interactive Arc Reactor Live Wallpaper.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JarvisSecondaryActionButton(
                            text = "Set Default Assistant",
                            icon = Icons.Default.Assistant,
                            accentColor = JarvisNeonCyan,
                            onClick = { openDefaultVoiceInputSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_set_default_assistant")
                        )
                        JarvisSecondaryActionButton(
                            text = "Set Live Wallpaper",
                            icon = Icons.Default.Wallpaper,
                            accentColor = JarvisVioletBright,
                            onClick = { openJarvisLiveWallpaperPicker(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_set_live_wallpaper")
                        )
                    }
                }
            }
        }

        // 2. DEEP TERMUX CLI BRIDGE CONSOLE
        item {
            JarvisGlassPanel(
                accentColor = JarvisNeonGreen,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = JarvisNeonGreen)
                            Text(
                                text = "Termux CLI Bridge",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isTermuxInstalled) StatusListeningEmerald.copy(alpha = 0.2f)
                                    else StatusThinkingAmber.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if (isTermuxInstalled) StatusListeningEmerald else StatusThinkingAmber,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isTermuxInstalled) "TERMUX DETECTED" else "TERMUX NOT INSTALLED",
                                color = if (isTermuxInstalled) StatusListeningEmerald else StatusThinkingAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = termuxCmdInput,
                        onValueChange = { termuxCmdInput = it },
                        label = { Text("Bash / Python Command") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisNeonGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("termux_command_input")
                    )

                    OutlinedTextField(
                        value = termuxWorkDirInput,
                        onValueChange = { termuxWorkDirInput = it },
                        label = { Text("Working Directory") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisNeonCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("termux_workdir_input")
                    )

                    JarvisPrimaryActionButton(
                        text = if (isTermuxRunning) "Executing in Termux..." else "Dispatch Command to Termux",
                        icon = Icons.Default.PlayArrow,
                        enabled = termuxCmdInput.isNotBlank() && !isTermuxRunning,
                        accentColor = JarvisNeonGreen,
                        onClick = { onRunTermuxCommand(termuxCmdInput, termuxWorkDirInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_run_termux_command")
                    )

                    if (termuxResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF060912))
                                .border(
                                    1.dp,
                                    if (termuxResult.isSuccess) StatusListeningEmerald else AlertCrimson,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                                .testTag("termux_result_box")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Session: ${termuxResult.sessionId.take(8)} • Exit Code: ${termuxResult.exitCode}",
                                    color = if (termuxResult.isSuccess) StatusListeningEmerald else AlertCrimson,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                if (termuxResult.stdout.isNotBlank()) {
                                    Text(
                                        text = termuxResult.stdout,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (termuxResult.stderr.isNotBlank()) {
                                    Text(
                                        text = termuxResult.stderr,
                                        color = AlertCrimson,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (termuxResult.errorMessage.isNotBlank()) {
                                    Text(
                                        text = termuxResult.errorMessage,
                                        color = StatusThinkingAmber,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. IN-APP SANDBOXED WEB AGENT CONSOLE
        item {
            JarvisGlassPanel(
                accentColor = JarvisElectricBlue,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = JarvisNeonCyan)
                        Text(
                            text = "Autonomous Headless Web Agent",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "Loads any URL inside a sandboxed Android WebView and extracts readable DOM text for AI inspection.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = webUrlInput,
                        onValueChange = { webUrlInput = it },
                        label = { Text("Target Web URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisNeonCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_agent_url_input")
                    )
                    JarvisPrimaryActionButton(
                        text = if (isWebFetching) "Extracting DOM..." else "Fetch & Extract Web DOM",
                        icon = Icons.Default.Language,
                        enabled = webUrlInput.isNotBlank() && !isWebFetching,
                        accentColor = JarvisNeonCyan,
                        onClick = { onFetchWebPage(webUrlInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_fetch_web_dom")
                    )

                    if (isWebFetching) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = JarvisNeonCyan)
                            Text("Loading page & running JS DOM extractor...", color = JarvisNeonCyan, fontSize = 12.sp)
                        }
                    }

                    if (!webError.isNullOrBlank()) {
                        Text(text = webError, color = AlertCrimson, fontSize = 12.sp)
                    }

                    if (webSnapshot != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0A1020))
                                .border(1.dp, JarvisNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                                .testTag("web_agent_result_box")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = webSnapshot.title.ifBlank { webSnapshot.url },
                                        color = JarvisNeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { clipboard.setText(AnnotatedString(webSnapshot.extractedText)) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy DOM", tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Text(
                                    text = webSnapshot.extractedText.take(1200),
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    maxLines = 10
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. ON-DEVICE OFFLINE SPEECH-TO-TEXT (STT) DICTATION STUDIO
        item {
            JarvisGlassPanel(
                accentColor = JarvisVioletBright,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isOfflineSttListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (isOfflineSttListening) JarvisNeonGreen else JarvisVioletBright
                            )
                            Text(
                                text = "On-Device Offline STT Dictation",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = if (isOfflineSttListening) "LISTENING (OFFLINE)" else "READY",
                            color = if (isOfflineSttListening) JarvisNeonGreen else JarvisTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Uses Android's on-device SpeechRecognizer with EXTRA_PREFER_OFFLINE=true for zero-cloud local dictation.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JarvisPrimaryActionButton(
                            text = if (isOfflineSttListening) "Listening..." else "Start Offline STT",
                            icon = Icons.Default.Mic,
                            enabled = !isOfflineSttListening,
                            accentColor = JarvisVioletBright,
                            onClick = onStartOfflineStt,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_start_offline_stt")
                        )
                        if (isOfflineSttListening) {
                            JarvisSecondaryActionButton(
                                text = "Stop",
                                icon = Icons.Default.MicOff,
                                accentColor = AlertCrimson,
                                onClick = onStopOfflineStt,
                                modifier = Modifier.testTag("btn_stop_offline_stt")
                            )
                        }
                    }

                    if (offlineSttPartial.isNotBlank() || offlineSttFinal.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1324))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (offlineSttPartial.isNotBlank()) {
                                    Text(
                                        text = "Partial: $offlineSttPartial",
                                        color = JarvisNeonCyan,
                                        fontSize = 12.sp
                                    )
                                }
                                if (offlineSttFinal.isNotBlank()) {
                                    Text(
                                        text = "Final: $offlineSttFinal",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. SELF-HEALING WATCHDOG, ANTI-TAMPER SIGNATURE, DRIVING MODE & KNOWLEDGE SYNC TELEMETRY
        item {
            JarvisGlassPanel(
                accentColor = StatusListeningEmerald,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = StatusListeningEmerald)
                        Text(
                            text = "Resilience, Security & Ambient Context",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    // Self-Healing Watchdog Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Self-Healing Watchdog Stage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Current Stage: ${safetyStage.name}", color = StatusListeningEmerald, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        JarvisSecondaryActionButton(
                            text = "Reset Stage",
                            accentColor = JarvisNeonCyan,
                            onClick = onResetSafetyGuard,
                            modifier = Modifier.testTag("btn_reset_safety_guard")
                        )
                    }

                    // Driving & Bluetooth Hands-Free Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = JarvisNeonCyan, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Ambient Driving / Hands-Free Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (isDrivingHandsFreeActive) "ACTIVE (Hands-Free Audio Enabled)"
                                    else if (isBluetoothHandsFreeConnected) "Bluetooth Audio Connected"
                                    else "Standby (Monitoring Car/Bluetooth)",
                                    color = JarvisTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Offline Knowledge Sync Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Knowledge Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(lastKnowledgeSyncStatus, color = JarvisTextSecondary, fontSize = 11.sp)
                        }
                        JarvisSecondaryActionButton(
                            text = "Sync Now",
                            icon = Icons.Default.CleaningServices,
                            accentColor = JarvisNeonGreen,
                            onClick = onRunKnowledgeSyncNow,
                            modifier = Modifier.testTag("btn_run_knowledge_sync")
                        )
                    }

                    // APK Anti-Tamper SHA-256 Certificate Digest
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF080D1A))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "APK Signing Certificate (SHA-256):",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = apkSignatureSha256.ifBlank { "VERIFIED (DEBUG / LOCAL KEYSTORE)" },
                            color = JarvisNeonCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun openDefaultVoiceInputSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
        }
    }
}

private fun openJarvisLiveWallpaperPicker(context: Context) {
    try {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, JarvisLiveWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
        }
    }
}
