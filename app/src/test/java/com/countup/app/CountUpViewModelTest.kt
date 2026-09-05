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

        viewModel.effects.test {
            viewModel.onEvent(CountUpUiEvent.RestoreWidgetReset(record))
            val refreshEffect = awaitItem()
            assertTrue(refreshEffect is CountUpUiEffect.RefreshWidget)
        }

        val restoredItem = viewModel.state.value.items.first { it.id == target.id }
        assertEquals(target.epochDay, restoredItem.epochDay)
        assertEquals(0, restoredItem.resetCount)
        assertTrue(viewModel.state.value.pendingWidgetResets.isEmpty())
    }

    @Test
    fun `dismiss widget reset removes record from pendingWidgetResets`() = runTest {
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

        viewModel.onEvent(CountUpUiEvent.DismissWidgetReset(record.id))
        assertTrue(viewModel.state.value.pendingWidgetResets.isEmpty())
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
    fun `restoreWidgetReset restores snapshot and removes pending widget reset record`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 5L,
            timestampMillis = 1000L,
        )
        repo.recordWidgetReset(record)
        repo.resetTo(target.id, fixedToday.toEpochDay())

        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertEquals(1, viewModel.state.value.pendingWidgetResets.size)

        viewModel.onEvent(CountUpUiEvent.RestoreWidgetReset(record))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(0, state.pendingWidgetResets.size)
            val restored = state.items.first { it.id == target.id }
            assertEquals(target.epochDay, restored.epochDay)
        }
    }

    @Test
    fun `dismissWidgetReset removes pending record from repository and state`() = runTest {
        val repo = FakeCountUpRepository(initialItems = sampleItems)
        val target = sampleItems.first()
        val record = WidgetResetRecord(
            itemId = target.id,
            itemName = target.name,
            snapshot = target.toResetSnapshot(),
            releasedDays = 5L,
            timestampMillis = 1000L,
        )
        repo.recordWidgetReset(record)

        val viewModel = CountUpViewModel(
            repository = repo,
            todayProvider = { fixedToday },
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertEquals(1, viewModel.state.value.pendingWidgetResets.size)

        viewModel.onEvent(CountUpUiEvent.DismissWidgetReset(record.id))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(0, state.pendingWidgetResets.size)
        }
    }
}
