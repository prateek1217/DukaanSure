package com.example.dukaaan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class LoginType { object Owner : LoginType(); object Staff : LoginType() }

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(val userId: String, val shopId: String? = null, val staffName: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel : ViewModel() {
    private val _loginType = MutableStateFlow<LoginType>(LoginType.Owner)
    val loginType: StateFlow<LoginType> = _loginType

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password
    private val _shopCode = MutableStateFlow("")
    val shopCode: StateFlow<String> = _shopCode
    private val _staffId = MutableStateFlow("")
    val staffId: StateFlow<String> = _staffId

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun setLoginType(type: LoginType) { _loginType.value = type }
    fun setEmail(email: String) { _email.value = email }
    fun setPassword(password: String) { _password.value = password }
    fun setShopCode(code: String) { _shopCode.value = code }
    fun setStaffId(id: String) { _staffId.value = id }

    fun loginOwner() {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val result = auth.signInWithEmailAndPassword(_email.value, _password.value).await()
                val user = result.user
                if (user != null) {
                    // Optionally fetch shop data for this owner
                    _authResult.value = AuthResult.Success(user.uid)
                } else {
                    _authResult.value = AuthResult.Error("Authentication failed.")
                }
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.localizedMessage ?: "Login failed.")
            }
        }
    }

    fun loginStaff() {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                // 1. Find shop by code
                val shopQuery = firestore.collection("shops")
                    .whereEqualTo("shop_code", _shopCode.value)
                    .get().await()
                if (shopQuery.isEmpty) {
                    _authResult.value = AuthResult.Error("Invalid shop code.")
                    return@launch
                }
                val shop = shopQuery.documents.first()
                val shopId = shop.getString("shop_id") ?: shop.id
                // 2. Find staff by id and shop_id
                val staffQuery = firestore.collection("staff")
                    .whereEqualTo("staff_id", _staffId.value)
                    .whereEqualTo("shop_id", shopId)
                    .get().await()
                if (staffQuery.isEmpty) {
                    _authResult.value = AuthResult.Error("Invalid staff ID.")
                    return@launch
                }
                val staffDoc = staffQuery.documents.first()
                val active = staffDoc.getBoolean("active_status") ?: false
                if (!active) {
                    _authResult.value = AuthResult.Error("Staff is not active.")
                    return@launch
                }
                val staffName = staffDoc.getString("name") ?: ""
                // 3. Update last_login
                firestore.collection("staff").document(staffDoc.id)
                    .update("last_login", System.currentTimeMillis()).await()
                _authResult.value = AuthResult.Success(
                    userId = staffDoc.getString("staff_id") ?: staffDoc.id,
                    shopId = shopId,
                    staffName = staffName
                )
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.localizedMessage ?: "Login failed.")
            }
        }
    }

    fun resetAuthResult() { _authResult.value = AuthResult.Idle }

    fun logout() {
        auth.signOut()
        _authResult.value = AuthResult.Idle
    }
} 