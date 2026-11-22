package com.urbancha.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.urbancha.inventory.data.Bill
import com.urbancha.inventory.data.Material
import com.urbancha.inventory.data.Product
import com.urbancha.inventory.data.Restock
import com.urbancha.inventory.data.Shift
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.ShopRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val authListener = FirebaseAuth.AuthStateListener { _user.value = it.currentUser }

    /** The shift that is running now. Re-checked every 30 s so the app rolls over at 3 AM by itself. */
    private val _currentShiftId = MutableStateFlow(ShiftClock.currentShiftId())
    val currentShiftId: StateFlow<String> = _currentShiftId

    init {
        auth.addAuthStateListener(authListener)
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _currentShiftId.value = ShiftClock.currentShiftId()
            }
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
    }

    private val repo: StateFlow<ShopRepository?> = user
        .map { u -> u?.let { ShopRepository(db, it.uid) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun <T> live(initial: T, block: (ShopRepository) -> Flow<T>): StateFlow<T> =
        repo.flatMapLatest { r -> r?.let(block) ?: flowOf(initial) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val products: StateFlow<List<Product>> = live(emptyList()) { it.products() }
    val materials: StateFlow<List<Material>> = live(emptyList()) { it.materials() }

    val currentShift: StateFlow<Shift> =
        combine(repo, currentShiftId) { r, id -> r to id }
            .flatMapLatest { (r, id) -> r?.shift(id) ?: flowOf(Shift(id)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Shift(ShiftClock.currentShiftId()))

    /**
     * The shift the Close screen works on. Closing often happens just after 3 AM, when the clock has
     * already moved to the next shift — so if the previous shift is still open and the new one hasn't
     * started, close the previous one.
     */
    val closableShift: StateFlow<Shift> =
        combine(repo, currentShiftId) { r, id -> r to id }
            .flatMapLatest { (r, id) ->
                if (r == null) flowOf(Shift(id))
                else combine(r.shift(id), r.shift(ShiftClock.previous(id))) { cur, prev ->
                    if (cur.status == Shift.STATUS_NOT_STARTED && prev.status == Shift.STATUS_OPEN) prev else cur
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Shift(ShiftClock.currentShiftId()))

    val currentRestocks: StateFlow<List<Restock>> =
        combine(repo, currentShiftId) { r, id -> r to id }
            .flatMapLatest { (r, id) -> r?.restocks(id) ?: flowOf(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shift(id: String): Flow<Shift> = repo.flatMapLatest { it?.shift(id) ?: flowOf(Shift(id)) }
    fun shiftRange(firstId: String, lastId: String): Flow<List<Shift>> =
        repo.flatMapLatest { it?.shiftRange(firstId, lastId) ?: flowOf(emptyList()) }
    fun bills(id: String): Flow<List<Bill>> = repo.flatMapLatest { it?.bills(id) ?: flowOf(emptyList()) }
    fun restocks(id: String): Flow<List<Restock>> = repo.flatMapLatest { it?.restocks(id) ?: flowOf(emptyList()) }

    // ---------- Actions ----------

    fun recordSale(lines: Map<Product, Int>, payment: String) = repo.value?.recordSale(lines, payment)
    fun startShift(received: Map<String, Double>) = repo.value?.startShift(materials.value, received)
    fun restock(m: Material, qty: Double) = repo.value?.restock(m, qty)
    fun closeShift(counted: Map<String, Double>) = repo.value?.closeShift(closableShift.value, materials.value, counted)
    fun saveMaterial(id: String?, name: String, unit: String, qty: Double?, lowAlert: Double, cost: Double) =
        repo.value?.saveMaterial(id, name, unit, qty, lowAlert, cost)
    fun deleteMaterial(id: String) = repo.value?.deleteMaterial(id)
    fun saveProduct(id: String?, name: String, price: Double, stockItemId: String?) =
        repo.value?.saveProduct(id, name, price, stockItemId)
    fun deleteProduct(id: String) = repo.value?.deleteProduct(id)
    fun loadStarterData() = repo.value?.loadStarterData()

    // ---------- Sign in ----------

    val authError = MutableStateFlow<String?>(null)
    val authBusy = MutableStateFlow(false)

    fun signIn(email: String, password: String, create: Boolean) {
        viewModelScope.launch {
            authBusy.value = true
            authError.value = null
            try {
                if (create) auth.createUserWithEmailAndPassword(email.trim(), password).await()
                else auth.signInWithEmailAndPassword(email.trim(), password).await()
            } catch (e: Exception) {
                authError.value = e.localizedMessage ?: "Could not sign in"
            } finally {
                authBusy.value = false
            }
        }
    }

    fun signOut() = auth.signOut()
}
