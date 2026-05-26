package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Resident
import com.example.data.ResidentRepository
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Establish the Room Database with User and Resident DAOs
        val database = AppDatabase.getDatabase(this)
        val repository = ResidentRepository(database.residentDao(), database.userDao())

        setContent {
            MyApplicationTheme {
                val viewModel: ResidentDirectoryViewModel = viewModel(
                    factory = ResidentDirectoryViewModel.Factory(repository)
                )

                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                
                var isShowFormDialog by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        // Display secure authentication screen first
                        AuthScreen(viewModel = viewModel)
                    } else {
                        // Display Resident Directory upon success
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = {
                                        viewModel.startAdd()
                                        isShowFormDialog = true
                                    },
                                    modifier = Modifier
                                        .testTag("add_resident_fab")
                                        .navigationBarsPadding(),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add New Resident"
                                    )
                                }
                            }
                        ) { innerPadding ->
                            ResidentDirectoryScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                modifier = Modifier.padding(innerPadding),
                                onOpenForm = { isShowFormDialog = true }
                            )

                            if (isShowFormDialog) {
                                ResidentFormDialog(
                                    viewModel = viewModel,
                                    onDismiss = { isShowFormDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: ResidentDirectoryViewModel) {
    val context = LocalContext.current
    val isRegisterMode = viewModel.isRegisterMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header decoration
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isRegisterMode) "Create Account" else "Resident Log In",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isRegisterMode) "Register your resident directories profile" else "Log in to safely manage your private community list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Inputs
                OutlinedTextField(
                    value = viewModel.authPhone,
                    onValueChange = { viewModel.onAuthPhoneChange(it) },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = viewModel.authPhoneError != null,
                    supportingText = viewModel.authPhoneError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_phone_input"),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                    }
                )

                OutlinedTextField(
                    value = viewModel.authPassword,
                    onValueChange = { viewModel.onAuthPasswordChange(it) },
                    label = { Text("Password") },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = viewModel.authPasswordError != null,
                    supportingText = viewModel.authPasswordError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    }
                )

                // General Error Message
                if (viewModel.authGeneralError != null) {
                    Text(
                        text = viewModel.authGeneralError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Action Button
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            viewModel.register {
                                Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.login {
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Register" else "Log In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Toggle Link
                Text(
                    text = if (isRegisterMode) "Already have an account? Log In" else "Don't have an account? Register",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { viewModel.toggleRegisterMode() }
                        .testTag("auth_toggle_mode")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentDirectoryScreen(
    viewModel: ResidentDirectoryViewModel,
    uiState: DirectoryUiState,
    modifier: Modifier = Modifier,
    onOpenForm: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBlockSort by viewModel.selectedBlockSort.collectAsStateWithLifecycle()
    val familySizeFilter by viewModel.familySizeFilter.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "App Architecture Logo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Resident Directory",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            actions = {
                // Auto seed option
                if (uiState.list.isEmpty() && searchQuery.isEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.addSampleResidents()
                            Toast.makeText(context, "Loaded community samples", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Load Sample Data",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Samples", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Export to Excel/CSV button
                if (uiState.list.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            exportToExcel(context, uiState.list)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export to Excel/CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Logout button
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Highlights Overview Panel
        CommunityStatsSection(uiState)

        // Search Box in styled container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = { Text("Search by name, block, owner, phone...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search Input"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Filters Section
        FiltersAndSortingBar(
            selectedBlockSort = selectedBlockSort,
            familySizeFilter = familySizeFilter,
            onSortChange = { viewModel.setBlockSort(it) },
            onFamilyFilterChange = { viewModel.setFamilySizeFilter(it) }
        )

        // Main List Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.list.isEmpty()) {
                EmptyStateView(
                    isSearchActive = searchQuery.isNotEmpty() || familySizeFilter != FamilySizeFilter.ALL,
                    onClearFilters = {
                        viewModel.updateSearchQuery("")
                        viewModel.setBlockSort(BlockSortOption.NONE)
                        viewModel.setFamilySizeFilter(FamilySizeFilter.ALL)
                    },
                    onAddSample = { viewModel.addSampleResidents() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.list,
                        key = { it.id }
                    ) { resident ->
                        ResidentCardItem(
                            resident = resident,
                            onEdit = {
                                viewModel.startEdit(resident)
                                onOpenForm()
                            },
                            onDelete = {
                                viewModel.deleteResident(resident)
                                Toast.makeText(context, "${resident.name} removed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityStatsSection(uiState: DirectoryUiState) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatCard(
                title = "Total Units",
                value = uiState.totalHouseholds.toString(),
                icon = Icons.Default.Home,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        item {
            StatCard(
                title = "Residents",
                value = uiState.totalResidents.toString(),
                icon = Icons.Default.Person,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        item {
            StatCard(
                title = "Total Blocks",
                value = uiState.distinctBlocksCount.toString(),
                icon = Icons.Default.Menu,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        item {
            StatCard(
                title = "Avg / Family",
                value = "%.1f".format(uiState.averageFamilySize),
                icon = Icons.Default.Info,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .width(130.dp)
            .height(68.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FiltersAndSortingBar(
    selectedBlockSort: BlockSortOption,
    familySizeFilter: FamilySizeFilter,
    onSortChange: (BlockSortOption) -> Unit,
    onFamilyFilterChange: (FamilySizeFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Block Sort:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(85.dp)
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val active = selectedBlockSort == BlockSortOption.NONE
                    InputChip(
                        selected = active,
                        onClick = { onSortChange(BlockSortOption.NONE) },
                        label = { Text("None") },
                        modifier = Modifier
                            .testTag("sort_block_none")
                            .height(32.dp)
                    )
                }
                item {
                    val active = selectedBlockSort == BlockSortOption.ASC
                    InputChip(
                        selected = active,
                        onClick = { onSortChange(BlockSortOption.ASC) },
                        label = { Text("A → Z") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .testTag("sort_block_asc")
                            .height(32.dp)
                    )
                }
                item {
                    val active = selectedBlockSort == BlockSortOption.DESC
                    InputChip(
                        selected = active,
                        onClick = { onSortChange(BlockSortOption.DESC) },
                        label = { Text("Z → A") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .testTag("sort_block_desc")
                            .height(32.dp)
                    )
                }
            }
        }

        // Family Dynamic Filter Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Family Size:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(85.dp)
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val active = familySizeFilter == FamilySizeFilter.ALL
                    InputChip(
                        selected = active,
                        onClick = { onFamilyFilterChange(FamilySizeFilter.ALL) },
                        label = { Text("All") },
                        modifier = Modifier
                            .testTag("filter_family_all")
                            .height(32.dp)
                    )
                }
                item {
                    val active = familySizeFilter == FamilySizeFilter.SINGLE_OR_COUPLE
                    InputChip(
                        selected = active,
                        onClick = { onFamilyFilterChange(FamilySizeFilter.SINGLE_OR_COUPLE) },
                        label = { Text("1-2 Members") },
                        modifier = Modifier
                            .testTag("filter_family_single_couple")
                            .height(32.dp)
                    )
                }
                item {
                    val active = familySizeFilter == FamilySizeFilter.MEDIUM
                    InputChip(
                        selected = active,
                        onClick = { onFamilyFilterChange(FamilySizeFilter.MEDIUM) },
                        label = { Text("3-4 Members") },
                        modifier = Modifier
                            .testTag("filter_family_medium")
                            .height(32.dp)
                    )
                }
                item {
                    val active = familySizeFilter == FamilySizeFilter.LARGE
                    InputChip(
                        selected = active,
                        onClick = { onFamilyFilterChange(FamilySizeFilter.LARGE) },
                        label = { Text("5+ Members") },
                        modifier = Modifier
                            .testTag("filter_family_large")
                            .height(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResidentCardItem(
    resident: Resident,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    // Aesthetic color hash representing different Blocks
    val blockColor = remember(resident.block) {
        val hash = resident.block.uppercase().hashCode()
        val colors = listOf(
            Color(0xFF6750A4), Color(0xFF388E3C), Color(0xFF0288D1), 
            Color(0xFFF57C00), Color(0xFFD32F2F), Color(0xFF7B1FA2)
        )
        colors[kotlin.math.abs(hash) % colors.size]
    }

    // High visual contrast or light tint color backgrounds to highlight TENANTS differently to clearly bifurcate
    val cardBorder = if (resident.isTenant) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val cardBackground = if (resident.isTenant) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("resident_card_${resident.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Block/Apt Circle Icon Indicator
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(blockColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resident.block.take(2).uppercase(),
                        color = blockColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Basic details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = resident.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )

                        // Bifurcated badge
                        if (resident.isTenant) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Tenant", style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        } else {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Owner", style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = "Block ${resident.block} • Apt/Room ${resident.buildingNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Edit/Delete buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Record",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata info row (Household Members & Action dialer link)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${resident.familySize} ${if (resident.familySize == 1) "member" else "members"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${resident.phoneNumber}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Underlying system dialer failed to load", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Dial Primary Phone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = resident.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Quick Option
                AssistChip(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${resident.phoneNumber}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Underlying system dialer failed to load", Toast.LENGTH_SHORT).show()
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("Call", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f)
                )

                // SMS Send Message Option
                AssistChip(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${resident.phoneNumber}")
                                putExtra("sms_body", "Hello ${resident.name}, ")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not launch SMS interface", Toast.LENGTH_SHORT).show()
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    label = { Text("SMS", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f)
                )

                // WhatsApp message (custom green color theme)
                val whatsappBrandColor = Color(0xFF25D366)
                AssistChip(
                    onClick = {
                        try {
                            val cleanPhone = resident.phoneNumber.filter { it.isDigit() || it == '+' }
                            val encodedMsg = Uri.encode("Hello ${resident.name}, ")
                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open WhatsApp interface", Toast.LENGTH_SHORT).show()
                        }
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(whatsappBrandColor)
                        )
                    },
                    label = { Text("WhatsApp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = whatsappBrandColor
                    ),
                    modifier = Modifier.weight(1.3f)
                )
            }

            // Tenant Custom Highlights Block
            if (resident.isTenant && (!resident.ownerName.isNullOrBlank() || !resident.ownerPhone.isNullOrBlank())) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Landed Owner Details:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = resident.ownerName ?: "Not Specified",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!resident.ownerPhone.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${resident.ownerPhone}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No system dialer tool available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Landed Owner",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = resident.ownerPhone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isSearchActive: Boolean,
    onClearFilters: () -> Unit,
    onAddSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearchActive) Icons.Default.Search else Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isSearchActive) "No matches found" else "Ready to add residents?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = if (isSearchActive) 
                "Try adjusting your search queries, sort, or members buckets." 
                else "Your isolated resident directory is currently empty. Get started by inserting households manually or seeding templates.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (isSearchActive) {
            Button(onClick = onClearFilters) {
                Text("Reset All Filters")
            }
        } else {
            OutlinedButton(onClick = onAddSample) {
                Text("Load Sample Community")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentFormDialog(
    viewModel: ResidentDirectoryViewModel,
    onDismiss: () -> Unit
) {
    val isEditMode = viewModel.currentEditingResident != null
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = if (isEditMode) "Edit Resident Info" else "Add New Resident",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Scrollable fields
                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Block and Room No.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        var blockExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = viewModel.formBlock,
                                onValueChange = { viewModel.onBlockChange(it) },
                                label = { Text("Block") },
                                placeholder = { Text("A") },
                                isError = viewModel.errorBlock != null,
                                supportingText = viewModel.errorBlock?.let { { Text(it) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("block_input_dropdown"),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { blockExpanded = !blockExpanded }) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand Block Options"
                                        )
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = blockExpanded,
                                onDismissRequest = { blockExpanded = false }
                            ) {
                                val databaseBlocks = uiState.list.map { r -> r.block.uppercase().trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .sorted()
                                val finalBlocks = (listOf("A", "B") + databaseBlocks).distinct()
                                finalBlocks.forEach { blockName ->
                                    DropdownMenuItem(
                                        text = { Text(blockName) },
                                        onClick = {
                                            viewModel.onBlockChange(blockName)
                                            blockExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = viewModel.formBuildingNumber,
                            onValueChange = { viewModel.onBuildingNumberChange(it) },
                            label = { Text("Room / Apt") },
                            placeholder = { Text("304") },
                            isError = viewModel.errorBuildingNumber != null,
                            supportingText = viewModel.errorBuildingNumber?.let { { Text(it) } },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                    }

                    // Name
                    OutlinedTextField(
                        value = viewModel.formName,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Resident Name") },
                        placeholder = { Text("Jane Doe") },
                        isError = viewModel.errorName != null,
                        supportingText = viewModel.errorName?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        }
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = viewModel.formPhoneNumber,
                        onValueChange = { viewModel.onPhoneNumberChange(it) },
                        label = { Text("Phone Number") },
                        placeholder = { Text("9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = viewModel.errorPhoneNumber != null,
                        supportingText = viewModel.errorPhoneNumber?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                        }
                    )

                    // Members Stepper
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No. of Family Members",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    val current = viewModel.formFamilySize.toIntOrNull() ?: 1
                                    if (current > 1) {
                                        viewModel.onFamilySizeChange((current - 1).toString())
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = viewModel.formFamilySize,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            FilledIconButton(
                                onClick = {
                                    val current = viewModel.formFamilySize.toIntOrNull() ?: 1
                                    viewModel.onFamilySizeChange((current + 1).toString())
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // BIFURCATION FLAGS (Tenant Switch & Nested dynamic field layout)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Column {
                                        Text(
                                            text = "Is Tenant?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Requires owner's information",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = viewModel.formIsTenant,
                                    onCheckedChange = { viewModel.onIsTenantChange(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                        checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    modifier = Modifier.testTag("tenant_switch")
                                )
                            }

                            AnimatedVisibility(
                                visible = viewModel.formIsTenant,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                    
                                    Text(
                                        text = "Landed Owner Information",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedTextField(
                                        value = viewModel.formOwnerName,
                                        onValueChange = { viewModel.onOwnerNameChange(it) },
                                        label = { Text("Owner Full Name") },
                                        placeholder = { Text("e.g. Martha Wayne") },
                                        isError = viewModel.errorOwnerName != null,
                                        supportingText = viewModel.errorOwnerName?.let { { Text(it) } },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("owner_name_input"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = viewModel.formOwnerPhone,
                                        onValueChange = { viewModel.onOwnerPhoneChange(it) },
                                        label = { Text("Owner Phone Number") },
                                        placeholder = { Text("e.g. 555-1234") },
                                        isError = viewModel.errorOwnerPhone != null,
                                        supportingText = viewModel.errorOwnerPhone?.let { { Text(it) } },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("owner_phone_input"),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.saveResident(onSuccess = {
                                Toast.makeText(
                                    context,
                                    if (isEditMode) "Changes saved" else "Resident saved successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            })
                        },
                        modifier = Modifier.testTag("submit_resident_button")
                    ) {
                        Text(if (isEditMode) "Save Changes" else "Add Resident")
                    }
                }
            }
        }
    }
}

/**
 * Builds a beautifully formatted CSV stream (Excel compatible) and shares it
 * using a FileProvider with standard ACTION_SEND chooser.
 */
fun exportToExcel(context: android.content.Context, residents: List<com.example.data.Resident>) {
    try {
        val csvHeader = "ID,Block,Apartment/Room,Name,Phone Number,Type,Family Size,Owner Name,Owner Phone\n"
        val csvBody = residents.joinToString("\n") { r ->
            val typeStr = if (r.isTenant) "Tenant" else "Owner"
            val ownerN = r.ownerName ?: ""
            val ownerP = r.ownerPhone ?: ""
            
            // Helper to escape commas and quotes in CSV data fields
            fun escapeCsv(s: String): String {
                if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
                    return "\"" + s.replace("\"", "\"\"") + "\""
                }
                return s
            }
            
            "${r.id},${escapeCsv(r.block)},${escapeCsv(r.buildingNumber)},${escapeCsv(r.name)},${escapeCsv(r.phoneNumber)},$typeStr,${r.familySize},${escapeCsv(ownerN)},${escapeCsv(ownerP)}"
        }
        
        // Include UTF-8 byte order mark (BOM) so MS Excel opens non-ASCII characters layout cleanly
        val fileContent = "\uFEFF" + csvHeader + csvBody
        val fileName = "Resident_Directory_${System.currentTimeMillis()}.csv"
        val file = java.io.File(context.cacheDir, fileName)
        file.writeText(fileContent, java.nio.charset.StandardCharsets.UTF_8)
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Resident Directory Excel Export")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(intent, "Export Resident Directory"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}
