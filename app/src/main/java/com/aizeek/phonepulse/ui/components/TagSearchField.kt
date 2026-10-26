package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.ui.theme.BevelBlue
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelCoral
import com.aizeek.phonepulse.ui.theme.BevelEaseIn
import com.aizeek.phonepulse.ui.theme.BevelEaseOut
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextSecondary
import com.aizeek.phonepulse.ui.theme.BevelTextTertiary

/** Pure suggestion rules for the tag input — unit-tested, no Compose involved. */
object TagSearchEngine {

    /** Suggest app names matching the query, excluding what is already a tag. */
    fun suggestions(
        query: String,
        pool: List<String>,
        taken: Set<String>,
        limit: Int = 6
    ): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return pool.asSequence()
            .filter { it.isNotBlank() && it !in taken && it.contains(q, ignoreCase = true) }
            .distinct()
            .take(limit)
            .toList()
    }

    fun normalizeTag(raw: String): String = raw.trim()
}

/**
 * Tag-capable search field for the app-usage screen.
 *
 * - Tags sit above the input line; tapping X collapses the tag into a dot,
 *   the dot vanishes, siblings slide over (pushed frame-by-frame as the tag
 *   shrinks) and the container height eases down with them.
 * - While typing, a suggestion overlay under the field lists matching app
 *   names; picking one inserts it as a tag and collapses the overlay.
 * - IME "done" commits the typed text as a tag.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    suggestionPool: List<String>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    val suggestions = remember(query, tags, suggestionPool) {
        TagSearchEngine.suggestions(query, suggestionPool, tags.toSet())
    }

    fun addTag(raw: String) {
        val tag = TagSearchEngine.normalizeTag(raw)
        if (tag.isEmpty() || tag in tags) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onTagsChange(tags + tag)
        onQueryChange("")
        menuOpen = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BevelCard)
            .border(1.dp, if (menuOpen) BevelBlue.copy(alpha = 0.65f) else BevelCardBorder, RoundedCornerShape(16.dp))
            .animateContentSize(tween(220, easing = BevelEaseOut))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        AnimatedVisibility(visible = tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    DeletableTagChip(
                        tag = tag,
                        onRemove = { onTagsChange(tags - tag) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = BevelTextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        menuOpen = it.isNotBlank()
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = BevelTextPrimary,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(BevelBlue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (tags.isEmpty() || query.isNotBlank()) addTag(query)
                    }),
                    decorationBox = { inner ->
                        if (query.isEmpty() && tags.isEmpty()) {
                            Text(
                                text = "搜索应用，回车或点联想添加标签",
                                color = BevelTextTertiary,
                                fontSize = 12.sp
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(visible = tags.isNotEmpty()) {
                Text(
                    text = "清除",
                    color = BevelBlue,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTagsChange(emptyList())
                        }
                        .padding(4.dp)
                )
            }
        }

        // Suggestion overlay: anchored under the field, filtered live while typing.
        DropdownMenu(
            expanded = menuOpen && suggestions.isNotEmpty(),
            onDismissRequest = { menuOpen = false },
            containerColor = BevelCard,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            if (query.isNotBlank() &&
                TagSearchEngine.normalizeTag(query) !in tags &&
                suggestions.none { it.equals(query.trim(), ignoreCase = true) }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BevelBlue, modifier = Modifier.size(14.dp))
                            Text("添加标签 “${query.trim()}”", color = BevelTextPrimary, fontSize = 13.sp)
                        }
                    },
                    onClick = { addTag(query) }
                )
            }
            suggestions.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, color = BevelTextPrimary, fontSize = 13.sp, maxLines = 1) },
                    onClick = { addTag(name) }
                )
            }
        }
    }
}

/**
 * A deletable tag with the two-phase exit: the label and X fade while the
 * chip collapses into a small dot, the dot then scales away, and only then is
 * the tag removed from the list — so the siblings slide smoothly into its
 * place instead of jumping on removal.
 */
@Composable
private fun DeletableTagChip(
    tag: String,
    onRemove: () -> Unit
) {
    var phase by remember(tag) { mutableStateOf(TagPhase.IDLE) }
    val vanish = remember(tag) { Animatable(1f) }
    // Enter pop: tags scale in softly so an added tag doesn't blink into place.
    val enter = remember(tag) { Animatable(0.6f) }
    LaunchedEffect(tag) { enter.animateTo(1f, tween(180, easing = BevelEaseOut)) }
    LaunchedEffect(phase) {
        if (phase == TagPhase.VANISH) {
            // One node owns the whole exit so nothing jumps: animateContentSize
            // collapses the chip into the dot, then the dot itself fades away.
            kotlinx.coroutines.delay(150)
            vanish.animateTo(0f, tween(130, easing = BevelEaseIn))
            onRemove()
        }
    }

    Box(
        modifier = Modifier
            .animateContentSize(tween(180, easing = BevelEaseOut))
            .graphicsLayer {
                scaleX = enter.value
                scaleY = enter.value
                alpha = (0.4f + 0.6f * enter.value) * vanish.value
            }
            .clip(RoundedCornerShape(10.dp))
            .background(BevelBlue.copy(alpha = 0.12f))
            .border(1.dp, BevelBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
    ) {
        if (phase == TagPhase.IDLE) {
            Row(
                modifier = Modifier.padding(start = 9.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tag,
                    color = BevelTextPrimary,
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable { phase = TagPhase.VANISH },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除标签 $tag",
                        tint = BevelTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else {
            // Collapse-to-dot: the chip content is replaced by an 8dp dot that
            // animateContentSize shrinks the chip into, then the dot scales away.
            Box(
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BevelBlue)
                )
            }
        }
    }
}

private enum class TagPhase { IDLE, VANISH }
