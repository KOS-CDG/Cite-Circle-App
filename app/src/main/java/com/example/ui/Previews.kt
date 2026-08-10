package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.SampleData
import com.example.data.SavedPaper
import com.example.ui.components.CitationBlock
import com.example.ui.components.CitationChart
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.LoadingCardPlaceholder
import com.example.ui.components.PaperCard
import com.example.ui.components.ScreenHeader
import com.example.ui.lists.ReadingListCard
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.opportunities.OpportunityCard
import com.example.ui.theme.InkAndFieldNotesTheme

/**
 * Previews for the pieces that do not need a Room-backed view model.
 *
 * Every preview is declared in both themes on purpose: the palette is hand-rolled and
 * dynamic colour is disabled, so a dark-mode contrast problem will not surface any other way.
 */

private val previewPaper = SavedPaper(
    id = "preview",
    authorInitials = "JD",
    authorName = "Dr. Jane Doe",
    timeAgo = "2h ago",
    affiliation = "AFFILIATION: OXFORD",
    content = "I just published a new preprint analyzing the semantic structures of large " +
        "language models. The findings suggest a stark shift in latent knowledge " +
        "representations.",
    citation = "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. " +
        "https://cite.circle/refs/882xj",
    isEndorsed = false,
)

@Composable
private fun PreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    InkAndFieldNotesTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(CiteCircleDefaults.ScreenPadding),
            ) {
                content()
            }
        }
    }
}

@Preview(name = "Paper card · light", showBackground = true, widthDp = 400)
@Composable
private fun PaperCardLightPreview() = PreviewSurface(darkTheme = false) {
    PaperCard(paper = previewPaper, onEndorse = {}, onViewContext = {})
}

@Preview(name = "Paper card · dark", showBackground = true, widthDp = 400)
@Composable
private fun PaperCardDarkPreview() = PreviewSurface(darkTheme = true) {
    PaperCard(paper = previewPaper.copy(isEndorsed = true), onEndorse = {}, onViewContext = {})
}

@Preview(name = "Citation block · light", showBackground = true, widthDp = 400)
@Composable
private fun CitationBlockLightPreview() = PreviewSurface(darkTheme = false) {
    CitationBlock(previewPaper.citation)
}

@Preview(name = "Citation block · dark", showBackground = true, widthDp = 400)
@Composable
private fun CitationBlockDarkPreview() = PreviewSurface(darkTheme = true) {
    CitationBlock(previewPaper.citation)
}

@Preview(name = "Citation chart · light", showBackground = true, widthDp = 400)
@Composable
private fun CitationChartLightPreview() = PreviewSurface(darkTheme = false) {
    CitationChart()
}

@Preview(name = "Citation chart · dark", showBackground = true, widthDp = 400)
@Composable
private fun CitationChartDarkPreview() = PreviewSurface(darkTheme = true) {
    CitationChart()
}

@Preview(name = "Empty state · light", showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun EmptyStateLightPreview() = PreviewSurface(darkTheme = false) {
    EmptyState(
        title = "No Papers Yet",
        message = "Publish a preprint or endorse a colleague's work to start building your " +
            "registry.",
        icon = Icons.Outlined.BookmarkBorder,
        actionLabel = "PUBLISH A PAPER",
        onAction = {},
    )
}

@Preview(name = "Empty state · dark", showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun EmptyStateDarkPreview() = PreviewSurface(darkTheme = true) {
    EmptyState(
        title = "No Papers Yet",
        message = "Publish a preprint or endorse a colleague's work to start building your " +
            "registry.",
        icon = Icons.Outlined.BookmarkBorder,
        actionLabel = "PUBLISH A PAPER",
        onAction = {},
    )
}

@Preview(name = "Error state · light", showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun ErrorStateLightPreview() = PreviewSurface(darkTheme = false) {
    ErrorState(
        title = "Paper Not Found",
        message = "This paper is no longer in your registry.",
        actionLabel = "GO BACK",
        onAction = {},
    )
}

@Preview(name = "Error state · dark", showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun ErrorStateDarkPreview() = PreviewSurface(darkTheme = true) {
    ErrorState(
        title = "Paper Not Found",
        message = "This paper is no longer in your registry.",
        actionLabel = "GO BACK",
        onAction = {},
    )
}

@Preview(name = "Loading skeleton · light", showBackground = true, widthDp = 400)
@Composable
private fun LoadingLightPreview() = PreviewSurface(darkTheme = false) {
    LoadingCardPlaceholder()
}

@Preview(name = "Loading skeleton · dark", showBackground = true, widthDp = 400)
@Composable
private fun LoadingDarkPreview() = PreviewSurface(darkTheme = true) {
    LoadingCardPlaceholder()
}

@Preview(name = "Screen header · light", showBackground = true, widthDp = 400)
@Composable
private fun ScreenHeaderLightPreview() = PreviewSurface(darkTheme = false) {
    ScreenHeader(title = "PAPER", onBack = {})
}

@Preview(name = "Screen header · dark", showBackground = true, widthDp = 400)
@Composable
private fun ScreenHeaderDarkPreview() = PreviewSurface(darkTheme = true) {
    ScreenHeader(title = "PAPER", onBack = {})
}

@Preview(name = "Reading list card · light", showBackground = true, widthDp = 400)
@Composable
private fun ReadingListCardLightPreview() = PreviewSurface(darkTheme = false) {
    ReadingListCard(folder = SampleData.readingLists.first(), onClick = {})
}

@Preview(name = "Reading list card · dark", showBackground = true, widthDp = 400)
@Composable
private fun ReadingListCardDarkPreview() = PreviewSurface(darkTheme = true) {
    ReadingListCard(folder = SampleData.readingLists.first(), onClick = {})
}

@Preview(name = "Opportunity card · light", showBackground = true, widthDp = 400)
@Composable
private fun OpportunityCardLightPreview() = PreviewSurface(darkTheme = false) {
    OpportunityCard(opportunity = SampleData.opportunities.first(), onViewDetails = {})
}

@Preview(name = "Opportunity card · dark", showBackground = true, widthDp = 400)
@Composable
private fun OpportunityCardDarkPreview() = PreviewSurface(darkTheme = true) {
    OpportunityCard(opportunity = SampleData.opportunities.first(), onViewDetails = {})
}

@Preview(name = "Onboarding · light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun OnboardingLightPreview() {
    InkAndFieldNotesTheme(darkTheme = false) { OnboardingScreen(onContinue = {}) }
}

@Preview(name = "Onboarding · dark", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun OnboardingDarkPreview() {
    InkAndFieldNotesTheme(darkTheme = true) { OnboardingScreen(onContinue = {}) }
}
