package com.aizeek.phonepulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.companion.*
import com.aizeek.phonepulse.ui.theme.BevelAmberDeep
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelCyan
import com.aizeek.phonepulse.ui.theme.BevelLavenderTop
import com.aizeek.phonepulse.ui.theme.BevelMint
import com.aizeek.phonepulse.ui.theme.BevelMintDeep
import com.aizeek.phonepulse.ui.theme.BevelMistBottom
import com.aizeek.phonepulse.ui.theme.BevelPageBottom
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextSecondary
import com.aizeek.phonepulse.ui.theme.BevelViolet

val ForestBackground = BevelPageBottom
val ForestSurface = BevelCard
val ForestBorder = BevelCardBorder
val ForestSage = BevelMintDeep
val ForestCream = BevelTextPrimary
val ForestMuted = BevelTextSecondary
val ForestGold = BevelAmberDeep

@Composable
fun PetScene(state: CompanionState, modifier: Modifier = Modifier, sleepy: Boolean = false,
             charging: Boolean = false, greeting: Boolean = false, interior: Boolean = false) {
    Canvas(modifier.semantics { contentDescription = "${state.name}的森林小窝" }) {
        drawIntoCanvas { ForestArtwork.home(it.nativeCanvas, size.width, size.height, state, interior) }
    }
}

@Composable
fun SceneryArtwork(letter: ForestLetter, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = letter.title }) {
        drawIntoCanvas { ForestArtwork.scenery(it.nativeCanvas, size.width, size.height, letter.placeId, letter.artwork) }
    }
}

@Composable
fun FriendArtwork(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = ForestWorld.friend(id)?.name.orEmpty() }) {
        drawIntoCanvas {
            val canvas = it.nativeCanvas
            canvas.save(); canvas.translate((size.width - minOf(size.width, size.height)) / 2, 0f)
            ForestArtwork.friend(canvas, id, scale = minOf(size.width, size.height) / 80f); canvas.restore()
        }
    }
}

@Composable
fun ItemArtwork(id: String, modifier: Modifier = Modifier, locked: Boolean = false) {
    Canvas(modifier) {
        val edge = minOf(size.width, size.height)
        drawIntoCanvas {
            val canvas = it.nativeCanvas
            canvas.save(); canvas.translate((size.width - edge) / 2, (size.height - edge) / 2)
            CompanionArtwork.item(canvas, id, edge, locked); canvas.restore()
        }
    }
}

@Composable
fun CompanionHomeEntry(ui: CompanionUiState, onClick: () -> Unit, modifier: Modifier = Modifier,
                       serviceRunning: Boolean = true, onAction: (CompanionPrompt) -> Unit = { onClick() }) {
    val prompt = CompanionPrompts.home(ui.data, serviceRunning, System.currentTimeMillis())
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(BevelLavenderTop, BevelMistBottom)))
        .border(1.dp, BevelCardBorder, RoundedCornerShape(24.dp))
        .clickable(enabled = ui.loaded) { onAction(prompt) }.testTag("companion_entry").padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PetScene(ui.data, Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (ui.loaded) prompt.title else "${ui.data.name}的森林小窝", color = BevelTextPrimary,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (ui.loaded) prompt.message else "正在看看森林里的新消息…", color = BevelTextSecondary, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(prompt.badge, color = BevelViolet, fontSize = 11.5.sp,
                modifier = Modifier.background(BevelViolet.copy(alpha = 0.12f), RoundedCornerShape(10.dp)).padding(9.dp, 5.dp))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onAction(prompt) }, enabled = ui.loaded,
                colors = ButtonDefaults.textButtonColors(contentColor = BevelMint)) {
                Text(prompt.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = BevelCyan)
            }
        }
    }
}
