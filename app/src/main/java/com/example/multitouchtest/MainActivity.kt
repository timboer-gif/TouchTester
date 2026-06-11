package com.example.multitouchtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.roundToInt

data class TouchPoint(val id: Long, val position: Offset, val color: Color)

data class IconChoice(val label: String, val icon: ImageVector)

data class EditorItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String? = null,
    val icon: ImageVector? = null,
    val color: Color? = null,
    val offset: Offset = Offset(300f, 300f)
)

val TouchColors = listOf(
    Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color(0xFFFFA500)
)

val EditorIcons = listOf(
    IconChoice("Star", Icons.Default.Star),
    IconChoice("Heart", Icons.Default.Favorite),
    IconChoice("Home", Icons.Default.Home),
    IconChoice("Search", Icons.Default.Search),
    IconChoice("Phone", Icons.Default.Phone),
    IconChoice("Email", Icons.Default.Email),
    IconChoice("Face", Icons.Default.Face),
    IconChoice("Tools", Icons.Default.Build),
    IconChoice("Pets", Icons.Default.Pets),
    IconChoice("Camera", Icons.Default.CameraAlt),
    IconChoice("Music", Icons.Default.MusicNote),
    IconChoice("Cart", Icons.Default.ShoppingCart),
    IconChoice("Alarm", Icons.Default.Alarm),
    IconChoice("Place", Icons.Default.Place),
    IconChoice("Send", Icons.AutoMirrored.Filled.Send),
    IconChoice("Like", Icons.Default.ThumbUp),
    IconChoice("Bolt", Icons.Default.Bolt),
    IconChoice("Flight", Icons.Default.Flight),
    IconChoice("Car", Icons.Default.DirectionsCar),
    IconChoice("Settings", Icons.Default.Settings)
)

val IconColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF00ACC1),
    Color(0xFF1E88E5),
    Color(0xFF5E35B1),
    Color(0xFFD81B60)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiTouchApp()
        }
    }
}

