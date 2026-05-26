package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Resident
import com.example.data.ResidentRepository
import com.example.data.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BlockSortOption {
    NONE, ASC, DESC
}

enum class FamilySizeFilter {
    ALL,
    SINGLE_OR_COUPLE, // 1-2
    MEDIUM,           // 3-4
    LARGE             // 5+
}

data class DirectoryUiState(
    val list: List<Resident> = emptyList(),
    val totalHouseholds: Int = 0,
    val totalResidents: Int = 0,
    val averageFamilySize: Double = 0.0,
    val distinctBlocksCount: Int = 0
)

class ResidentDirectoryViewModel(private val repository: ResidentRepository) : ViewModel() {

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    var authPhone by mutableStateOf("")
        private set
    var authPassword by mutableStateOf("")
        private set
    var isRegisterMode by mutableStateOf(false)
        private set
    var authPhoneError by mutableStateOf<String?>(null)
        private set
    var authPasswordError by mutableStateOf<String?>(null)
        private set
    var authGeneralError by mutableStateOf<String?>(null)
        private set

    // --- Directory Filtering & Sorting State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBlockSort = MutableStateFlow(BlockSortOption.NONE)
    val selectedBlockSort = _selectedBlockSort.asStateFlow()

    private val _familySizeFilter = MutableStateFlow(FamilySizeFilter.ALL)
    val familySizeFilter = _familySizeFilter.asStateFlow()

    // --- Resident Form State (For adding/editing resident) ---
    var currentEditingResident: Resident? by mutableStateOf(null)
        private set

    var formBuildingNumber by mutableStateOf("")
        private set
    var formBlock by mutableStateOf("")
        private set
    var formName by mutableStateOf("")
        private set
    var formPhoneNumber by mutableStateOf("")
        private set
    var formFamilySize by mutableStateOf("1")
        private set
    var formIsTenant by mutableStateOf(false)
        private set
    var formOwnerName by mutableStateOf("")
        private set
    var formOwnerPhone by mutableStateOf("")
        private set

    // Simple Form Validation States
    var errorBuildingNumber by mutableStateOf<String?>(null)
        private set
    var errorBlock by mutableStateOf<String?>(null)
        private set
    var errorName by mutableStateOf<String?>(null)
        private set
    var errorPhoneNumber by mutableStateOf<String?>(null)
        private set
    var errorFamilySize by mutableStateOf<String?>(null)
        private set
    var errorOwnerName by mutableStateOf<String?>(null)
        private set
    var errorOwnerPhone by mutableStateOf<String?>(null)
        private set

