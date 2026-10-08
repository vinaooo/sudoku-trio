package io.github.vinaooo.sudokutrio.feature.scores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.sudokutrio.core.ui.modeName
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.vinkit.core.formatElapsed
import io.github.vinaooo.vinkit.designsystem.spokenElapsed
import java.text.DateFormat
import java.util.Date

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(uiState, onBack, modifier, onSelectMode = viewModel::selectMode)
}

/** A tab per mode played (none for a single one), its stats and its top 10. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(
    uiState: ScoresUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectMode: (GameMode) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scores_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.modes.size > 1) {
                item {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = uiState.modes.indexOf(uiState.mode).coerceAtLeast(0),
                        edgePadding = 0.dp,
                    ) {
                        uiState.modes.forEach { mode ->
                            Tab(
                                selected = mode == uiState.mode,
                                onClick = { onSelectMode(mode) },
                                text = { Text(modeName(mode)) },
                            )
                        }
                    }
                }
            } else {
                uiState.mode?.let { item { Text(modeName(it), style = MaterialTheme.typography.titleMedium) } }
            }
            if (uiState.mode != null) item { StatsCard(uiState.stats) }
            if (!uiState.isLoading && uiState.scores.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_scores), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            itemsIndexed(uiState.scores) { index, record -> ScoreRow(index + 1, record) }
        }
    }
}

@Composable
private fun StatsCard(stats: GameStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(stringResource(R.string.stat_played), stats.played.toString())
            StatItem(stringResource(R.string.stat_won), stats.won.toString())
            StatItem(stringResource(R.string.stat_win_rate), "${stats.winRatePercent}%")
            StatItem(stringResource(R.string.stat_streak), stats.currentStreak.toString())
            StatItem(stringResource(R.string.stat_best_streak), stats.bestStreak.toString())
        }
    }
}

/** A value over its label, read by TalkBack as one item: "Won, 3". */
@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $value" },
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The rank, the points, the time, and the mistakes, hints and date below. */
@Composable
private fun ScoreRow(rank: Int, record: ScoreRecord) {
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(record.playedAtMillis))
    val rankDescription = stringResource(R.string.rank_description, rank)
    val spokenTime = spokenElapsed(record.elapsedSeconds)
    // Zero has its own words: Portuguese plural rules would say "0 erro".
    val mistakes = if (record.mistakes == 0) {
        stringResource(R.string.no_mistakes)
    } else {
        pluralStringResource(R.plurals.mistakes, record.mistakes, record.mistakes)
    }
    val hints = if (record.hintsUsed == 0) {
        stringResource(R.string.no_hints)
    } else {
        pluralStringResource(R.plurals.hints, record.hintsUsed, record.hintsUsed)
    }
    ListItem(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        leadingContent = {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = rankDescription },
            )
        },
        supportingContent = { Text(stringResource(R.string.score_details, mistakes, hints, date)) },
        trailingContent = {
            Text(
                formatElapsed(record.elapsedSeconds),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = spokenTime },
            )
        },
    ) { Text(record.points.toString(), fontWeight = FontWeight.Bold) }
}
