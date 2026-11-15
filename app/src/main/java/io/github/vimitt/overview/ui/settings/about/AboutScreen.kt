package io.github.vimitt.overview.ui.settings.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.vimitt.overview.BuildConfig
import io.github.vimitt.overview.R
import io.github.vimitt.overview.ui.settings.components.NavigationRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsRow
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.theme.OverviewTheme

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val githubUrl = stringResource(R.string.settings_about_github_url)
    val versionLabel = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_about),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "about_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(shapes.large)
                        .background(colors.accent.copy(alpha = 0.16f))
                        .border(1.5.dp, colors.accent.copy(alpha = 0.6f), shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_grid),
                        contentDescription = stringResource(R.string.app_name),
                        tint = colors.accent,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        item(key = "about_app_group") {
            SettingsGroup(title = stringResource(R.string.settings_about_section_app)) {
                SettingsRow(
                    title = stringResource(R.string.settings_about_version),
                    subtitle = versionLabel,
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_about_license),
                    subtitle = stringResource(R.string.settings_about_license_value),
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_about_source),
                    subtitle = stringResource(R.string.settings_about_source_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, githubUrl.toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
        }

        item(key = "about_credits_group") {
            SettingsGroup(title = stringResource(R.string.settings_about_section_credits)) {
                SettingsRow(
                    title = stringResource(R.string.settings_about_credit_gnome),
                    subtitle = stringResource(R.string.settings_about_credit_gnome_sub),
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_about_credit_lawnchair),
                    subtitle = stringResource(R.string.settings_about_credit_lawnchair_sub),
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
