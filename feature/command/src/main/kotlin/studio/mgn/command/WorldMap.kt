package studio.mgn.command

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.model.DiplomacyState
import kotlin.math.cos
import kotlin.math.sin

/** Relation status driving polygon color. */
enum class RelationStatus { ALLY, WAR, TRADE, NEUTRAL }

fun relationStatusOf(country: DiplomacyState): RelationStatus = when {
    country.atWar -> RelationStatus.WAR
    country.isAlly -> RelationStatus.ALLY
    country.hasTradeDeal -> RelationStatus.TRADE
    else -> RelationStatus.NEUTRAL
}

fun RelationStatus.color(): Color = when (this) {
    RelationStatus.ALLY -> Color(0xFF3DD68C)
    RelationStatus.WAR -> Color(0xFFFF5A4E)
    RelationStatus.TRADE -> Color(0xFF4ED6FF)
    RelationStatus.NEUTRAL -> Color(0xFFB8862B)
}

/**
 * Miniature world map drawn in code: the player at the center, one hexagon
 * per AI country on a ring. Tapping a hex opens the country card.
 */
@Composable
fun WorldMap(
    countries: List<DiplomacyState>,
    playerName: String,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth.value
        val h = maxHeight.value
        val cx = w / 2f
        val cy = h / 2f
        val ring = minOf(w, h) * 0.36f
        val hexR = minOf(w, h) * 0.11f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val toPxX = size.width / w
            val toPxY = size.height / h
            // Player emblem at the center.
            drawCircle(
                color = Color(0xFFF2B33D),
                radius = hexR * toPxX * 0.55f,
                center = Offset(cx * toPxX, cy * toPxY),
            )
            countries.forEachIndexed { i, country ->
                val angle = (i.toDouble() / countries.size.coerceAtLeast(1) * 360.0) - 90.0
                val rad = Math.toRadians(angle)
                val px = (cx + ring * cos(rad).toFloat()) * toPxX
                val py = (cy + ring * sin(rad).toFloat()) * toPxY
                val path = Path().apply {
                    repeat(6) { k ->
                        val a = Math.toRadians((60 * k).toDouble())
                        val vx = px + hexR * toPxX * cos(a).toFloat()
                        val vy = py + hexR * toPxY * sin(a).toFloat()
                        if (k == 0) moveTo(vx, vy) else lineTo(vx, vy)
                    }
                    close()
                }
                val status = relationStatusOf(country)
                drawPath(path, status.color().copy(alpha = 0.85f))
                if (country.countryId == selectedId) {
                    drawPath(path, Color.White.copy(alpha = 0.35f))
                }
            }
        }
        // Invisible tap targets over each hex.
        countries.forEachIndexed { i, country ->
            val angle = (i.toDouble() / countries.size.coerceAtLeast(1) * 360.0) - 90.0
            val rad = Math.toRadians(angle)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (cx + ring * cos(rad).toFloat()).dp - hexR.dp,
                        y = (cy + ring * sin(rad).toFloat()).dp - hexR.dp,
                    )
                    .size(hexR.dp * 2)
                    .semantics {
                        contentDescription = country.nameAr
                        role = Role.Button
                    }
                    .clickable {
                        onSelect(
                            if (selectedId == country.countryId) {
                                null
                            } else {
                                country.countryId
                            },
                        )
                    },
            )
        }
        Text(
            text = playerName,
            style = MgnTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
    }
}

/** Country card shown under the map when a hex is selected. */
@Composable
fun CountryCard(
    country: DiplomacyState,
    relationLabel: String,
    treatiesLabel: String,
    noTreaties: String,
    treatyLabel: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    val status = relationStatusOf(country)
    GoldFramePanel(modifier = modifier) {
        Column {
            Text(
                text = country.nameAr,
                style = MgnTheme.typography.titleMedium,
                color = status.color(),
            )
            Text(
                text = "$relationLabel: ${"%.0f".format(java.util.Locale.US, country.relation)}",
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            val treaties = country.treaties.map { treatyLabel(it.name) }
            Text(
                text = if (treaties.isEmpty()) {
                    "$treatiesLabel: $noTreaties"
                } else {
                    "$treatiesLabel: ${treaties.joinToString("، ")}"
                },
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(name = "WorldMap", widthDp = 300, heightDp = 220)
@Composable
private fun WorldMapPreview() {
    MgnTheme {
        WorldMap(
            countries = previewState().countries.values.toList(),
            playerName = "المجد",
            selectedId = null,
            onSelect = {},
        )
    }
}
