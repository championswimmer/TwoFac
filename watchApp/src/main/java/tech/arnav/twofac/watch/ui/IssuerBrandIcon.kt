package tech.arnav.twofac.watch.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconMatch
import tech.arnav.twofac.watch.R

private val issuerBrandFontFamily = FontFamily(
    Font(R.font.fa_brands_400_regular, weight = FontWeight.Normal),
)

@Composable
fun IssuerBrandIcon(
    issuer: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color,
    contentDescription: String? = null,
) {
    IssuerBrandIcon(
        match = IssuerIconCatalog.resolveIssuerIcon(issuer),
        modifier = modifier,
        size = size,
        tint = tint,
        contentDescription = contentDescription,
    )
}

@Composable
fun IssuerBrandIcon(
    match: IssuerIconMatch,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color,
    contentDescription: String? = null,
) {
    val accessibleModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val fontSize = with(LocalDensity.current) { (size * 0.8f).toSp() }

    if (match.isPlaceholder) {
        PlaceholderIssuerIcon(
            modifier = modifier.then(accessibleModifier),
            size = size,
            tint = tint,
        )
        return
    }

    val glyph = IssuerIconCatalog.glyphForIconKey(match.iconKey) ?: "?"
    Text(
        text = glyph,
        modifier = modifier
            .size(size)
            .then(accessibleModifier),
        color = tint,
        fontFamily = issuerBrandFontFamily,
        fontSize = fontSize,
        fontWeight = FontWeight.Normal,
    )
}

@Composable
private fun PlaceholderIssuerIcon(
    modifier: Modifier,
    size: Dp,
    tint: Color,
) {
    val fontSize = with(LocalDensity.current) { (size * 0.55f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .border(width = 1.dp, color = tint.copy(alpha = 0.6f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            color = tint,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}