@Composable
fun MultiTouchApp() {
    var isDarkTheme by remember { mutableStateOf(true) }
    var isEditMode by remember { mutableStateOf(false) }
    var showIconMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var touchRadius by remember { mutableStateOf(24f) }
    val touchPoints = remember { mutableStateMapOf<Long, TouchPoint>() }
    val touchSlots = remember { mutableStateMapOf<Long, Int>() }
    val editorItems = remember { mutableStateListOf<EditorItem>() }

    val bgColor = if (isDarkTheme) Color.Black else Color.White
    val fgColor = if (isDarkTheme) Color.White else Color.Black
    val panelColor = if (isDarkTheme) Color(0xFF1C1C1F) else Color(0xFFF4ECFF)
    val panelBorderColor = if (isDarkTheme) Color(0xFF3A3A40) else Color(0xFFD8C7FF)
    val textEditBg = if (isDarkTheme) Color(0xFF202024) else Color(0xFFF7F1FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    val id = change.id.value
                                    val slot = touchSlots.getOrPut(id) {
                                        val usedSlots = touchSlots.values.toSet()
                                        TouchColors.indices.firstOrNull { it !in usedSlots }
                                            ?: (touchSlots.size % TouchColors.size)
                                    }
                                    val color = TouchColors[slot % TouchColors.size]
                                    touchPoints[id] = TouchPoint(id, change.position, color)
                                } else {
                                    touchPoints.remove(change.id.value)
                                    touchSlots.remove(change.id.value)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (!isEditMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                touchPoints.values.forEach { point ->
                    drawCircle(
                        color = point.color,
                        radius = touchRadius,
                        center = point.position
                    )
                }
            }
        }

        editorItems.forEach { item ->
            var contentSize by remember(item.id) { mutableStateOf(IntSize.Zero) }
            val density = LocalDensity.current
            val contentWidth = with(density) { contentSize.width.toDp() }
            val contentHeight = with(density) { contentSize.height.toDp() }
            val paletteYOffset = with(density) { contentSize.height.toDp() + 8.dp }
            Box(
                modifier = Modifier
                    .offset { IntOffset(item.offset.x.roundToInt(), item.offset.y.roundToInt()) }
            ) {
                Box(
                    modifier = Modifier.onSizeChanged { contentSize = it }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (item.text != null) {
                            BasicTextField(
                                value = item.text,
                                onValueChange = { updatedText ->
                                    val index = editorItems.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        val old = editorItems[index]
                                        editorItems[index] = old.copy(text = updatedText)
                                    }
                                },
                                readOnly = !isEditMode,
                                textStyle = MaterialTheme.typography.headlineMedium.copy(color = fgColor),
                                modifier = Modifier.widthIn(min = 96.dp, max = 240.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isEditMode) textEditBg else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = if (isEditMode) 1.dp else 0.dp,
                                                color = if (isEditMode) panelBorderColor else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        if (item.text.isEmpty()) {
                                            Text("Tap to edit", color = fgColor.copy(alpha = 0.45f))
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        if (item.icon != null) {
                            Box(
                                modifier = Modifier
                                    .clickable(enabled = isEditMode) { }
                                    .background(
                                        color = if (isEditMode) textEditBg else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = if (isEditMode) 1.dp else 0.dp,
                                        color = if (isEditMode) panelBorderColor else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = null,
                                    tint = item.color ?: fgColor,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }

                    if (isEditMode) {
                        Box(
                            modifier = Modifier.size(
                                width = contentWidth,
                                height = contentHeight
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-36).dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { editorItems.removeAll { it.id == item.id } },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFD73636), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(panelColor, CircleShape)
                                        .border(BorderStroke(1.dp, panelBorderColor), CircleShape)
                                        .pointerInput(item.id) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val index = editorItems.indexOfFirst { it.id == item.id }
                                                if (index != -1) {
                                                    val old = editorItems[index]
                                                    editorItems[index] = old.copy(offset = old.offset + dragAmount)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DragIndicator,
                                        contentDescription = "Move item",
                                        tint = fgColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (item.icon != null) {
                                FlowRow(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = paletteYOffset),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    maxItemsInEachRow = 5
                                ) {
                                    IconColors.forEach { colorChoice ->
                                        val isSelected = item.color == colorChoice
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(colorChoice, CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) fgColor else fgColor.copy(alpha = 0.25f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    val index = editorItems.indexOfFirst { it.id == item.id }
                                                    if (index != -1) {
                                                        val old = editorItems[index]
                                                        editorItems[index] = old.copy(color = colorChoice)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Text("Touch Point Size: ${touchRadius.roundToInt()}")
                        Slider(
                            value = touchRadius,
                            onValueChange = { touchRadius = it },
                            valueRange = 10f..200f
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            FilledTonalIconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isEditMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { editorItems.add(EditorItem(text = "Tap to edit")) }
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Add Text")
                    }
                    Box {
                        FilledTonalButton(
                            onClick = { showIconMenu = true }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Add Icon")
                        }
                        DropdownMenu(
                            expanded = showIconMenu,
                            onDismissRequest = { showIconMenu = false },
                            modifier = Modifier
                                .background(panelColor)
                                .border(BorderStroke(1.dp, panelBorderColor), RoundedCornerShape(18.dp))
                        ) {
                            Text(
                                text = "Choose an icon",
                                style = MaterialTheme.typography.labelLarge,
                                color = fgColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 5
                            ) {
                                EditorIcons.forEach { choice ->
                                    FilledTonalIconButton(
                                        onClick = {
                                            editorItems.add(EditorItem(icon = choice.icon))
                                            showIconMenu = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            choice.icon,
                                            contentDescription = choice.label,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { isDarkTheme = !isDarkTheme },
                    icon = {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.Check,
                            contentDescription = null
                        )
                    },
                    text = { Text(if (isDarkTheme) "Light Mode" else "Dark Mode") }
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        showIconMenu = false
                        isEditMode = !isEditMode
                        if (isEditMode) {
                            touchPoints.clear()
                            touchSlots.clear()
                        }
                    },
                    icon = { Icon(if (isEditMode) Icons.Default.Check else Icons.Default.Edit, contentDescription = null) },
                    text = { Text(if (isEditMode) "Confirm" else "Edit") }
                )
            }
        }
    }
}