    // --- Unified Reactive UI State scoped per authenticating user ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DirectoryUiState> = _currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(DirectoryUiState())
        } else {
            combine(
                repository.getResidents(user.id),
                _searchQuery,
                _selectedBlockSort,
                _familySizeFilter
            ) { residents, query, sort, familyFilter ->
                
                // 1. Search filtering
                var filteredList = if (query.trim().isEmpty()) {
                    residents
                } else {
                    val q = query.lowercase().trim()
                    residents.filter {
                        it.name.lowercase().contains(q) ||
                        it.block.lowercase().contains(q) ||
                        it.buildingNumber.lowercase().contains(q) ||
                        it.phoneNumber.lowercase().contains(q) ||
                        (it.ownerName?.lowercase()?.contains(q) == true)
                    }
                }

                // 2. Family size filtering
                filteredList = when (familyFilter) {
                    FamilySizeFilter.ALL -> filteredList
                    FamilySizeFilter.SINGLE_OR_COUPLE -> filteredList.filter { it.familySize <= 2 }
                    FamilySizeFilter.MEDIUM -> filteredList.filter { it.familySize in 3..4 }
                    FamilySizeFilter.LARGE -> filteredList.filter { it.familySize >= 5 }
                }

                // 3. Sorting by block
                filteredList = when (sort) {
                    BlockSortOption.NONE -> filteredList
                    BlockSortOption.ASC -> filteredList.sortedWith(
                        compareBy<Resident> { it.block.lowercase() }.thenBy { it.buildingNumber }
                    )
                    BlockSortOption.DESC -> filteredList.sortedWith(
                        compareByDescending<Resident> { it.block.lowercase() }.thenBy { it.buildingNumber }
                    )
                }

                // 4. Calculate stats on database list of this specific user
                val totalHouseholds = residents.size
                val totalResidentsCount = residents.sumOf { it.familySize }
                val avgFamilySize = if (totalHouseholds > 0) totalResidentsCount.toDouble() / totalHouseholds else 0.0
                val uniqueBlocks = residents.map { it.block.uppercase().trim() }.distinct().size

                DirectoryUiState(
                    list = filteredList,
                    totalHouseholds = totalHouseholds,
                    totalResidents = totalResidentsCount,
                    averageFamilySize = avgFamilySize,
                    distinctBlocksCount = uniqueBlocks
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DirectoryUiState()
    )

    // --- Auth View Bindings ---
    fun onAuthPhoneChange(value: String) {
        authPhone = value
        authPhoneError = null
        authGeneralError = null
    }

    fun onAuthPasswordChange(value: String) {
        authPassword = value
        authPasswordError = null
        authGeneralError = null
    }

    fun toggleRegisterMode() {
        isRegisterMode = !isRegisterMode
        authPhoneError = null
        authPasswordError = null
        authGeneralError = null
    }

    // --- Auth Execution Functions ---
    fun register(onSuccess: () -> Unit) {
        var isValid = true
        if (authPhone.trim().length < 6) {
            authPhoneError = "Phone must be at least 6 characters"
            isValid = false
        }
        if (authPassword.length < 4) {
            authPasswordError = "Password must be at least 4 characters"
            isValid = false
        }
        if (!isValid) return

        viewModelScope.launch {
            try {
                val existing = repository.getUserByPhone(authPhone.trim())
                if (existing != null) {
                    authGeneralError = "User with this phone number already exists"
                } else {
                    val newId = repository.registerUser(authPhone, authPassword)
                    val registeredUser = repository.getUserById(newId)
                    _currentUser.value = registeredUser
                    onSuccess()
                }
            } catch (e: Exception) {
                authGeneralError = "Registration error: ${e.localizedMessage}"
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        var isValid = true
        if (authPhone.trim().isEmpty()) {
            authPhoneError = "Phone status is required"
            isValid = false
        }
        if (authPassword.isEmpty()) {
            authPasswordError = "Password status is required"
            isValid = false
        }
        if (!isValid) return

        viewModelScope.launch {
            try {
                val user = repository.loginUser(authPhone, authPassword)
                if (user != null) {
                    _currentUser.value = user
                    onSuccess()
                } else {
                    authGeneralError = "Invalid phone or password"
                }
            } catch (e: Exception) {
                authGeneralError = "Login error: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        authPhone = ""
        authPassword = ""
        _searchQuery.value = ""
        _selectedBlockSort.value = BlockSortOption.NONE
        _familySizeFilter.value = FamilySizeFilter.ALL
    }

    // --- Directory Actions & State Updates ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBlockSort(option: BlockSortOption) {
        _selectedBlockSort.value = option
    }

    fun setFamilySizeFilter(filter: FamilySizeFilter) {
        _familySizeFilter.value = filter
    }

    // --- Form UI Input Handlers ---
    fun onBuildingNumberChange(value: String) {
        formBuildingNumber = value
        if (value.isNotBlank()) errorBuildingNumber = null
    }

    fun onBlockChange(value: String) {
        formBlock = value
        if (value.isNotBlank()) errorBlock = null
    }

    fun onNameChange(value: String) {
        formName = value
        if (value.isNotBlank()) errorName = null
    }

    fun onPhoneNumberChange(value: String) {
        formPhoneNumber = value
        if (value.isNotBlank()) errorPhoneNumber = null
    }

    fun onFamilySizeChange(value: String) {
        formFamilySize = value
        val parsed = value.toIntOrNull()
        if (parsed != null && parsed >= 1) errorFamilySize = null
    }

    fun onIsTenantChange(value: Boolean) {
        formIsTenant = value
        if (!value) {
            errorOwnerName = null
            errorOwnerPhone = null
        }
    }

    fun onOwnerNameChange(value: String) {
        formOwnerName = value
        if (value.isNotBlank()) errorOwnerName = null
    }

    fun onOwnerPhoneChange(value: String) {
        formOwnerPhone = value
        if (value.isNotBlank()) errorOwnerPhone = null
    }

    fun startAdd() {
        currentEditingResident = null
        formBuildingNumber = ""
        formBlock = ""
        formName = ""
        formPhoneNumber = ""
        formFamilySize = "1"
        formIsTenant = false
        formOwnerName = ""
        formOwnerPhone = ""
        clearErrors()
    }

    fun startEdit(resident: Resident) {
        currentEditingResident = resident
        formBuildingNumber = resident.buildingNumber
        formBlock = resident.block
        formName = resident.name
        formPhoneNumber = resident.phoneNumber
        formFamilySize = resident.familySize.toString()
        formIsTenant = resident.isTenant
        formOwnerName = resident.ownerName ?: ""
        formOwnerPhone = resident.ownerPhone ?: ""
        clearErrors()
    }

    private fun clearErrors() {
        errorBuildingNumber = null
        errorBlock = null
        errorName = null
        errorPhoneNumber = null
        errorFamilySize = null
        errorOwnerName = null
        errorOwnerPhone = null
    }

    fun saveResident(onSuccess: () -> Unit): Boolean {
        val currentUserId = _currentUser.value?.id ?: return false
        var isValid = true

        if (formBuildingNumber.trim().isBlank()) {
            errorBuildingNumber = "Building No. is required"
            isValid = false
        }
        if (formBlock.trim().isBlank()) {
            errorBlock = "Block is required"
            isValid = false
        }
        if (formName.trim().isBlank()) {
            errorName = "Resident name is required"
            isValid = false
        }
        
        val cleanedPhone = formPhoneNumber.trim()
        if (cleanedPhone.isBlank()) {
            errorPhoneNumber = "Phone number is required"
            isValid = false
        }

        val familySizeInt = formFamilySize.trim().toIntOrNull()
        if (familySizeInt == null || familySizeInt < 1) {
            errorFamilySize = "Family size must be at least 1"
            isValid = false
        }

        // Validate Tenant custom fields
        if (formIsTenant) {
            if (formOwnerName.trim().isBlank()) {
                errorOwnerName = "Owner name is required for Tenant"
                isValid = false
            }
            if (formOwnerPhone.trim().isBlank()) {
                errorOwnerPhone = "Owner phone is required for Tenant"
                isValid = false
            }
        }

        if (!isValid) return false

        viewModelScope.launch {
            val blockTrim = formBlock.trim()
            val bNoTrim = formBuildingNumber.trim()
            val nameTrim = formName.trim()

            val existing = repository.findResidentByUniqueComposite(currentUserId, blockTrim, bNoTrim, nameTrim)
            if (existing != null && existing.id != (currentEditingResident?.id ?: 0L)) {
                errorName = "A resident with name \"$nameTrim\" already exists in Block ${blockTrim.uppercase()}, Room $bNoTrim."
                return@launch
            }

            val residentToSave = Resident(
                id = currentEditingResident?.id ?: 0L,
                userId = currentUserId,
                buildingNumber = bNoTrim,
                block = blockTrim.uppercase(),
                name = nameTrim,
                phoneNumber = cleanedPhone,
                familySize = familySizeInt ?: 1,
                isTenant = formIsTenant,
                ownerName = if (formIsTenant) formOwnerName.trim() else null,
                ownerPhone = if (formIsTenant) formOwnerPhone.trim() else null
            )
            repository.insertResident(residentToSave)
            onSuccess()
        }
        return true
    }

    fun deleteResident(resident: Resident) {
        viewModelScope.launch {
            repository.deleteResident(resident)
        }
    }

    fun addSampleResidents() {
        val currentUserId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val samples = listOf(
                Resident(
                    userId = currentUserId,
                    buildingNumber = "101",
                    block = "A",
                    name = "John Doe (Owner)",
                    phoneNumber = "9876543210",
                    familySize = 3,
                    isTenant = false
                ),
                Resident(
                    userId = currentUserId,
                    buildingNumber = "304",
                    block = "B",
                    name = "Alice Smith (Tenant)",
                    phoneNumber = "8765432109",
                    familySize = 2,
                    isTenant = true,
                    ownerName = "Michael Scott",
                    ownerPhone = "555-0128"
                ),
                Resident(
                    userId = currentUserId,
                    buildingNumber = "12",
                    block = "A",
                    name = "Robert Johnson (Owner)",
                    phoneNumber = "7654321098",
                    familySize = 5,
                    isTenant = false
                ),
                Resident(
                    userId = currentUserId,
                    buildingNumber = "202",
                    block = "C",
                    name = "Emily Brown (Tenant)",
                    phoneNumber = "6543210987",
                    familySize = 1,
                    isTenant = true,
                    ownerName = "Bruce Wayne",
                    ownerPhone = "555-1939"
                ),
                Resident(
                    userId = currentUserId,
                    buildingNumber = "505",
                    block = "B",
                    name = "Michael Wilson (Owner)",
                    phoneNumber = "5432109876",
                    familySize = 4,
                    isTenant = false
                )
            )
            for (res in samples) {
                repository.insertResident(res)
            }
        }
    }

    class Factory(private val repository: ResidentRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResidentDirectoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ResidentDirectoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
