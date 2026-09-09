package com.countup.app

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CountUpStressAndBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedToday = LocalDate.of(2026, 8, 29)

    @Test
    fun largeCollectionFilteringAndSortingExecutesDeterministically() = runTest {
        val largeCount = 1000
        val items = (0 until largeCount).map { i ->
            CountUpItem(
                id = "item-$i",
                name = if (i % 50 == 0) "Target Habit $i" else "Daily Routine $i",
                epochDay = fixedToday.minusDays((i % 365).toLong()).toEpochDay(),
                comment = if (i % 2 == 0) "Notes for $i" else "",
            )
        }

        val repo = FakeCountUpRepository(initialItems = items)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        assertEquals(largeCount, viewModel.state.value.items.size)
        assertEquals(largeCount, viewModel.state.value.displayItems.size)

        // Filter for "Target"
        viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("Target"))
        val filtered = viewModel.state.value.displayItems
        assertEquals(20, filtered.size)
        assertTrue(filtered.all { it.name.contains("Target") })

        // Sort by NAME_ASC
        viewModel.onEvent(CountUpUiEvent.SortOrderSelected(SortOrder.NAME_ASC))
        val sorted = viewModel.state.value.displayItems
        assertEquals(20, sorted.size)
        assertEquals("Target Habit 0", sorted.first().name)
    }

    @Test
    fun emojiSpecialCharactersAndUnicodePreservedThroughWorkflow() = runTest {
        val specialName = "🧘‍♂️ Zen Meditation 🎋 {peace} \\ \"quotes\""
        val specialComment = "Line 1 🍵\nLine 2 🏯"

        val repo = FakeCountUpRepository()
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.onEvent(CountUpUiEvent.SaveItem(name = specialName, epochDay = fixedToday.toEpochDay(), comment = specialComment))

        val saved = viewModel.state.value.items.first()
        assertEquals(specialName, saved.name)
        assertEquals(specialComment, saved.comment)

        // Substring search with emoji or special symbol
        viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("🎋"))
        assertEquals(1, viewModel.state.value.displayItems.size)

        viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("{peace}"))
        assertEquals(1, viewModel.state.value.displayItems.size)

        viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("🍵"))
        assertEquals(1, viewModel.state.value.displayItems.size)
    }

    @Test
    fun leapYearAndHistoricalEpochBoundariesCalculatedCorrectly() = runTest {
        val leapDay2024 = LocalDate.of(2024, 2, 29).toEpochDay()
        val moonLanding1969 = LocalDate.of(1969, 7, 20).toEpochDay() // Negative epoch day
        val future2040 = LocalDate.of(2040, 1, 1).toEpochDay()

        val items = listOf(
            CountUpItem(id = "leap", name = "Leap Day 2024", epochDay = leapDay2024),
            CountUpItem(id = "moon", name = "Apollo 11", epochDay = moonLanding1969),
            CountUpItem(id = "future", name = "Future Vision", epochDay = future2040, futureFlag = true),
        )

        val repo = FakeCountUpRepository(initialItems = items)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        val state = viewModel.state.value
        assertEquals(3, state.items.size)

        // Moon landing has the most days elapsed
        viewModel.onEvent(CountUpUiEvent.SortOrderSelected(SortOrder.DAYS_DESC))
        assertEquals("Apollo 11", viewModel.state.value.displayItems.first().name)

        // Future vision has the most recent anchor date
        viewModel.onEvent(CountUpUiEvent.SortOrderSelected(SortOrder.DATE_DESC))
        assertEquals("Future Vision", viewModel.state.value.displayItems.first().name)
    }

    @Test
    fun rapidSequentialEventsMaintainSynchronizedStateTimeline() = runTest {
        val repo = FakeCountUpRepository()
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.state.test {
            val s0 = awaitItem()
            assertTrue(s0.items.isEmpty())

            // Sequential rapid user events
            viewModel.onEvent(CountUpUiEvent.SaveItem(name = "Meditation Morning", epochDay = fixedToday.minusDays(10).toEpochDay()))
            val s1Loading = awaitItem()
            assertTrue(s1Loading.isSaving)
            val s1 = awaitItem()
            assertFalse(s1.isSaving)
            assertEquals(1, s1.items.size)

            viewModel.onEvent(CountUpUiEvent.SaveItem(name = "Bonsai Care", epochDay = fixedToday.minusDays(20).toEpochDay()))
            val s2Loading = awaitItem()
            assertTrue(s2Loading.isSaving)
            val s2 = awaitItem()
            assertFalse(s2.isSaving)
            assertEquals(2, s2.items.size)

            viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("Morning"))
            val s3 = awaitItem()
            assertEquals(1, s3.displayItems.size)
            assertEquals("Meditation Morning", s3.displayItems.first().name)

            viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("Bonsai"))
            val s4 = awaitItem()
            assertEquals(1, s4.displayItems.size)
            assertEquals("Bonsai Care", s4.displayItems.first().name)

            viewModel.onEvent(CountUpUiEvent.CycleBackground)
            val s5 = awaitItem()
            assertEquals(BackgroundTheme.AUTO_DAILY.next(), s5.backgroundTheme)

            viewModel.onEvent(CountUpUiEvent.ClearSearch)
            val s6 = awaitItem()
            assertEquals(2, s6.displayItems.size)
        }
    }
}
