package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    showGrid: Boolean,
    onToggleGrid: (Boolean) -> Unit,
    showFlowArrows: Boolean,
    onToggleFlowArrows: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onExitToMainMenu: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IndustrialSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showHelp) "Керування та Інструкція" else "Налаштування гри",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = SlateDim)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!showHelp) {
                    // Setting 1: Grid Lines
                    SettingToggleRow(
                        title = "Сітка тайлів",
                        subtitle = "Показувати розмітку клітинок на карті",
                        checked = showGrid,
                        onCheckedChange = onToggleGrid
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Setting 2: Flow Arrows
                    SettingToggleRow(
                        title = "Стрілки на конвеєрах",
                        subtitle = "Відображати напрямок руху ресурсів",
                        checked = showFlowArrows,
                        onCheckedChange = onToggleFlowArrows
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Setting 3: Sound Effects
                    SettingToggleRow(
                        title = "Звукові ефекти / Вібро",
                        subtitle = "Тактильний та звуковий відгук",
                        checked = soundEnabled,
                        onCheckedChange = onToggleSound
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Help / Tutorial Button
                    OutlinedButton(
                        onClick = { showHelp = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoreCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("tutorial_guide_btn")
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Як грати (Інструкція)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Exit to Main Menu Button
                    OutlinedButton(
                        onClick = onExitToMainMenu,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("exit_to_menu_btn")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Вийти в Головне Меню")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resume Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CoreCyan, contentColor = IndustrialDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("resume_game_btn")
                    ) {
                        Text("Продовжити гру", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Tutorial Screen
                    TutorialCard(
                        icon = Icons.Default.PanTool,
                        title = "1. Пересування картою",
                        text = "Проведіть пальцем по екрану у будь-якому напрямку для огляду сектору. Використовуйте кнопки + / - зліва для масштабування."
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TutorialCard(
                        icon = Icons.Default.GridView,
                        title = "2. Вибір та кресленики",
                        text = "В правому нижньому кутку оберіть об'єкт. Торкніться клітинки, щоб встановити напівпрозорий кресленик. Повторний дотик по ньому скасовує вибір!"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TutorialCard(
                        icon = Icons.Default.Timeline,
                        title = "3. Лінія конвеєрів та поворот",
                        text = "Оберіть Конвеєр та протягніть пальцем по екрану — гра автоматично прокладе лінію конвеєрів за вашим рухом! Натискайте кнопку повороту, щоб змінити напрямок."
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TutorialCard(
                        icon = Icons.Default.FlightTakeoff,
                        title = "4. Дрон-будівельник",
                        text = "Натисніть зелену кнопку '✓ Побудувати'. Літаючий дрон вилетить зі штабу (Ядра), підлетить до об'єкта та лазером збудує його!"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TutorialCard(
                        icon = Icons.Default.Engineering,
                        title = "5. Видобуток та логістика",
                        text = "Розмістіть Мідний бур на золотистій мідній жилі (2x2). Підведіть конвеєри від бура до Ядра. Видобута мідь автоматично поїде по стрічці на склад Ядра!"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showHelp = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CoreCyan, contentColor = IndustrialDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Назад до налаштувань", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IndustrialCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, color = SlateDim, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = IndustrialDark,
                checkedTrackColor = CoreCyan,
                uncheckedTrackColor = IndustrialBorder
            )
        )
    }
}

@Composable
private fun TutorialCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IndustrialCard)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CoreCyan,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = text, color = SlateDim, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
