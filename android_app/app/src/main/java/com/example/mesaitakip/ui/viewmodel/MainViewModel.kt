package com.example.mesaitakip.ui.viewmodel

import androidx.lifecycle.*
import com.example.mesaitakip.data.entities.OvertimeRecord
import com.example.mesaitakip.data.entities.Week
import com.example.mesaitakip.repository.OvertimeRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: OvertimeRepository) : ViewModel() {

    private val _weeks = MutableLiveData<List<Week>>()
    val weeks: LiveData<List<Week>> = _weeks

    private val _currentWeekRecords = MutableLiveData<List<OvertimeRecord>>()
    val currentWeekRecords: LiveData<List<OvertimeRecord>> = _currentWeekRecords

    fun loadWeeks(userId: Int) {
        viewModelScope.launch {
            _weeks.postValue(repository.getWeeksForUser(userId))
        }
    }

    fun addWeek(week: Week, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertWeek(week)
            loadWeeks(week.user_id)
            onComplete(id)
        }
    }

    fun deleteWeek(week: Week) {
        viewModelScope.launch {
            repository.deleteWeek(week)
            loadWeeks(week.user_id)
        }
    }

    fun loadRecords(weekId: Int) {
        viewModelScope.launch {
            _currentWeekRecords.postValue(repository.getRecordsForWeek(weekId))
        }
    }

    fun addRecord(record: OvertimeRecord) {
        viewModelScope.launch {
            repository.insertRecord(record)
            loadRecords(record.hafta_id)
        }
    }

    fun addRecords(records: List<OvertimeRecord>) {
        viewModelScope.launch {
            if (records.isNotEmpty()) {
                repository.insertRecords(records)
                loadRecords(records[0].hafta_id)
            }
        }
    }

    fun updateRecord(record: OvertimeRecord) {
        viewModelScope.launch {
            repository.updateRecord(record)
            loadRecords(record.hafta_id)
        }
    }

    fun deleteRecord(record: OvertimeRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            loadRecords(record.hafta_id)
        }
    }
}

class MainViewModelFactory(private val repository: OvertimeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
