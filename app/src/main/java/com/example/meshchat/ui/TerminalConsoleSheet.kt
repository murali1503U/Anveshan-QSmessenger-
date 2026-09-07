package com.example.meshchat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.meshchat.cli.CliLine
import com.example.meshchat.cli.CliLineType

val TerminalBackground = Color(0xFF090D16)
val TerminalSurface = Color(0xFF111726)
val TerminalGreen = Color(0xFF00E676)
val TerminalCyan = Color(0xFF00E5FF)
val TerminalAmber = Color(0xFFFFD600)
val TerminalRed = Color(0xFFFF5252)
val TerminalPurple = Color(0xFFB388FF)
val TerminalText = Color(0xFFCFD8DC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalConsoleSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cliLines by viewModel.cliLines.collectAsStateWithLifecycle()
    val isCliRunning by viewModel.isCliRunning.collectAsStateWithLifecycle()
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()

    var inputCommand by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom when new output lines arrive
    LaunchedEffect(cliLines.size) {
        if (cliLines.isNotEmpty()) {
            listState.animateScrollToItem(cliLines.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TerminalBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(TerminalBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Terminal Window Title Bar
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(TerminalRed, RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(TerminalAmber, RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(TerminalGreen, RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "mesh-node: bash (termux v2.4)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TerminalCyan
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCliRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = TerminalCyan
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = { viewModel.clearCli() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear Screen",
                                tint = TerminalText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Quick Shortcut Chips for instant AI & Security CLI commands
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val shortcuts = listOf(
                    "qsvm status" to "qsvm status",
                    "qsvm metrics" to "qsvm metrics",
                    "qsvm eval \"top secret token\"" to "qsvm eval",
                    "qsvm bench" to "qsvm bench",
                    "ai eval \"secret pin 4892\"" to "ai eval",
                    "ai eval --local \"status report\"" to "ai eval -l",
                    "ai classify \"GPS 37.77, -122.41\"" to "ai classify",
                    "ai status" to "ai status",
                    "ai benchmark" to "ai bench",
                    "sec auto \"critical token 0x9A\"" to "sec auto",
                    "sec get" to "sec get",
                    "sec set 0" to "sec auto mode",
                    "sec set 4" to "sec PQ mode",
                    "session info" to "session info",
                    "session rotate" to "session rotate",
                    "mesh status" to "mesh status",
                    "help" to "help"
                )

                shortcuts.forEach { (cmd, label) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TerminalSurface,
                        modifier = Modifier.clickable {
                            inputCommand = cmd
                            viewModel.executeCliCommand(cmd)
                            inputCommand = ""
                        }
                    ) {
                        Text(
                            label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Main Console Buffer Output View
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TerminalBackground)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(cliLines) { line ->
                    CliLineItem(line)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Interactive CLI Prompt & Command Input Bar
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$ ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TerminalGreen
                    )

                    OutlinedTextField(
                        value = inputCommand,
                        onValueChange = { inputCommand = it },
                        placeholder = {
                            Text(
                                "ai eval, sec set, bench, help...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputCommand.isNotBlank()) {
                                    val cmd = inputCommand
                                    viewModel.executeCliCommand(cmd)
                                    inputCommand = ""
                                    historyIndex = -1
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = TerminalGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // History Navigation Up
                    IconButton(
                        onClick = {
                            if (commandHistory.isNotEmpty()) {
                                val nextIdx = if (historyIndex == -1) commandHistory.size - 1 else (historyIndex - 1).coerceAtLeast(0)
                                historyIndex = nextIdx
                                inputCommand = commandHistory[nextIdx]
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Previous Command",
                            tint = if (commandHistory.isNotEmpty()) TerminalText else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Execute Command Button
                    IconButton(
                        onClick = {
                            if (inputCommand.isNotBlank()) {
                                val cmd = inputCommand
                                viewModel.executeCliCommand(cmd)
                                inputCommand = ""
                                historyIndex = -1
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute Command",
                            tint = TerminalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CliLineItem(line: CliLine) {
    val (color, fontWeight) = when (line.type) {
        CliLineType.COMMAND -> TerminalCyan to FontWeight.Bold
        CliLineType.OUTPUT -> TerminalText to FontWeight.Normal
        CliLineType.SUCCESS -> TerminalGreen to FontWeight.SemiBold
        CliLineType.WARNING -> TerminalAmber to FontWeight.Normal
        CliLineType.ERROR -> TerminalRed to FontWeight.Bold
        CliLineType.ACCENT -> TerminalPurple to FontWeight.Bold
    }

    Text(
        text = line.text,
        color = color,
        fontWeight = fontWeight,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}
