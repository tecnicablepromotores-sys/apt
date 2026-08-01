#!/bin/bash
sed -i '/val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()/a \
    private val _isUserAuthenticated = MutableStateFlow(false)\
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()' app/src/main/java/com/example/MainViewModel.kt

sed -i '/val currentUser = auth.currentUser/a \
            _isUserAuthenticated.value = currentUser != null' app/src/main/java/com/example/MainViewModel.kt
