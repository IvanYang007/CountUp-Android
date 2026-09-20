package com.countup.app

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

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
    fun `theme mode selection updates state, repository and emits snackbar and refresh widget effects`() = runTest {
        val repo = FakeCountUpRepository(initialThemeMode = ThemeMode.SYSTEM)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ThemeModeSelected(ThemeMode.DARK))

            val snackbarEffect = awaitItem()
            assertTrue(snackbarEffect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.theme_switched_toast, (snackbarEffect as CountUpUiEffect.ShowSnackbar).messageRes)
            assertEquals(ThemeMode.DARK.labelRes, snackbarEffect.formatArgRes)

            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertEquals(ThemeMode.DARK, repo.getThemeMode())
        }
    }

    @Test
    fun `cycle theme mode cycles through SYSTEM, LIGHT, and DARK sequentially`() = runTest {
        val repo = FakeCountUpRepository(initialThemeMode = ThemeMode.SYSTEM)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())

        viewModel.effects.test {
            // 1st tap: SYSTEM -> LIGHT
            viewModel.onEvent(CountUpUiEvent.CycleThemeMode)
            val effect1 = awaitItem() as CountUpUiEffect.ShowSnackbar
            assertEquals(ThemeMode.LIGHT.labelRes, effect1.formatArgRes)
            assertTrue(awaitItem() is CountUpUiEffect.RefreshWidget)
            assertEquals(ThemeMode.LIGHT, viewModel.state.value.themeMode)

            // 2nd tap: LIGHT -> DARK
            viewModel.onEvent(CountUpUiEvent.CycleThemeMode)
            val effect2 = awaitItem() as CountUpUiEffect.ShowSnackbar
            assertEquals(ThemeMode.DARK.labelRes, effect2.formatArgRes)
            assertTrue(awaitItem() is CountUpUiEffect.RefreshWidget)
            assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)

            // 3rd tap: DARK -> SYSTEM
            viewModel.onEvent(CountUpUiEvent.CycleThemeMode)
            val effect3 = awaitItem() as CountUpUiEffect.ShowSnackbar
            assertEquals(ThemeMode.SYSTEM.labelRes, effect3.formatArgRes)
            assertTrue(awaitItem() is CountUpUiEffect.RefreshWidget)
            assertEquals(ThemeMode.SYSTEM, viewModel.state.value.themeMode)
        }
    }

    @Test
    fun `set settings dialog visibility updates state correctly`() = runTest {
        val repo = FakeCountUpRepository()
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        assertFalse(viewModel.state.value.isSettingsDialogOpen)

        viewModel.onEvent(CountUpUiEvent.SetSettingsDialogOpen(true))
        assertTrue(viewModel.state.value.isSettingsDialogOpen)

        viewModel.onEvent(CountUpUiEvent.SetSettingsDialogOpen(false))
        assertFalse(viewModel.state.value.isSettingsDialogOpen)
    }

    @Test
    fun `save new item adds item and closes editor`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()

        viewModel.onEvent(CountUpUiEvent.OpenEditor(target))
        assertEquals(target, viewModel.state.value.editorTarget)

        viewModel.effects.test {
            viewModel.onEvent(
                CountUpUiEvent.SaveItem(
                    name = "Water Bonsai Trees",
                    epochDay = target.epochDay,
                    comment = "Updated note",
                    icon = "yard",
                    cardColor = "terracotta",
                )
            )
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

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
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
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
    fun `reset item updates anchor date to today and shows in-card whisper`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val state = awaitItem()
            val resetItem = state.items.first { it.id == target.id }
            assertEquals(fixedToday.toEpochDay(), resetItem.epochDay)
            assertEquals(1, resetItem.resetCount)
            assertEquals(5L, resetItem.totalResetDays)
            assertEquals(5, resetItem.averageResetDays)

            val whisper = state.cardWhispers[target.id]
            assertNotNull(whisper)
            assertEquals(5L, whisper!!.releasedDays)
            assertEquals(target.epochDay, whisper.snapshot.epochDay)
            assertEquals(target.resetCount, whisper.snapshot.resetCount)
            assertEquals(target.totalResetDays, whisper.snapshot.totalResetDays)
        }
    }

    @Test
    fun `reset item does not trigger reset when accumulate days is 0`() = runTest {
        // Create item where epochDay is already today
        val zeroDayItem = CountUpItem(
            id = "zero_1",
            name = "Zero Day Item",
            epochDay = fixedToday.toEpochDay(),
            resetCount = 2,
            totalResetDays = 20L,
        )
        val repo = FakeCountUpRepository(initialItems = listOf(zeroDayItem))
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.onEvent(CountUpUiEvent.ConfirmReset(zeroDayItem.id))

        // State must remain unchanged, no whisper added
        val currentState = viewModel.state.value
        val item = currentState.items.first { it.id == zeroDayItem.id }
        assertEquals(fixedToday.toEpochDay(), item.epochDay)
        assertEquals(2, item.resetCount)
        assertEquals(20L, item.totalResetDays)
        assertTrue(currentState.cardWhispers.isEmpty())
    }

    @Test
    fun `reset item triggers reset and whisper when item has negative day count`() = runTest {
        val futureItem = CountUpItem(
            id = "future_1",
            name = "Future Countdown",
            epochDay = fixedToday.plusDays(5).toEpochDay(),
            futureFlag = true,
            resetCount = 0,
            totalResetDays = 0L,
        )
        val repo = FakeCountUpRepository(initialItems = listOf(futureItem))
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(futureItem.id))
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        val currentState = viewModel.state.value
        val item = currentState.items.first { it.id == futureItem.id }
        assertEquals(fixedToday.toEpochDay(), item.epochDay)
        assertEquals(1, item.resetCount)
        assertEquals(5L, item.totalResetDays)
        assertFalse(item.futureFlag)
        assertTrue(currentState.cardWhispers.containsKey(futureItem.id))
        assertEquals(5L, currentState.cardWhispers[futureItem.id]?.releasedDays)
    }

    @Test
    fun `reset item does not trigger reset when item accumulated days is zero`() = runTest {
        val zeroDayItem = CountUpItem(
            id = "zero_1",
            name = "Zero Days Item",
            epochDay = fixedToday.toEpochDay(),
            resetCount = 0,
            totalResetDays = 0L,
        )
        val repo = FakeCountUpRepository(initialItems = listOf(zeroDayItem))
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.onEvent(CountUpUiEvent.ConfirmReset(zeroDayItem.id))

        // State must remain unchanged, no whisper added
        val currentState = viewModel.state.value
        val item = currentState.items.first { it.id == zeroDayItem.id }
        assertEquals(zeroDayItem.epochDay, item.epochDay)
        assertEquals(0, item.resetCount)
        assertTrue(currentState.cardWhispers.isEmpty())
    }

    @Test
    fun `undo reset restores previous anchor date and metrics`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        val target = sampleItems.first()

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
            val resetEffect = awaitItem()
            assertTrue(resetEffect is CountUpUiEffect.RefreshWidget)
            assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

            viewModel.onEvent(CountUpUiEvent.UndoReset(target.id))
            val undoEffect = awaitItem()
            assertTrue(undoEffect is CountUpUiEffect.RefreshWidget)
        }

        val restoredState = viewModel.state.value
        val restoredItem = restoredState.items.first { it.id == target.id }
        assertEquals(target.epochDay, restoredItem.epochDay)
        assertEquals(target.resetCount, restoredItem.resetCount)
        assertEquals(target.totalResetDays, restoredItem.totalResetDays)
        assertFalse(restoredState.cardWhispers.containsKey(target.id))
    }

    @Test
    fun `undo reset on negative day item restores futureFlag and original future anchor`() = runTest {
        val originalFuture = CountUpItem(
            id = "future_undo",
            name = "Vacation Flight",
            epochDay = fixedToday.plusDays(14).toEpochDay(),
            futureFlag = true,
            resetCount = 0,
            totalResetDays = 0L,
        )
        val repo = FakeCountUpRepository(initialItems = listOf(originalFuture))
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(originalFuture.id))
            val resetEffect = awaitItem()
            assertTrue(resetEffect is CountUpUiEffect.RefreshWidget)

            // Whisper should report 14 days released (positive)
            val whisper = viewModel.state.value.cardWhispers[originalFuture.id]
            assertEquals(14L, whisper?.releasedDays)

            viewModel.onEvent(CountUpUiEvent.UndoReset(originalFuture.id))
            val undoEffect = awaitItem()
            assertTrue(undoEffect is CountUpUiEffect.RefreshWidget)
        }

        val restoredState = viewModel.state.value
        val restoredItem = restoredState.items.first { it.id == originalFuture.id }
        assertEquals(originalFuture.epochDay, restoredItem.epochDay)
        assertEquals(true, restoredItem.futureFlag)
        assertEquals(0, restoredItem.resetCount)
        assertEquals(0L, restoredItem.totalResetDays)
        assertFalse(restoredState.cardWhispers.containsKey(originalFuture.id))
    }

    @Test
    fun `reset arrived-future item resets futureFlag to false and computes positive released days`() = runTest {
        val arrivedFuture = CountUpItem(
            id = "arrived_vm",
            name = "Arrived Launch",
            epochDay = fixedToday.minusDays(7).toEpochDay(),
            futureFlag = true,
            resetCount = 0,
            totalResetDays = 0L,
        )
        val repo = FakeCountUpRepository(initialItems = listOf(arrivedFuture))
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmReset(arrivedFuture.id))
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        val item = viewModel.state.value.items.first { it.id == arrivedFuture.id }
        assertEquals(fixedToday.toEpochDay(), item.epochDay)
        assertEquals(false, item.futureFlag)
        assertEquals(1, item.resetCount)
        assertEquals(7L, item.totalResetDays)
        val whisper = viewModel.state.value.cardWhispers[arrivedFuture.id]
        assertEquals(7L, whisper?.releasedDays)
    }


    @Test
    fun `card whisper auto-dismisses after 5000ms timeout`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = testDispatcher)
        val target = sampleItems.first()

        // Complete initial async load on testDispatcher
        testDispatcher.scheduler.runCurrent()

        viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

        testDispatcher.scheduler.advanceTimeBy(4999L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

        testDispatcher.scheduler.advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.state.value.cardWhispers.containsKey(target.id))
    }

    @Test
    fun `restore widget reset restores item state and dismisses record`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        // Simulate widget reset previously recorded
        val record = WidgetResetRecord(
            id = "rec_1",
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 5L,
            timestampMillis = System.currentTimeMillis(),
        )
        repo.resetTo(target.id, fixedToday.toEpochDay())
        repo.recordWidgetReset(record)

        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        assertEquals(1, viewModel.state.value.pendingWidgetResets.size)
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.UndoReset(target.id))
            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        val restoredItem = viewModel.state.value.items.first { it.id == target.id }
        assertEquals(target.epochDay, restoredItem.epochDay)
        assertEquals(0, restoredItem.resetCount)
        assertTrue(viewModel.state.value.pendingWidgetResets.isEmpty())
        assertTrue(viewModel.state.value.cardWhispers.isEmpty())
    }

    @Test
    fun `dismiss card whisper removes pending reset record and clears whisper`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            id = "w_dismiss",
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 5L,
            timestampMillis = System.currentTimeMillis(),
        )
        repo.recordWidgetReset(record)

        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        assertEquals(1, viewModel.state.value.pendingWidgetResets.size)
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

        viewModel.onEvent(CountUpUiEvent.DismissCardWhisper(target.id))
        assertTrue(viewModel.state.value.pendingWidgetResets.isEmpty())
        assertFalse(viewModel.state.value.cardWhispers.containsKey(target.id))
    }

    @Test
    fun `widget reset is mapped to in-card whisper and acknowledged in persistent storage on launch`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            id = "w_whisper_test",
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 7L,
            timestampMillis = System.currentTimeMillis(),
        )
        repo.recordWidgetReset(record)
        assertEquals(1, repo.getPendingWidgetResets().size)

        // Launch ViewModel (active session starts)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)

        // Whisper is present on the card with fromWidget = true
        val whisper = viewModel.state.value.cardWhispers[target.id]
        assertNotNull("Whisper must be mapped directly to the reset card", whisper)
        assertEquals(target.id, whisper?.itemId)
        assertEquals(7L, whisper?.releasedDays)
        assertTrue(whisper?.fromWidget == true)
        assertEquals(record.id, whisper?.recordId)

        // Persistent storage was acknowledged/cleared so subsequent launches won't nag
        assertTrue("Pending resets in persistent storage must be cleared for subsequent launches", repo.getPendingWidgetResets().isEmpty())

        // Undo in-card whisper restores counter and clears whisper
        viewModel.onEvent(CountUpUiEvent.UndoReset(target.id))
        assertTrue(viewModel.state.value.cardWhispers.isEmpty())

        // Confirm counter was restored
        val restoredItem = viewModel.state.value.items.first { it.id == target.id }
        assertEquals(target.epochDay, restoredItem.epochDay)
    }

    @Test
    fun `dismiss card whisper removes in-situ whisper from memory`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            id = "w_dismiss_whisper",
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 3L,
            timestampMillis = System.currentTimeMillis(),
        )
        repo.recordWidgetReset(record)

        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

        // Dismiss whisper manually
        viewModel.onEvent(CountUpUiEvent.DismissCardWhisper(target.id))
        assertFalse(viewModel.state.value.cardWhispers.containsKey(target.id))
    }

    @Test
    fun `subsequent in-session Refresh retains card whispers and pending resets in active session state`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            id = "w_refresh_retain",
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 5L,
            timestampMillis = System.currentTimeMillis(),
        )
        repo.recordWidgetReset(record)

        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))
        assertEquals(1, viewModel.state.value.pendingWidgetResets.size)
        assertTrue(repo.getPendingWidgetResets().isEmpty()) // cleared on disk

        // Trigger onResume refresh
        viewModel.onEvent(CountUpUiEvent.Refresh)

        // Whispers and pendingWidgetResets remain intact in memory for active session
        assertTrue("Card whisper must persist across onResume refresh during active session", viewModel.state.value.cardWhispers.containsKey(target.id))
        assertEquals("Pending resets must persist across onResume refresh during active session", 1, viewModel.state.value.pendingWidgetResets.size)
    }

    @Test
    fun `delete item purges matching pendingWidgetResets and card whisper`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            id = "w_del",
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 5L,
            timestampMillis = System.currentTimeMillis(),
        )
        repo.recordWidgetReset(record)

        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
        // Reset in-app to generate whisper
        viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
        assertEquals(1, viewModel.state.value.cardWhispers.size)
        assertEquals(1, viewModel.state.value.pendingWidgetResets.size)

        // Delete the item
        viewModel.onEvent(CountUpUiEvent.ConfirmDelete(target.id))

        val state = viewModel.state.value
        assertFalse(state.items.any { it.id == target.id })
        assertTrue(state.cardWhispers.isEmpty())
        assertTrue(state.pendingWidgetResets.isEmpty())
    }

    @Test
    fun `toggle widget visibility updates item and emits toast effect`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(repo, todayProvider = { fixedToday }, ioDispatcher = mainDispatcherRule.testDispatcher)
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

    @Test
    fun `checkMidnight refreshes state and updates today when calendar date changes`() = runTest {
        var simulatedToday = fixedToday
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { simulatedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertEquals(fixedToday, viewModel.state.value.today)

        // Midnight arrives: date rolls over
        simulatedToday = fixedToday.plusDays(1)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.CheckMidnight)
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)
        }

        viewModel.state.test {
            val updatedState = awaitItem()
            assertEquals(fixedToday.plusDays(1), updatedState.today)
        }
    }

    @Test
    fun `checkMidnight is no-op when calendar date has not changed`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertEquals(fixedToday, viewModel.state.value.today)

        // Date has not changed
        viewModel.onEvent(CountUpUiEvent.CheckMidnight)

        assertEquals(fixedToday, viewModel.state.value.today)
    }

    @Test
    fun `confirmReset creates whisper and updates item to today`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val target = sampleItems.first()
        viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))

        viewModel.state.test {
            val state = awaitItem()
            val resetItem = state.items.first { it.id == target.id }
            assertEquals(fixedToday.toEpochDay(), resetItem.epochDay)
            assertEquals(1, resetItem.resetCount)

            val whisper = state.cardWhispers[target.id]
            assertNotNull(whisper)
            assertEquals(5L, whisper?.releasedDays)
            assertEquals(target.epochDay, whisper?.snapshot?.epochDay)
        }
    }

    @Test
    fun `undoReset restores original item snapshot and clears whisper`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val target = sampleItems.first()
        viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))
        assertTrue(viewModel.state.value.cardWhispers.containsKey(target.id))

        viewModel.onEvent(CountUpUiEvent.UndoReset(target.id))

        viewModel.state.test {
            val state = awaitItem()
            val restoredItem = state.items.first { it.id == target.id }
            assertEquals(target.epochDay, restoredItem.epochDay)
            assertEquals(target.resetCount, restoredItem.resetCount)
            assertFalse(state.cardWhispers.containsKey(target.id))
        }
    }

    @Test
    fun `undoReset does not clobber newer manual edit when item date changed`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val target = sampleItems.first()
        viewModel.onEvent(CountUpUiEvent.ConfirmReset(target.id))

        // Simulate user subsequently manually editing the item's date
        val manualDate = fixedToday.minusDays(100).toEpochDay()
        repo.updateItem(
            id = target.id,
            name = target.name,
            epochDay = manualDate,
            comment = target.comment,
            icon = target.icon,
            cardColor = target.cardColor,
        )

        // Attempt stale undo
        viewModel.onEvent(CountUpUiEvent.UndoReset(target.id))

        viewModel.state.test {
            val state = awaitItem()
            // Verify the manual edit was preserved, not overwritten by stale undo snapshot
            val itemInRepo = repo.getItems().first { it.id == target.id }
            assertEquals(manualDate, itemInRepo.epochDay)
            // Verify whisper was cleared
            assertFalse(state.cardWhispers.containsKey(target.id))
        }
    }


    @Test
    fun `initial loading state transitions from isLoading true to false on ioDispatcher`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = testDispatcher,
        )

        // Initial state before IO dispatcher runs has isLoading = true and empty items
        assertTrue(viewModel.state.value.isLoading)
        assertEquals(0, viewModel.state.value.items.size)

        // Advance dispatcher to execute pending initial refreshState()
        testDispatcher.scheduler.runCurrent()

        // State is now loaded
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(3, viewModel.state.value.items.size)
    }

    @Test
    fun `rapid theme and sort events emit RefreshWidget effects independently without dropping`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.effects.test {
            // Rapidly trigger theme cycle and sort order change
            viewModel.onEvent(CountUpUiEvent.CycleBackground)
            viewModel.onEvent(CountUpUiEvent.SortOrderSelected(SortOrder.NAME_ASC))
            viewModel.onEvent(CountUpUiEvent.CycleBackground)

            // Verify both snackbars and all RefreshWidget invalidations are emitted in order
            val effect1 = awaitItem()
            assertTrue(effect1 is CountUpUiEffect.ShowSnackbar)

            val effect2 = awaitItem()
            assertTrue(effect2 is CountUpUiEffect.RefreshWidget)

            val effect3 = awaitItem()
            assertTrue(effect3 is CountUpUiEffect.RefreshWidget)

            val effect4 = awaitItem()
            assertTrue(effect4 is CountUpUiEffect.ShowSnackbar)

            val effect5 = awaitItem()
            assertTrue(effect5 is CountUpUiEffect.RefreshWidget)
        }
    }

    @Test
    fun `saving item with isPinned true saves pinned status and emits RefreshWidget`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.effects.test {
            val draft = ItemDraft(
                name = "Morning Tea",
                epochDay = fixedToday.minusDays(3).toEpochDay(),
                comment = "Zen practice",
                icon = "leaf",
                cardColor = "washi_moss",
                isPinned = true,
            )
            viewModel.onEvent(CountUpUiEvent.SaveItem(draft))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.RefreshWidget)

            val updatedItems = repo.getItems()
            val savedItem = updatedItems.firstOrNull { it.name == "Morning Tea" }
            assertNotNull(savedItem)
            assertTrue(savedItem!!.isPinned)
        }
    }

    @Test
    fun `requestExportBackup closes search menu and emits TriggerExportDocument with today filename`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.onEvent(CountUpUiEvent.SetSearchSortMenuOpen(true))

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.RequestExportBackup)

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.TriggerExportDocument)
            assertEquals("CountUp_Backup_2026-08-29.json", (effect as CountUpUiEffect.TriggerExportDocument).defaultFilename)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSearchSortMenuOpen)
        }
    }

    @Test
    fun `exportBackupToStream writes valid UTF-8 JSON payload and emits ShowSnackbar success`() = runTest {
        val repo = FakeCountUpRepository(
            initialItems = sampleItems,
            initialTheme = BackgroundTheme.MOUNTAIN,
            initialSortOrder = SortOrder.DATE_DESC,
        )
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val out = java.io.ByteArrayOutputStream()
        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ExportBackupToStream(out))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.backup_export_success, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        val json = out.toByteArray().toString(Charsets.UTF_8)
        val payload = CountUpBackupPayload.decode(json)
        assertNotNull(payload)
        assertEquals(3, payload!!.items.size)
        assertEquals(SortOrder.DATE_DESC, payload.sortOrder)
        assertEquals(BackgroundTheme.MOUNTAIN, payload.backgroundTheme)
    }

    @Test
    fun `exportBackupToStream emits ShowSnackbar failure when stream write throws`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val failingStream = object : java.io.OutputStream() {
            override fun write(b: Int) {
                throw java.io.IOException("Disk full")
            }
        }

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ExportBackupToStream(failingStream))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.backup_export_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }
    }

    @Test
    fun `requestImportBackup closes search menu and emits TriggerImportDocument`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.onEvent(CountUpUiEvent.SetSearchSortMenuOpen(true))

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.RequestImportBackup)

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.TriggerImportDocument)
            val mimeTypes = (effect as CountUpUiEffect.TriggerImportDocument).mimeTypes
            assertTrue(mimeTypes.contains("application/json"))
            assertTrue(mimeTypes.contains("*/*"))
        }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSearchSortMenuOpen)
        }
    }

    @Test
    fun `importBackupFromStream parses valid payload and sets pendingRestorePayload`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val payload = CountUpBackupPayload(
            items = listOf(CountUpItem(id = "restored-1", name = "Bonsai Tree", epochDay = 20000)),
        )
        val json = CountUpBackupPayload.encode(payload)
        val inStream = java.io.ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(inStream))

        viewModel.state.test {
            val state = awaitItem()
            val pending = state.pendingRestorePayload
            assertNotNull(pending)
            assertEquals(1, pending?.items?.size)
            assertEquals("Bonsai Tree", pending?.items?.first()?.name)
        }
    }

    @Test
    fun `importBackupFromStream with unparseable payload emits ShowSnackbar invalid file`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val inStream = java.io.ByteArrayInputStream("not a valid backup".toByteArray(Charsets.UTF_8))

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(inStream))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.backup_restore_invalid_file, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.pendingRestorePayload)
        }
    }

    @Test
    fun `importBackupFromStream emits ShowSnackbar failure when stream throws`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val failingStream = object : java.io.InputStream() {
            override fun read(): Int {
                throw java.io.IOException("Stream read failure")
            }
        }

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(failingStream))

            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.backup_restore_failed, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }
    }

    @Test
    fun `confirmRestore executes restore, refreshes state, and emits RefreshWidget and success snackbar`() = runTest {
        val repo = FakeCountUpRepository(
            initialItems = sampleItems,
            initialTheme = BackgroundTheme.SAND_DUNES,
            initialSortOrder = SortOrder.DAYS_DESC,
        )
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val restorePayload = CountUpBackupPayload(
            sortOrder = SortOrder.DATE_DESC,
            themeMode = ThemeMode.DARK,
            backgroundTheme = BackgroundTheme.MOUNTAIN,
            items = listOf(CountUpItem(id = "new-1", name = "Morning Tea", epochDay = 20500)),
        )
        val json = CountUpBackupPayload.encode(restorePayload)
        viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(java.io.ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))))

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ConfirmRestore(RestoreStrategy.REPLACE_ALL))

            val effect1 = awaitItem()
            assertTrue(effect1 is CountUpUiEffect.RefreshWidget)

            val effect2 = awaitItem()
            assertTrue(effect2 is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.backup_restore_success, (effect2 as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.pendingRestorePayload)
            assertEquals(1, state.items.size)
            assertEquals("Morning Tea", state.items.first().name)
            assertEquals(SortOrder.DATE_DESC, state.sortOrder)
            assertEquals(BackgroundTheme.MOUNTAIN, state.backgroundTheme)
            assertEquals(ThemeMode.DARK, state.themeMode)
        }
    }

    @Test
    fun `dismissRestorePreview clears pendingRestorePayload`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val payload = CountUpBackupPayload(
            items = listOf(CountUpItem(id = "temp-1", name = "Temporary", epochDay = 20000)),
        )
        val json = CountUpBackupPayload.encode(payload)
        viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(java.io.ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))))

        viewModel.onEvent(CountUpUiEvent.DismissRestorePreview)

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.pendingRestorePayload)
        }
    }

    @Test
    fun `requestImportBackup emits TriggerImportDocument when items are empty`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.RequestImportBackup)
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.TriggerImportDocument)
        }
    }

    @Test
    fun `saveItem debounces duplicate submissions while isSaving`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = testDispatcher,
        )

        val draft = ItemDraft(name = "Meditation", epochDay = fixedToday.toEpochDay())

        viewModel.onEvent(CountUpUiEvent.SaveItem(draft))
        viewModel.onEvent(CountUpUiEvent.SaveItem(draft))

        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.getItems().size)
        assertFalse(viewModel.state.value.isEditorOpen)
    }

    @Test
    fun `importBackupFromStream rejects stream exceeding 2MB limit`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val oversizedBytes = ByteArray(2 * 1024 * 1024 + 10)
        val stream = java.io.ByteArrayInputStream(oversizedBytes)

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(stream))
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            assertEquals(R.string.backup_restore_file_too_large, (effect as CountUpUiEffect.ShowSnackbar).messageRes)
        }

        assertNull(viewModel.state.value.pendingRestorePayload)
    }

    @Test
    fun `importBackupFromStream with unsupported schema version emits ShowSnackbar`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val futureJson = """
            {
              "schemaVersion": 99,
              "itemsJson": "[]"
            }
        """.trimIndent()
        val stream = java.io.ByteArrayInputStream(futureJson.toByteArray(Charsets.UTF_8))

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(stream))
            val effect = awaitItem()
            assertTrue(effect is CountUpUiEffect.ShowSnackbar)
            val snackbar = effect as CountUpUiEffect.ShowSnackbar
            assertEquals(R.string.backup_restore_unsupported_version, snackbar.messageRes)
            assertEquals("99", snackbar.formatArg)
        }

        assertNull(viewModel.state.value.pendingRestorePayload)
    }

    @Test
    fun `importBackupFromStream with damaged payload marks isRestorePayloadDamaged and coerces REPLACE_ALL to MERGE`() = runTest {
        val initialItem = CountUpItem(id = "existing", name = "Existing", epochDay = 19000L)
        val repo = FakeCountUpRepository(initialItems = listOf(initialItem))
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        val damagedJson = """
            {
              "schemaVersion": 1,
              "itemsJson": "[{\"id\":\"salvaged\",\"name\":\"Salvaged Habit\",\"epochDay\":20000},{\"id\":\"broken"
        """.trimIndent()
        val stream = java.io.ByteArrayInputStream(damagedJson.toByteArray(Charsets.UTF_8))

        viewModel.onEvent(CountUpUiEvent.ImportBackupFromStream(stream))

        val state = viewModel.state.value
        assertNotNull(state.pendingRestorePayload)
        assertTrue(state.isRestorePayloadDamaged)
        val payload = state.pendingRestorePayload!!
        assertEquals(1, payload.items.size)
        assertEquals("salvaged", payload.items[0].id)

        // Attempting REPLACE_ALL on damaged payload must be coerced to MERGE_KEEP_EXISTING
        viewModel.onEvent(CountUpUiEvent.ConfirmRestore(RestoreStrategy.REPLACE_ALL))

        // In MERGE_KEEP_EXISTING, existing items are preserved and novel items appended
        val finalItems = repo.getItems()
        assertEquals(2, finalItems.size)
        assertEquals("existing", finalItems[0].id)
        assertEquals("salvaged", finalItems[1].id)
        assertFalse(viewModel.state.value.isRestorePayloadDamaged)
    }

    @Test
    fun `save item synchronizes isSaving state in CountUpUiState`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.onEvent(CountUpUiEvent.OpenEditor(null))
        assertFalse(viewModel.state.value.isSaving)

        viewModel.state.test {
            val initialState = awaitItem()
            assertFalse(initialState.isSaving)

            viewModel.onEvent(
                CountUpUiEvent.SaveItem(
                    name = "Meditation",
                    epochDay = fixedToday.toEpochDay(),
                    comment = "Zen",
                )
            )

            val savingState = awaitItem()
            assertTrue(savingState.isSaving)

            val finishedState = awaitItem()
            assertFalse(finishedState.isSaving)
            assertEquals(1, finishedState.items.size)
        }
    }

    @Test
    fun `cycleCardStyleFilter cycles through all suites in memory without persisting to repository`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.state.test {
            val initial = awaitItem()
            assertEquals(CountUpStore.OVERVIEW_FILTER_ALL, initial.cardStyleFilter)

            // Cycle: all -> washi
            viewModel.onEvent(CountUpUiEvent.CycleCardStyleFilter)
            val washiState = awaitItem()
            assertEquals("washi", washiState.cardStyleFilter)

            // Cycle: washi -> earth
            viewModel.onEvent(CountUpUiEvent.CycleCardStyleFilter)
            val earthState = awaitItem()
            assertEquals("earth", earthState.cardStyleFilter)

            // Cycle: earth -> sumi
            viewModel.onEvent(CountUpUiEvent.CycleCardStyleFilter)
            val sumiState = awaitItem()
            assertEquals("sumi", sumiState.cardStyleFilter)

            // Cycle: sumi -> all
            viewModel.onEvent(CountUpUiEvent.CycleCardStyleFilter)
            val allState = awaitItem()
            assertEquals(CountUpStore.OVERVIEW_FILTER_ALL, allState.cardStyleFilter)
        }

        // Relaunching the app (new ViewModel) must always default to All
        val relaunchedViewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
        assertEquals(CountUpStore.OVERVIEW_FILTER_ALL, relaunchedViewModel.state.value.cardStyleFilter)
    }

    @Test
    fun `cardStyleFilterSelected filters displayItems to matching category`() = runTest {
        val washiItem = CountUpItem(id = "1", name = "Washi Item", epochDay = fixedToday.toEpochDay(), cardColor = "paper_gold")
        val earthItem = CountUpItem(id = "2", name = "Earth Item", epochDay = fixedToday.toEpochDay(), cardColor = "celadon_bamboo")
        val sumiItem = CountUpItem(id = "3", name = "Sumi Item", epochDay = fixedToday.toEpochDay(), cardColor = "ink_gold")

        val repo = FakeCountUpRepository(
            initialItems = listOf(washiItem, earthItem, sumiItem),
        )
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.state.test {
            val initial = awaitItem()
            assertEquals(3, initial.displayItems.size)

            viewModel.onEvent(CountUpUiEvent.CardStyleFilterSelected("washi"))
            val washiState = awaitItem()
            assertEquals(1, washiState.displayItems.size)
            assertEquals("Washi Item", washiState.displayItems.first().name)
            assertFalse(washiState.isCardStyleMenuOpen)

            viewModel.onEvent(CountUpUiEvent.CardStyleFilterSelected("earth"))
            val earthState = awaitItem()
            assertEquals(1, earthState.displayItems.size)
            assertEquals("Earth Item", earthState.displayItems.first().name)

            viewModel.onEvent(CountUpUiEvent.CardStyleFilterSelected("sumi"))
            val sumiState = awaitItem()
            assertEquals(1, sumiState.displayItems.size)
            assertEquals("Sumi Item", sumiState.displayItems.first().name)

            viewModel.onEvent(CountUpUiEvent.CardStyleFilterSelected(CountUpStore.OVERVIEW_FILTER_ALL))
            val allState = awaitItem()
            assertEquals(3, allState.displayItems.size)
        }
    }

    @Test
    fun `openEditor preserves initialCategory in state`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.onEvent(CountUpUiEvent.OpenEditor(null, initialCategory = "earth"))
        assertEquals("earth", viewModel.state.value.editorInitialCategory)
        assertTrue(viewModel.state.value.isEditorOpen)

        viewModel.onEvent(CountUpUiEvent.CloseEditor)
        assertNull(viewModel.state.value.editorInitialCategory)
        assertFalse(viewModel.state.value.isEditorOpen)
    }

    @Test
    fun `setCardStyleMenuOpen toggles isCardStyleMenuOpen in state`() = runTest {
        val repo = FakeCountUpRepository(initialItems = emptyList())
        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertFalse(viewModel.state.value.isCardStyleMenuOpen)
        viewModel.onEvent(CountUpUiEvent.SetCardStyleMenuOpen(true))
        assertTrue(viewModel.state.value.isCardStyleMenuOpen)
        viewModel.onEvent(CountUpUiEvent.SetCardStyleMenuOpen(false))
        assertFalse(viewModel.state.value.isCardStyleMenuOpen)
    }
}
