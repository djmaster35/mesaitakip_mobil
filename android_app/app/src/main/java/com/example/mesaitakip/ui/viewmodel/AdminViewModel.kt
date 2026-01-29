package com.example.mesaitakip.ui.viewmodel

import androidx.lifecycle.*
import com.example.mesaitakip.data.entities.User
import com.example.mesaitakip.repository.UserRepository
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: UserRepository) : ViewModel() {

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsers.postValue(repository.getAllUsers())
        }
    }

    fun toggleBan(user: User) {
        viewModelScope.launch {
            val updatedUser = user.copy(is_banned = if (user.is_banned == 1) 0 else 1)
            repository.updateUser(updatedUser)
            loadAllUsers()
        }
    }

    fun toggleAdmin(user: User) {
        viewModelScope.launch {
            val updatedUser = user.copy(is_admin = if (user.is_admin == 1) 0 else 1)
            repository.updateUser(updatedUser)
            loadAllUsers()
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
            loadAllUsers()
        }
    }
}

class AdminViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
