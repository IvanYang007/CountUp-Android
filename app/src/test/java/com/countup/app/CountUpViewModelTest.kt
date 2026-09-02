package com.countup.app

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
            viewModel.onEvent(
                CountUpUiEvent.SaveItem(
                    name = "Tea Ceremony",
                    epochDay = fixedToday.toEpochDay(),
                    comment = "Matcha",
                    icon = "spa",
                    cardColor = "willow_sage",
                )
            )

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditorOpen)
            assertEquals(1, state.items.size)
            val item = state.items.first()
            assertEquals("Tea Ceremony", item.name)
            assertEquals("spa", item.icon)
            assertEquals("willow_sage", item.cardColor)
        }
    }

    @Test
    fun `save existing item updates target and closes editor`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday })
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.OpenEditor(target))
        assertEquals(target, viewModel.state.value.editorTarget)

        viewModel.onEvent(
            CountUpUiEvent.SaveItem(
                name = "Water Bonsai Trees",
                epochDay = target.epochDay,
                comment = "Updated note",
                icon = "yard",
                cardColor = "terracotta",
            )
        )

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditorOpen)
            val updated = state.items.first { it.id == target.id }
            assertEquals("Water Bonsai Trees", updated.name)
            assertEquals("Updated note", updated.comment)
            assertEquals("yard", updated.icon)
            assertEquals("terracotta", updated.cardColor)
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

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
            val snackbarEffect = awaitItem()
            assertTrue(snackbarEffect is CountUpUiEffect.ShowSnackbar)
            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.SaveItem("Fail Item", fixedToday.toEpochDay(), ""))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.error_save_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }
    }

    @Test
    fun `ioDispatcher offloads repository mutations asynchronously`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = testDispatcher,
        )

        viewModel.onEvent(
            CountUpUiEvent.SaveItem(
                name = "Async Habit",
                epochDay = fixedToday.toEpochDay(),
                comment = "",
            )
        )

        // Before testDispatcher advances, the async write has NOT committed to state
        assertEquals(0, viewModel.state.value.items.size)

        // Advance dispatcher to execute pending IO coroutines
        testDispatcher.scheduler.runCurrent()

        // Now the write has completed
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Async Habit", viewModel.state.value.items.first().name)
    }

    @Test
    fun `failed update item emits error snackbar effect and keeps previous state`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.OpenEditor(target))
        repo.shouldFailWrite = true

        viewModel.effects.test {
            viewModel.onEvent(
                CountUpUiEvent.SaveItem(
                    name = "Will Fail",
                    epochDay = target.epochDay,
                    comment = "",
                )
            )

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.error_save_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        // Original item remains unchanged
        assertEquals("Water Bonsai", viewModel.state.value.items.first().name)
    }

    @Test
    fun `failed delete item emits error snackbar effect and clears pending delete`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        repo.shouldFailWrite = true
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.RequestDelete(target))
        assertEquals(target, viewModel.state.value.pendingDelete)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmDelete(target.id))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.error_save_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        // pendingDelete is cleared, and item was not deleted
        assertNull(viewModel.state.value.pendingDelete)
        assertEquals(3, viewModel.state.value.items.size)
    }

    @Test
    fun `failed reset item emits error snackbar effect and retains original epoch day`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        repo.shouldFailWrite = true
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()
        val originalEpoch = target.epochDay

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.error_save_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        assertEquals(originalEpoch, viewModel.state.value.items.first { it.id == target.id }.epochDay)
    }

    @Test
    fun `failed toggle widget visibility emits error snackbar effect and retains previous visibility`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        repo.shouldFailWrite = true
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()
        val originalVisibility = target.showInWidget

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ToggleWidgetVisibility(target.id))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.error_save_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        assertEquals(originalVisibility, viewModel.state.value.items.first { it.id == target.id }.showInWidget)
    }

    @Test
    fun `refresh event reloads modified repository state into ui state`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        assertEquals(3, viewModel.state.value.items.size)

        // Mutate repository directly (as would happen via Widget reset or external process)
        repo.addItem("External Widget Item", fixedToday.toEpochDay(), "From Widget")
        repo.setBackgroundTheme(BackgroundTheme.ZEN_BAMBOO)
        repo.setSortOrder(SortOrder.NAME_ASC)

        // Send refresh event
        viewModel.onEvent(CountUpUiEvent.Refresh)

        viewModel.state.test {
            val refreshed = awaitItem()
            assertEquals(4, refreshed.items.size)
            assertTrue(refreshed.items.any { it.name == "External Widget Item" })
            assertEquals(BackgroundTheme.ZEN_BAMBOO, refreshed.backgroundTheme)
            assertEquals(SortOrder.NAME_ASC, refreshed.sortOrder)
        }
    }
}
