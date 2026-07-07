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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
    // Home position (top-left) of the item in root coordinates.
    val offset: Offset = Offset(300f, 300f),
    // Waypoints as displacements from the item's home (icon center). Empty = no path.
    val pathPoints: List<Offset> = emptyList(),
    // Time in ms to travel the full path once (one direction of the ping-pong).
    val animationDurationMs: Float = 2000f,
    // When true and a path exists, the item moves when not in edit mode.
    val isAnimated: Boolean = true
)

/**
 * Returns the point along the polyline [points] at normalized progress [t] (0..1),
 * parameterized by arc length so travel speed is constant regardless of segment lengths.
 */
fun pointAlongPath(points: List<Offset>, t: Float): Offset {
    if (points.isEmpty()) return Offset.Zero
    if (points.size == 1) return points[0]

    val lengths = FloatArray(points.size - 1)
    var total = 0f
    for (i in 0 until points.size - 1) {
        val len = (points[i + 1] - points[i]).getDistance()
        lengths[i] = len
        total += len
    }
    if (total <= 0f) return points[0]

    var target = t.coerceIn(0f, 1f) * total
    for (i in lengths.indices) {
        val len = lengths[i]
        if (target <= len || i == lengths.size - 1) {
            val frac = if (len <= 0f) 0f else (target / len).coerceIn(0f, 1f)
            return points[i] + (points[i + 1] - points[i]) * frac
        }
        target -= len
    }
    return points.last()
}

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
    var selectedItemId by remember { mutableStateOf<String?>(null) }

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

        // Tap empty space (in edit mode) to deselect the active item.
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedItemId = null }
            )
        }

        editorItems.forEach { item ->
            key(item.id) {
                EditorItemView(
                    item = item,
                    isEditMode = isEditMode,
                    isSelected = item.id == selectedItemId,
                    fgColor = fgColor,
                    panelColor = panelColor,
                    panelBorderColor = panelBorderColor,
                    textEditBg = textEditBg,
                    onUpdate = { updated ->
                        val index = editorItems.indexOfFirst { it.id == item.id }
                        if (index != -1) editorItems[index] = updated
                    },
                    onDelete = {
                        editorItems.removeAll { it.id == item.id }
                        if (selectedItemId == item.id) selectedItemId = null
                    },
                    onSelect = { selectedItemId = item.id }
                )
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
            val selectedItem = editorItems.firstOrNull { it.id == selectedItemId }
            if (isEditMode && selectedItem != null) {
                SelectedItemPanel(
                    item = selectedItem,
                    fgColor = fgColor,
                    panelColor = panelColor,
                    panelBorderColor = panelBorderColor,
                    onUpdate = { updated ->
                        val index = editorItems.indexOfFirst { it.id == selectedItem.id }
                        if (index != -1) editorItems[index] = updated
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (isEditMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val newItem = EditorItem(text = "Tap to edit")
                            editorItems.add(newItem)
                            selectedItemId = newItem.id
                        }
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
                                            val newItem = EditorItem(icon = choice.icon)
                                            editorItems.add(newItem)
                                            selectedItemId = newItem.id
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
                        selectedItemId = null
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

@Composable
fun EditorItemView(
    item: EditorItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    fgColor: Color,
    panelColor: Color,
    panelBorderColor: Color,
    textEditBg: Color,
    onUpdate: (EditorItem) -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val density = LocalDensity.current
    // Always read the freshest item inside long-lived pointer callbacks so drags accumulate.
    val latestItem by rememberUpdatedState(item)

    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val halfContent = Offset(contentSize.width / 2f, contentSize.height / 2f)

    val hasPath = item.pathPoints.isNotEmpty()
    val shouldAnimate = !isEditMode && item.isAnimated && hasPath

    // Drive the path traversal with a frame clock. Progress ping-pongs 0->1->0, and the
    // duration is the time for one direction, so a full there-and-back cycle is 2x duration.
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(shouldAnimate, item.animationDurationMs, item.pathPoints.size) {
        if (!shouldAnimate) {
            progress = 0f
            return@LaunchedEffect
        }
        val halfNanos = item.animationDurationMs.coerceAtLeast(1f).toDouble() * 1_000_000.0
        val periodNanos = halfNanos * 2.0
        val startNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val phase = ((now - startNanos).toDouble() % periodNanos) / halfNanos // 0..2
            progress = (if (phase <= 1.0) phase else 2.0 - phase).toFloat()
        }
    }

    val fullPath = remember(item.pathPoints) { listOf(Offset.Zero) + item.pathPoints }
    val displacement = if (shouldAnimate) pointAlongPath(fullPath, progress) else Offset.Zero
    val currentOffset = item.offset + displacement

    // Path preview: faint lines through the icon center and each waypoint (edit mode only),
    // brighter for the selected item.
    if (isEditMode && hasPath) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val home = item.offset + halfContent
            var prev = home
            item.pathPoints.forEach { wp ->
                val p = home + wp
                drawLine(
                    color = (item.color ?: fgColor).copy(alpha = if (isSelected) 0.85f else 0.3f),
                    start = prev,
                    end = p,
                    strokeWidth = 3.dp.toPx()
                )
                prev = p
            }
        }
    }

    // Tap-to-select covers the whole item when it isn't the active one.
    val selectable = isEditMode && !isSelected

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt()) }
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { contentSize = it }
                .then(
                    if (selectable) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect() } else Modifier
                )
        ) {
            val highlight = MaterialTheme.colorScheme.primary
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (item.text != null) {
                    BasicTextField(
                        value = item.text,
                        onValueChange = { updatedText -> onUpdate(latestItem.copy(text = updatedText)) },
                        // Only the selected item is editable, so a tap on an unselected one selects
                        // it instead of stealing focus.
                        readOnly = !isEditMode || !isSelected,
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
                                        width = if (isEditMode) (if (isSelected) 2.dp else 1.dp) else 0.dp,
                                        color = when {
                                            !isEditMode -> Color.Transparent
                                            isSelected -> highlight
                                            else -> panelBorderColor
                                        },
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
                            .background(
                                color = if (isEditMode) textEditBg else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = if (isEditMode) (if (isSelected) 2.dp else 1.dp) else 0.dp,
                                color = when {
                                    !isEditMode -> Color.Transparent
                                    isSelected -> highlight
                                    else -> panelBorderColor
                                },
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

            // Delete + drag-to-move handles float above the item, only when it's selected.
            if (isEditMode && isSelected) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFD73636), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(panelColor, CircleShape)
                            .border(BorderStroke(1.dp, panelBorderColor), CircleShape)
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = { onSelect() }
                                ) { change, dragAmount ->
                                    change.consume()
                                    onUpdate(latestItem.copy(offset = latestItem.offset + dragAmount))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = "Move item",
                            tint = fgColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Draggable, numbered waypoint handles for the selected item, in root coordinates.
    if (isEditMode && isSelected) {
        item.pathPoints.forEachIndexed { index, wp ->
            val handleSizeDp = 34.dp
            val handleHalfPx = with(density) { (handleSizeDp / 2).toPx() }
            val center = item.offset + halfContent + wp
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x - handleHalfPx).roundToInt(),
                            (center.y - handleHalfPx).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .background(item.color ?: panelColor, CircleShape)
                    .border(BorderStroke(2.dp, fgColor), CircleShape)
                    .pointerInput(item.id, index) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val pts = latestItem.pathPoints.toMutableList()
                            if (index < pts.size) {
                                pts[index] = pts[index] + dragAmount
                                onUpdate(latestItem.copy(pathPoints = pts))
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = fgColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Full-width editing controls for the currently selected item: color, path building, and
 * traversal duration. Sits above the add/edit toolbar so it has room to breathe.
 */
@Composable
fun SelectedItemPanel(
    item: EditorItem,
    fgColor: Color,
    panelColor: Color,
    panelBorderColor: Color,
    onUpdate: (EditorItem) -> Unit
) {
    val latestItem by rememberUpdatedState(item)
    val hasPath = item.pathPoints.isNotEmpty()

    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = 12.dp)
            .background(panelColor.copy(alpha = 0.96f), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, panelBorderColor), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (item.icon != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                maxItemsInEachRow = 10
            ) {
                IconColors.forEach { colorChoice ->
                    val chosen = item.color == colorChoice
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(colorChoice, CircleShape)
                            .border(
                                width = if (chosen) 3.dp else 1.dp,
                                color = if (chosen) fgColor else fgColor.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable { onUpdate(latestItem.copy(color = colorChoice)) }
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = {
                    val last = latestItem.pathPoints.lastOrNull() ?: Offset.Zero
                    onUpdate(
                        latestItem.copy(
                            pathPoints = latestItem.pathPoints + (last + Offset(160f, 120f)),
                            isAnimated = true
                        )
                    )
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Point", style = MaterialTheme.typography.labelMedium)
            }

            if (hasPath) {
                FilledTonalButton(
                    onClick = { onUpdate(latestItem.copy(pathPoints = emptyList())) },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = { onUpdate(latestItem.copy(isAnimated = !latestItem.isAnimated)) },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (item.isAnimated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(if (item.isAnimated) "Moving" else "Paused", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (hasPath) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${String.format(java.util.Locale.US, "%.1f", item.animationDurationMs / 1000f)}s",
                    color = fgColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(min = 34.dp)
                )
                Slider(
                    value = item.animationDurationMs,
                    onValueChange = { v -> onUpdate(latestItem.copy(animationDurationMs = v)) },
                    valueRange = 300f..10000f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
