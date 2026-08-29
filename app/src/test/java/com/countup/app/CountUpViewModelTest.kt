package com.countup.app

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CountUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedToday = LocalDate.of(2026, 8, 29)

    private val sampleItems = listOf(
        CountUpItem(id = "1", name = "Water Bonsai", epochDay = fixedToday.minusDays(5).toEpochDay(), comment = "Morning care"),
        CountUpItem(id = "2", name = "Read Philosophy", epochDay = fixedToday.minusDays(20).toEpochDay(), comment = "Evening read"),
        CountUpItem(id = "3", name = "Meditation", epochDay = fixedToday.minusDays(10).toEpochDay(), comment = "Zen sitting"),
    )

    @Test
    fun `initial state loads items and sort order correctly`() = runTest {
        val repo = FakeCountUpRepository(
            initialItems = sampleItems,
            initialTheme = BackgroundTheme.DREAM_BOAT,
            initialSortOrder = SortOrder.DAYS_DESC,
        )
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(3, state.items.size)
            assertEquals(BackgroundTheme.DREAM_BOAT, state.backgroundTheme)
            assertEquals(SortOrder.DAYS_DESC, state.sortOrder)
            assertEquals("Read Philosophy", state.displayItems.first().name)
        }
    }

    @Test
    fun `search query filters displayItems in real time`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })

        viewModel.state.test {
            val initial = awaitItem()
            assertEquals(3, initial.displayItems.size)

            viewModel.onEvent(CountUpUiEvent.SearchQueryChanged("bon"))
            val filtered = awaitItem()
            assertEquals("bon", filtered.searchQuery)
            assertEquals(1, filtered.displayItems.size)
            assertEquals("Water Bonsai", filtered.displayItems.first().name)

            viewModel.onEvent(CountUpUiEvent.ClearSearch)
            val cleared = awaitItem()
            assertEquals("", cleared.searchQuery)
            assertEquals(3, cleared.displayItems.size)
        }
    }

    @Test
    fun `sort order selection updates state and emits RefreshWidget effect`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems, initialSortOrder = SortOrder.DAYS_DESC)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.SortOrderSelected(SortOrder.NAME_ASC))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
            assertEquals(SortOrder.NAME_ASC, repo.getSortOrder())
        }

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(SortOrder.NAME_ASC, state.sortOrder)
            assertEquals("Meditation", state.displayItems.first().name)
        }
    }

    @Test
    fun `cycle background switches theme, updates repo and emits effects`() = runTest {
        val repo = FakeCountUpRepository(initialTheme = BackgroundTheme.DREAM_BOAT)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.CycleBackground)

            val snackbarEffect = awaitItem()
            assertTrue(snackbarEffect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.bg_switched_toast, (snackbarEffect as CountUpUiEffect.ShowSnackbar).messageRes)
            assertEquals(BackgroundTheme.DREAM_BOAT.next().labelRes, snackbarEffect.formatArgRes)

            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(BackgroundTheme.DREAM_BOAT.next(), state.backgroundTheme)
            assertEquals(BackgroundTheme.DREAM_BOAT.next(), repo.getBackgroundTheme())
        }
    }

    @Test
    fun `save new item adds item and closes editor`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })

        viewModel.onEvent(CountUpUiEvent.OpenEditor(null))
        assertTrue(viewModel.state.value.isEditorOpen)
        assertNull(viewModel.state.value.editorTarget)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.SaveItem(name = "Tea Ceremony", epochDay = fixedToday.toEpochDay(), comment = "Matcha"))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditorOpen)
            assertEquals(1, state.items.size)
            assertEquals("Tea Ceremony", state.items.first().name)
        }
    }

    @Test
    fun `save existing item updates target and closes editor`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.OpenEditor(target))
        assertEquals(target, viewModel.state.value.editorTarget)

        viewModel.onEvent(CountUpUiEvent.SaveItem(name = "Water Bonsai Trees", epochDay = target.epochDay, comment = "Updated note"))

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditorOpen)
            val updated = state.items.first { it.id == target.id }
            assertEquals("Water Bonsai Trees", updated.name)
            assertEquals("Updated note", updated.comment)
        }
    }

    @Test
    fun `delete item workflow removes item and emits effect`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.RequestDelete(target))
        assertEquals(target, viewModel.state.value.pendingDelete)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmDelete(target.id))
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.pendingDelete)
            assertEquals(2, state.items.size)
            assertFalse(state.items.any { it.id == target.id })
        }
    }

    @Test
    fun `reset item updates anchor date to today`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.RequestReset(target))
        assertEquals(target, viewModel.state.value.pendingReset)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.pendingReset)
            val resetItem = state.items.first { it.id == target.id }
            assertEquals(fixedToday.toEpochDay(), resetItem.epochDay)
        }
    }

    @Test
    fun `toggle widget visibility updates item and emits toast effect`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })
        val target = sampleItems.first()

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ToggleWidgetVisibility(target.id))

            val snackbarEffect = awaitItem()
            assertTrue(snackbarEffect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.toast_hidden_from_widget, (snackbarEffect as CountUpUiEffect.ShowSnackbar).messageRes)

            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            val updated = state.items.first { it.id == target.id }
            assertFalse(updated.showInWidget)
        }
    }

    @Test
    fun `failed write emits error snackbar effect`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        repo.shouldFailWrite = true
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.SaveItem("Fail Item", fixedToday.toEpochDay(), ""))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.error_save_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }
    }
}
