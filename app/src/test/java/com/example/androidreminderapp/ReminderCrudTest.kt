package com.example.androidreminderapp

import android.content.Context
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.domain.repository.ReminderRepository
import com.example.androidreminderapp.ui.reminder.AddEditReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderCrudTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeReminderRepository
    private lateinit var viewModel: AddEditReminderViewModel
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeReminderRepository()
        viewModel = AddEditReminderViewModel(fakeRepository)
        mockContext = android.app.Application()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBlankTitleValidation() {
        viewModel.title = "   "
        viewModel.saveReminder(mockContext) {}
        assertNotNull(viewModel.errorMessage)
        assertTrue(fakeRepository.remindersMap.isEmpty())
    }

    @Test
    fun testInsertAndRetrieveReminder() = runTest {
        viewModel.title = "Buy Groceries"
        viewModel.description = "Milk, eggs, bread"
        viewModel.saveReminder(mockContext) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.remindersMap.size.toLong())
        val saved = fakeRepository.remindersMap.values.first()
        assertEquals("Buy Groceries", saved.title)
        assertEquals("Milk, eggs, bread", saved.description)
    }

    @Test
    fun testUpdateExistingReminder() = runTest {
        // Insert initial
        val reminder = Reminder(id = 1L, title = "Meeting", startDate = System.currentTimeMillis(), timeInMillis = System.currentTimeMillis())
        fakeRepository.insertReminder(reminder)

        viewModel.loadReminder(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Meeting", viewModel.title)
        viewModel.title = "Important Team Meeting"
        viewModel.saveReminder(mockContext) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = fakeRepository.getReminderById(1L)
        assertEquals("Important Team Meeting", updated?.title)
    }

    @Test
    fun testDeleteReminder() = runTest {
        val reminder = Reminder(id = 2L, title = "Gym", startDate = System.currentTimeMillis(), timeInMillis = System.currentTimeMillis())
        fakeRepository.insertReminder(reminder)
        assertEquals(1, fakeRepository.remindersMap.size.toLong())

        fakeRepository.deleteReminder(reminder)
        assertNull(fakeRepository.getReminderById(2L))
    }

    @Test
    fun testEnableDisableReminder() = runTest {
        val reminder = Reminder(id = 3L, title = "Task", isActive = true, startDate = System.currentTimeMillis(), timeInMillis = System.currentTimeMillis())
        fakeRepository.insertReminder(reminder)

        val updated = reminder.copy(isActive = false)
        fakeRepository.updateReminder(updated)

        val fetched = fakeRepository.getReminderById(3L)
        assertFalse(fetched!!.isActive)
    }

    @Test
    fun testDuplicateSavePrevention() = runTest {
        viewModel.title = "Gym"
        viewModel.isSaving = true
        viewModel.saveReminder(mockContext) {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepository.remindersMap.isEmpty())
    }
}

class FakeReminderRepository : ReminderRepository {
    val remindersMap = mutableMapOf<Long, Reminder>()
    private val remindersFlow = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1L

    override fun getAllReminders(): Flow<List<Reminder>> = remindersFlow

    override suspend fun getReminderById(id: Long): Reminder? = remindersMap[id]

    override suspend fun getActiveReminders(): List<Reminder> = remindersMap.values.filter { it.isActive }

    override suspend fun insertReminder(reminder: Reminder): Long {
        val id = if (reminder.id == 0L) nextId++ else reminder.id
        val newReminder = reminder.copy(id = id)
        remindersMap[id] = newReminder
        remindersFlow.update { remindersMap.values.toList() }
        return id
    }

    override suspend fun updateReminder(reminder: Reminder) {
        remindersMap[reminder.id] = reminder
        remindersFlow.update { remindersMap.values.toList() }
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        remindersMap.remove(reminder.id)
        remindersFlow.update { remindersMap.values.toList() }
    }

    override suspend fun deleteReminderById(id: Long) {
        remindersMap.remove(id)
        remindersFlow.update { remindersMap.values.toList() }
    }
}
