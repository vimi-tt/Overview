package io.github.vimitt.overview.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Ícone oficial unificado do Overview reutilizado na abertura, boas-vindas e tela Sobre,
 * correspondendo ao mesmo desenho adaptativo exibido na bandeja de aplicativos.
 */
@Composable
fun OverviewAppIcon(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(size * 0.28f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.dp, colors.hairlineBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Compensa a margem interna padrão de ícones adaptativos (72/108)
                    scaleX = 1.24f
                    scaleY = 1.24f
                },
        )
    }
}
