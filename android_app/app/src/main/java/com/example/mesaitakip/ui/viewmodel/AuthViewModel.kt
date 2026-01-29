package com.example.mesaitakip.ui.viewmodel

import androidx.lifecycle.*
import com.example.mesaitakip.data.entities.User
import com.example.mesaitakip.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null) {
                if (user.is_banned == 1) {
                    _loginResult.postValue(Result.failure(Exception("Hesabınız banlanmıştır.")))
                } else if (user.password == password) {
                    _currentUser.postValue(user)
                    _loginResult.postValue(Result.success(user))
                } else {
                    _loginResult.postValue(Result.failure(Exception("Hatalı şifre.")))
                }
            } else {
                _loginResult.postValue(Result.failure(Exception("Kullanıcı bulunamadı.")))
            }
        }
    }

    fun register(username: String, password: String, adsoyad: String) {
        viewModelScope.launch {
            val existing = repository.getUserByUsername(username)
            if (existing != null) {
                _registerResult.postValue(Result.failure(Exception("Bu kullanıcı adı zaten alınmış.")))
                return@launch
            }
            val newUser = User(username = username, password = password, adsoyad = adsoyad)
            val id = repository.insertUser(newUser)
            _registerResult.postValue(Result.success(newUser.copy(id = id.toInt())))
        }
    }

    fun logout() {
        _currentUser.postValue(null)
    }
}

class AuthViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
