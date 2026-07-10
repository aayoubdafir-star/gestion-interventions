package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.model.AppNotification
import com.example.data.model.Inspector
import com.example.data.model.Intervention
import com.example.data.model.UserAccount
import com.example.ui.theme.*

@Composable
fun OcpLogoMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.img_app_icon_1783681977153),
        contentDescription = "OCP Logo Icon",
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
    )
}

@Composable
fun OcpLogo(modifier: Modifier = Modifier, showText: Boolean = true) {
    if (showText) {
        Image(
            painter = painterResource(id = R.drawable.img_ocp_logo_1783681956445),
            contentDescription = "OCP Maintenance Solutions Logo",
            modifier = modifier
                .height(42.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        OcpLogoMark(modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterventionsApp(viewModel: InterventionViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        val currentRole by viewModel.currentRole.collectAsState()
        val selectedInspectorId by viewModel.selectedInspectorId.collectAsState()
        val simulatedTimeMinutes by viewModel.simulatedTimeMinutes.collectAsState()

        val inspectors by viewModel.inspectors.collectAsState()
        val interventions by viewModel.interventions.collectAsState()
        val notifications by viewModel.filteredNotifications.collectAsState()

    var activeTab by remember { mutableStateOf("interventions") } // "interventions", "inspectors", "notifications"
    var showCreateInterventionDialog by remember { mutableStateOf(false) }
    var showCreateInspectorDialog by remember { mutableStateOf(false) }
    var showCreateChefDialog by remember { mutableStateOf(false) }
    var personnelSubTab by remember { mutableStateOf("inspectors") } // "inspectors", "chefs"

    val scope = rememberCoroutineScope()
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionTargetTab by remember { mutableStateOf("interventions") }

    val navigateToTab: (String) -> Unit = { target ->
        if (activeTab != target && !isTransitioning) {
            transitionTargetTab = target
            scope.launch {
                isTransitioning = true
                kotlinx.coroutines.delay(450)
                activeTab = target
                isTransitioning = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Main Header
                TopAppBar(
                    title = {
                        OcpLogo()
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ),
                    actions = {
                        val currentUserEmail by viewModel.currentUserEmail.collectAsState()
                        if (currentUserEmail != null) {
                            Text(
                                text = currentUserEmail!!.substringBefore("@"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp).testTag("header_username")
                            )
                        }
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("logout_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Déconnexion",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Quick switch role
                        var showRoleMenu by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { showRoleMenu = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("role_selector_btn")
                            ) {
                                Icon(
                                    imageVector = when (currentRole) {
                                        "CHEF_DE_PROJET" -> Icons.Default.BusinessCenter
                                        "HEAD" -> Icons.Default.AdminPanelSettings
                                        else -> Icons.Default.Engineering
                                    },
                                    contentDescription = "Rôle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (currentRole) {
                                        "CHEF_DE_PROJET" -> "Chef Projet"
                                        "HEAD" -> "Head (Full)"
                                        else -> "Inspecteur"
                                    },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                            DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Head (Accès complet)") },
                                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, "Head") },
                                    onClick = {
                                        viewModel.setRole("HEAD")
                                        showRoleMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Chef de Projet") },
                                    leadingIcon = { Icon(Icons.Default.BusinessCenter, "Chef de Projet") },
                                    onClick = {
                                        viewModel.setRole("CHEF_DE_PROJET")
                                        showRoleMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Inspecteur") },
                                    leadingIcon = { Icon(Icons.Default.Engineering, "Inspecteur") },
                                    onClick = {
                                        viewModel.setRole("INSPECTEUR")
                                        showRoleMenu = false
                                    }
                                )
                            }
                        }
                    }
                )

                // Sub-Bar for Inspector Selection when active role is INSPECTEUR
                AnimatedVisibility(visible = currentRole == "INSPECTEUR") {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Inspecteur Actif :",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            var showInspectorMenu by remember { mutableStateOf(false) }
                            val activeInspector = inspectors.find { it.id == selectedInspectorId }

                            Box {
                                OutlinedButton(
                                    onClick = { showInspectorMenu = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("active_inspector_spinner")
                                ) {
                                    Text(
                                        text = activeInspector?.name ?: "Sélectionner un inspecteur",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                                DropdownMenu(
                                    expanded = showInspectorMenu,
                                    onDismissRequest = { showInspectorMenu = false }
                                ) {
                                    if (inspectors.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Aucun inspecteur inscrit") },
                                            onClick = { showInspectorMenu = false }
                                        )
                                    } else {
                                        inspectors.forEach { ins ->
                                            DropdownMenuItem(
                                                text = { Text(ins.name) },
                                                onClick = {
                                                    viewModel.setSelectedInspectorId(ins.id)
                                                    showInspectorMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Time Simulator banner
                TimeSimulatorBar(
                    simulatedTimeMinutes = simulatedTimeMinutes,
                    onAdvanceTime = { viewModel.advanceTime(it) },
                    onResetTime = { viewModel.resetTimeSimulation() }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                val currentSelectedTab = if (isTransitioning) transitionTargetTab else activeTab
                NavigationBarItem(
                    selected = currentSelectedTab == "interventions",
                    onClick = { navigateToTab("interventions") },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Interventions") },
                    label = { Text("Interventions") },
                    modifier = Modifier.testTag("tab_interventions")
                )
                NavigationBarItem(
                    selected = currentSelectedTab == "inspectors",
                    onClick = { navigateToTab("inspectors") },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Inspecteurs") },
                    label = { Text("Inspecteurs") },
                    modifier = Modifier.testTag("tab_inspectors")
                )
                NavigationBarItem(
                    selected = currentSelectedTab == "notifications",
                    onClick = { navigateToTab("notifications") },
                    icon = {
                        Box {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            val unreadCount = notifications.count { !it.isRead }
                            if (unreadCount > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                ) {
                                    Text(unreadCount.toString(), fontSize = 9.sp)
                                }
                            }
                        }
                    },
                    label = { Text("Notifications") },
                    modifier = Modifier.testTag("tab_notifications")
                )
            }
        },
        floatingActionButton = {
            if (activeTab == "interventions" && (currentRole == "CHEF_DE_PROJET" || currentRole == "HEAD")) {
                FloatingActionButton(
                    onClick = { showCreateInterventionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_intervention_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Créer intervention")
                }
            } else if (activeTab == "inspectors" && (currentRole == "CHEF_DE_PROJET" || currentRole == "HEAD")) {
                FloatingActionButton(
                    onClick = {
                        if (personnelSubTab == "inspectors") {
                            showCreateInspectorDialog = true
                        } else {
                            showCreateChefDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("add_inspector_fab")
                ) {
                    Icon(
                        imageVector = if (personnelSubTab == "inspectors") Icons.Default.PersonAdd else Icons.Default.PersonAdd, // Both are valid, PersonAdd is extremely safe
                        contentDescription = if (personnelSubTab == "inspectors") "Ajouter Inspecteur" else "Ajouter Chef de Projet"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isTransitioning) {
                val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseAlpha"
                )

                Column(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OcpLogoMark(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connexion OCP en cours...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                when (activeTab) {
                    "interventions" -> {
                        InterventionsListScreen(
                            viewModel = viewModel,
                            interventions = interventions,
                            inspectors = inspectors,
                            currentRole = currentRole,
                            selectedInspectorId = selectedInspectorId
                        )
                    }
                    "inspectors" -> {
                        InspectorsScreen(
                            viewModel = viewModel,
                            inspectors = inspectors,
                            currentRole = currentRole,
                            subTab = personnelSubTab,
                            onSubTabChange = { personnelSubTab = it }
                        )
                    }
                    "notifications" -> {
                        NotificationsScreen(
                            viewModel = viewModel,
                            notifications = notifications
                        )
                    }
                }
            }
        }
    }

    // Dialog for creating intervention
    if (showCreateInterventionDialog) {
        CreateInterventionDialog(
            inspectors = inspectors,
            onDismiss = { showCreateInterventionDialog = false },
            onSave = { client, email, phone, type, factory, zone, equip, selectedIds ->
                viewModel.createIntervention(client, email, phone, type, factory, zone, equip, selectedIds)
                showCreateInterventionDialog = false
            }
        )
    }

    // Dialog for adding inspector
    if (showCreateInspectorDialog) {
        CreateInspectorDialog(
            onDismiss = { showCreateInspectorDialog = false },
            onSave = { name, email, phone, specialty ->
                viewModel.addInspector(name, email, phone, specialty)
                showCreateInspectorDialog = false
            }
        )
    }

    // Dialog for adding chef de projet
    if (showCreateChefDialog) {
        CreateChefDialog(
            onDismiss = { showCreateChefDialog = false },
            onSave = { name, email, phone ->
                viewModel.addChefDeProjet(name, email, phone)
                showCreateChefDialog = false
            }
        )
    }
}
}

@Composable
fun TimeSimulatorBar(
    simulatedTimeMinutes: Int,
    onAdvanceTime: (Int) -> Unit,
    onResetTime: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Simulateur",
                        tint = WarningYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Simulateur de Temps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val hrs = simulatedTimeMinutes / 60
                val mins = simulatedTimeMinutes % 60
                Text(
                    text = "Temps écoulé : ${hrs}h ${mins}m",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { onAdvanceTime(30) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+30 min", fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = { onAdvanceTime(120) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+2 h", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onResetTime,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Reset", modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun InterventionsListScreen(
    viewModel: InterventionViewModel,
    interventions: List<Intervention>,
    inspectors: List<Inspector>,
    currentRole: String,
    selectedInspectorId: Int?
) {
    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "CLOSED", "MINE"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Horizontal Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = mutableListOf(
                "ALL" to "Toutes",
                "ACTIVE" to "En cours / Actives",
                "CLOSED" to "Clôturées"
            )
            if (currentRole == "INSPECTEUR" && selectedInspectorId != null) {
                filterOptions.add(0, "MINE" to "Mes Missions")
            }

            filterOptions.forEach { (key, label) ->
                val selected = filterStatus == key
                FilterChip(
                    selected = selected,
                    onClick = { filterStatus = key },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Interventions List
        val filteredList = interventions.filter { item ->
            when (filterStatus) {
                "ACTIVE" -> !item.isClosed
                "CLOSED" -> item.isClosed
                "MINE" -> selectedInspectorId != null && item.assignedInspectorIds.contains(selectedInspectorId)
                else -> true
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentLate,
                        contentDescription = "Aucune intervention",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucune intervention trouvée",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Sélectionnez un autre filtre ou cliquez sur le bouton + pour en créer une nouvelle.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    InterventionItemCard(
                        intervention = item,
                        inspectors = inspectors,
                        currentRole = currentRole,
                        selectedInspectorId = selectedInspectorId,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun InterventionItemCard(
    intervention: Intervention,
    inspectors: List<Inspector>,
    currentRole: String,
    selectedInspectorId: Int?,
    viewModel: InterventionViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var showNotDoneDialog by remember { mutableStateOf(false) }

    val isAssignedToMe = selectedInspectorId != null && intervention.assignedInspectorIds.contains(selectedInspectorId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("intervention_card_${intervention.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (intervention.isClosed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Client & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = intervention.clientName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = intervention.interventionType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                StatusBadge(status = intervention.status, isClosed = intervention.isClosed)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoLabelAndValue(
                    icon = Icons.Default.Factory,
                    label = "Usine",
                    value = intervention.factory
                )
                InfoLabelAndValue(
                    icon = Icons.Default.Map,
                    label = "Zone / Équip.",
                    value = "${intervention.zone} - ${intervention.equipment}"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Assigned Inspectors List
            val assignedNames = inspectors
                .filter { intervention.assignedInspectorIds.contains(it.id) }
                .map { it.name }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = "Inspectors",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Inspecteurs : " + if (assignedNames.isEmpty()) "Aucun" else assignedNames.joinToString(", "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded view with details and workflow actions
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Client contact details
                    Text(
                        text = "Contact Client :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📧 ${intervention.clientContactEmail}", fontSize = 13.sp)
                        Text("📞 ${intervention.clientContactPhone}", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stepper timeline
                    WorkflowProgressStepper(intervention = intervention)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Close message if closed
                    if (intervention.isClosed && !intervention.closeMessage.isNullOrBlank()) {
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Intervention clôturée avec succès 🎉",
                                    fontWeight = FontWeight.Bold,
                                    color = OcpMediumBlue,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = intervention.closeMessage,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Remark if not done
                    if (intervention.status == "PAS_FAIT" && !intervention.remarkIfNotDone.isNullOrBlank()) {
                        Surface(
                            color = ErrorRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Raison du non-achèvement :",
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = intervention.remarkIfNotDone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Report Details
                    if (intervention.status == "TERMINE") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Statut du Rapport",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = when (intervention.reportStatus) {
                                            "AUCUN" -> "Pas encore commencé"
                                            "EN_COURS_REDACTION" -> "En cours de rédaction"
                                            "FAIT" -> "Terminé & Réalisé"
                                            else -> "Aucun"
                                        },
                                        fontSize = 13.sp,
                                        color = if (intervention.reportStatus == "FAIT") SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (intervention.reportValidated) {
                                        Badge(containerColor = SuccessGreen) {
                                            Text("Rapport Validé", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (intervention.reportSentToClient) {
                                        Badge(containerColor = InfoBlue) {
                                            Text("Envoyé au client", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- WORKFLOW ACTIONS ---
                    if (!intervention.isClosed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Actions disponibles :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // A. Actions for INSPECTEUR
                        if (currentRole == "INSPECTEUR" || currentRole == "HEAD") {
                            val prefix = if (currentRole == "HEAD") "[En tant qu'Inspecteur] " else ""
                            
                            if (isAssignedToMe || currentRole == "HEAD") {
                                when (intervention.status) {
                                    "CREE" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.updateInterventionStatusByInspector(intervention, "ACCEPTE")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                modifier = Modifier.weight(1f).testTag("action_accepter")
                                            ) {
                                                Icon(Icons.Default.Check, "Accepter")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${prefix}Accepter")
                                            }
                                            Button(
                                                onClick = { showNotDoneDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                                modifier = Modifier.weight(1f).testTag("action_pas_fait")
                                            ) {
                                                Icon(Icons.Default.Close, "Pas fait")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${prefix}Pas Fait")
                                            }
                                        }
                                    }
                                    "ACCEPTE" -> {
                                        Button(
                                            onClick = {
                                                viewModel.updateInterventionStatusByInspector(intervention, "EN_COURS")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                                            modifier = Modifier.fillMaxWidth().testTag("action_commencer")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, "Commencer")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${prefix}Commencer l'intervention")
                                        }
                                    }
                                    "EN_COURS" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.updateInterventionStatusByInspector(intervention, "TERMINE")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = OcpMediumBlue),
                                                modifier = Modifier.weight(1.5f).testTag("action_terminer")
                                            ) {
                                                Icon(Icons.Default.DoneAll, "Terminer")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${prefix}Terminer")
                                            }
                                            OutlinedButton(
                                                onClick = { showNotDoneDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Pas Fait")
                                            }
                                        }
                                    }
                                    "TERMINE" -> {
                                        // Report management actions
                                        if (intervention.reportStatus != "FAIT") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (intervention.reportStatus != "EN_COURS_REDACTION") {
                                                    Button(
                                                        onClick = {
                                                            viewModel.updateReportStatusByInspector(intervention, "EN_COURS_REDACTION")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = OcpMediumBlue),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Rédiger Rapport")
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.updateReportStatusByInspector(intervention, "FAIT")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                    modifier = Modifier.weight(1.2f).testTag("action_rapport_fait")
                                                ) {
                                                    Icon(Icons.Default.Save, "Sauvegarder")
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Rapport Terminé")
                                                }
                                            }
                                        } else if (intervention.reportValidated && !intervention.reportSentToClient) {
                                            Button(
                                                onClick = {
                                                    viewModel.confirmReportSent(intervention)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                                                modifier = Modifier.fillMaxWidth().testTag("action_envoyer_client")
                                            ) {
                                                Icon(Icons.Default.Send, "Envoyer")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${prefix}Confirmer Envoi au Client")
                                            }
                                        } else {
                                            Text(
                                                text = "Attente d'action du Chef de projet (validation ou clôture)...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = WarningYellow
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Vous n'êtes pas assigné à cette intervention.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // B. Actions for CHEF_DE_PROJET or HEAD
                        if (currentRole == "CHEF_DE_PROJET" || currentRole == "HEAD") {
                            val prefix = if (currentRole == "HEAD") "[Chef de Projet] " else ""

                            if (intervention.reportStatus == "FAIT" && !intervention.reportValidated) {
                                Button(
                                    onClick = {
                                        viewModel.validateReport(intervention, isApproved = true)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("action_valider_rapport")
                                ) {
                                    Icon(Icons.Default.CheckCircle, "Valider")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${prefix}Valider le Rapport")
                                }
                            } else if (intervention.reportSentToClient && !intervention.isClosed) {
                                Button(
                                    onClick = { showCloseDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = OcpNavy),
                                    modifier = Modifier.fillMaxWidth().testTag("action_cloturer")
                                ) {
                                    Icon(Icons.Default.Lock, "Clôturer")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${prefix}Clôturer l'intervention")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue for Closing Intervention with motivational message
    if (showCloseDialog) {
        var motivationalMsg by remember { mutableStateOf("Excellent travail d'équipe ! Merci pour votre rigueur et professionnalisme.") }
        
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Clôturer l'intervention", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Veuillez saisir un message de motivation destiné aux inspecteurs assignés :",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = motivationalMsg,
                        onValueChange = { motivationalMsg = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("close_msg_input"),
                        placeholder = { Text("Message de motivation...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeIntervention(intervention, motivationalMsg)
                        showCloseDialog = false
                    },
                    modifier = Modifier.testTag("close_dialog_confirm")
                ) {
                    Text("Clôturer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal dialogue for Remark of non-completed intervention
    if (showNotDoneDialog) {
        var remark by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNotDoneDialog = false },
            title = { Text("Marquer non effectuée", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Veuillez expliquer pourquoi cette intervention n'a pas pu être effectuée :",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("remark_input"),
                        placeholder = { Text("Ex: Équipement inaccessible, usine fermée, panne de courant...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateInterventionStatusByInspector(intervention, "PAS_FAIT", remark)
                        showNotDoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("remark_dialog_confirm")
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotDoneDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun InfoLabelAndValue(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatusBadge(status: String, isClosed: Boolean) {
    val containerColor: Color
    val contentColor: Color
    val text: String

    if (isClosed) {
        containerColor = MaterialTheme.colorScheme.surfaceVariant
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        text = "Clôturée"
    } else {
        when (status) {
            "CREE" -> {
                containerColor = InfoBlue.copy(alpha = 0.15f)
                contentColor = InfoBlue
                text = "Créée"
            }
            "ACCEPTE" -> {
                containerColor = OcpMediumBlue.copy(alpha = 0.15f)
                contentColor = OcpMediumBlue
                text = "Acceptée"
            }
            "EN_COURS" -> {
                containerColor = WarningYellow.copy(alpha = 0.15f)
                contentColor = WarningYellow
                text = "En Cours"
            }
            "PAS_FAIT" -> {
                containerColor = ErrorRed.copy(alpha = 0.15f)
                contentColor = ErrorRed
                text = "Pas Faite"
            }
            "TERMINE" -> {
                containerColor = SuccessGreen.copy(alpha = 0.15f)
                contentColor = SuccessGreen
                text = "Terminée"
            }
            else -> {
                containerColor = Color.Gray.copy(alpha = 0.15f)
                contentColor = Color.Gray
                text = "Inconnu"
            }
        }
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun WorkflowProgressStepper(intervention: Intervention) {
    val steps = listOf("Créée", "Acceptée", "En Cours", "Terminée", "Rapport Fait", "Validé", "Client", "Clôturée")
    
    val activeIndex = when {
        intervention.isClosed -> 7
        intervention.reportSentToClient -> 6
        intervention.reportValidated -> 5
        intervention.reportStatus == "FAIT" -> 4
        intervention.status == "TERMINE" -> 3
        intervention.status == "EN_COURS" -> 2
        intervention.status == "ACCEPTE" -> 1
        intervention.status == "PAS_FAIT" -> 3 // Mark terminal as terminated red
        else -> 0
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Étapes de l'intervention :", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                val isCompleted = index <= activeIndex
                val isError = intervention.status == "PAS_FAIT" && index == 3
                
                val color = when {
                    isError -> ErrorRed
                    isCompleted -> SuccessGreen
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = step,
                        fontSize = 8.sp,
                        fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun InspectorsScreen(
    viewModel: InterventionViewModel,
    inspectors: List<Inspector>,
    currentRole: String,
    subTab: String = "inspectors",
    onSubTabChange: (String) -> Unit = {}
) {
    var selectedProfileId by remember { mutableStateOf<Int?>(null) }
    val userAccounts by viewModel.userAccounts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // OcpLogo decorative banner
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OcpLogoMark(modifier = Modifier.size(50.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "OCP Maintenance Solutions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Système de Gestion & Annuaire du Personnel",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (currentRole == "INSPECTEUR") {
            // Inspector Account Workspace: Enter and update personal information
            Text(
                text = "Mon Espace Inspecteur ⚙️",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configurez votre fiche et saisissez/mettez à jour vos informations personnelles.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (inspectors.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "Aucun inspecteur n'a été invité pour le moment. Veuillez basculer sur le rôle 'Chef de Projet' ou 'Head' pour ajouter un inspecteur par invitation email.",
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Selector
                var showProfileSelector by remember { mutableStateOf(false) }
                val activeProfile = inspectors.find { it.id == selectedProfileId } ?: inspectors.first()
                if (selectedProfileId == null) {
                    selectedProfileId = activeProfile.id
                }

                Text("Sélectionner mon profil actif :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(
                        onClick = { showProfileSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${activeProfile.name} (${activeProfile.email})")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = showProfileSelector,
                        onDismissRequest = { showProfileSelector = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        inspectors.forEach { ins ->
                            DropdownMenuItem(
                                text = { Text("${ins.name} (${ins.email})") },
                                onClick = {
                                    selectedProfileId = ins.id
                                    showProfileSelector = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile form for Saisie d'information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Saisie & Modification de mes Informations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        var editName by remember(activeProfile.id) { mutableStateOf(activeProfile.name) }
                        var editEmail by remember(activeProfile.id) { mutableStateOf(activeProfile.email) }
                        var editPhone by remember(activeProfile.id) { mutableStateOf(activeProfile.phone) }
                        var editSpecialty by remember(activeProfile.id) { mutableStateOf(activeProfile.specialty) }

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nom complet") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("E-mail (Lien d'invitation)") },
                            readOnly = true, // email remains identifier
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Téléphone professionnel") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editSpecialty,
                            onValueChange = { editSpecialty = it },
                            label = { Text("Spécialités / Qualifications") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.updateInspector(
                                    activeProfile.copy(
                                        name = editName,
                                        phone = editPhone,
                                        specialty = editSpecialty
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_profile_btn"),
                            enabled = editName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrer mon profil inspecteur")
                        }
                    }
                }
            }
        } else {
            // Head or Chef de Projet: View the registry with custom sub-tabs
            TabRow(
                selectedTabIndex = if (subTab == "inspectors") 0 else 1,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = subTab == "inspectors",
                    onClick = { onSubTabChange("inspectors") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inspecteurs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                )
                Tab(
                    selected = subTab == "chefs",
                    onClick = { onSubTabChange("chefs") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chefs de Projet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                )
            }

            if (subTab == "inspectors") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Annuaire des Inspecteurs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Liste des inspecteurs invités qui collaborent sur les interventions terrain.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (inspectors.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GroupOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucun inspecteur inscrit.", fontWeight = FontWeight.SemiBold)
                            Text("Invitez un inspecteur par email en cliquant sur le bouton '+'", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    inspectors.forEach { ins ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("inspector_card_${ins.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ins.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("🔧 Spécialité: ${ins.specialty}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("📧 Email: ${ins.email}", fontSize = 12.sp)
                                    Text("📞 Tél: ${ins.phone}", fontSize = 12.sp)
                                }
                                if (currentRole == "CHEF_DE_PROJET" || currentRole == "HEAD") {
                                    IconButton(
                                        onClick = { viewModel.deleteInspector(ins) },
                                        modifier = Modifier.testTag("delete_inspector_btn_${ins.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Annuaire des Chefs de Projet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Liste des chefs de projet autorisés à configurer le planning et créer des interventions.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val chefs = userAccounts.filter { it.role == "CHEF_DE_PROJET" || it.role == "HEAD" }
                if (chefs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GroupOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucun chef de projet inscrit.", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    chefs.forEach { chef ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("chef_card_${chef.email}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(chef.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(chef.role, fontSize = 10.sp) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                    Text("📧 Email: ${chef.email}", fontSize = 12.sp)
                                    if (chef.phone.isNotBlank()) {
                                        Text("📞 Tél: ${chef.phone}", fontSize = 12.sp)
                                    }
                                }
                                val currentUserEmail by viewModel.currentUserEmail.collectAsState()
                                if ((currentRole == "CHEF_DE_PROJET" || currentRole == "HEAD") && chef.role != "HEAD" && chef.email != currentUserEmail) {
                                    IconButton(
                                        onClick = { viewModel.deleteUserAccount(chef) },
                                        modifier = Modifier.testTag("delete_chef_btn_${chef.email}")
                                    ) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = ErrorRed)
                                    }
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
fun NotificationsScreen(
    viewModel: InterventionViewModel,
    notifications: List<AppNotification>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Journal des Notifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Flux de notifications reçu pour le profil actif",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (notifications.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllNotifications() }) {
                    Text("Effacer tout")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        "Aucune notification",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aucune notification reçue", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = if (!notif.isRead) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = notif.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Simulé",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarningYellow
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notif.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateInterventionDialog(
    inspectors: List<Inspector>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, List<Int>) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Inspections thermographiques") }
    var factory by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    val selectedInspectors = remember { mutableStateListOf<Int>() }

    val types = listOf(
        "Inspections thermographiques",
        "Audit énergétique",
        "Contrôle de sécurité",
        "Maintenance préventive",
        "Diagnostic de pannes"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Nouvelle Demande d'Intervention",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nom du client") },
                    modifier = Modifier.fillMaxWidth().testTag("input_client_name")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientEmail,
                    onValueChange = { clientEmail = it },
                    label = { Text("Contact Email") },
                    modifier = Modifier.fillMaxWidth().testTag("input_client_email"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Contact Téléphone") },
                    modifier = Modifier.fillMaxWidth().testTag("input_client_phone"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Intervention Type (Manually Entered by Chef de Projet)
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = { selectedType = it },
                    label = { Text("Type d'intervention (Saisie Manuelle)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_intervention_type")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = factory,
                    onValueChange = { factory = it },
                    label = { Text("Usine / Factory") },
                    modifier = Modifier.fillMaxWidth().testTag("input_factory")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zone") },
                    modifier = Modifier.fillMaxWidth().testTag("input_zone")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("L'équipement") },
                    modifier = Modifier.fillMaxWidth().testTag("input_equipment")
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Selector for multiple inspectors
                Text(
                    text = "Sélectionner l'équipe (Plusieurs inspecteurs) :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (inspectors.isEmpty()) {
                    Text("Aucun inspecteur disponible. Veuillez d'abord ajouter un inspecteur.", color = ErrorRed, fontSize = 12.sp)
                } else {
                    inspectors.forEach { ins ->
                        val isChecked = selectedInspectors.contains(ins.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedInspectors.remove(ins.id)
                                    else selectedInspectors.add(ins.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) selectedInspectors.remove(ins.id)
                                    else selectedInspectors.add(ins.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(ins.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(ins.specialty, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (clientName.isNotBlank() && factory.isNotBlank() && selectedInspectors.isNotEmpty()) {
                                onSave(clientName, clientEmail, clientPhone, selectedType, factory, zone, equipment, selectedInspectors.toList())
                            }
                        },
                        enabled = clientName.isNotBlank() && factory.isNotBlank() && selectedInspectors.isNotEmpty(),
                        modifier = Modifier.testTag("save_intervention_btn")
                    ) {
                        Text("Saisir & Enregistrer")
                    }
                }
            }
        }
    }
}

// Dialog for creating inspector
@Composable
fun CreateInspectorDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Ajouter un Inspecteur",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inspector_name")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inspector_email"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inspector_phone"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Spécialité (ex: HVAC, Électricité)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inspector_specialty")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, email, phone, specialty)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("save_inspector_btn")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateChefDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Ajouter un Chef de Projet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth().testTag("input_chef_name")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().testTag("input_chef_email"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth().testTag("input_chef_phone"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank()) {
                                onSave(name, email, phone)
                            }
                        },
                        enabled = name.isNotBlank() && email.isNotBlank(),
                        modifier = Modifier.testTag("save_chef_btn")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: InterventionViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("INSPECTEUR") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isFirebaseInitialized = viewModel.isFirebaseInitialized

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF002737), // Dark OCP Teal Top
                        Color(0xFF00415C)  // OCP Navy Bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Section
            Spacer(modifier = Modifier.height(16.dp))
            OcpLogoMark(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "OCP MAINTENANCE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "SOLUTIONS PORTAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5AC3F1),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sign In / Sign Up tab selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { isSignUp = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isSignUp) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!isSignUp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("tab_login")
                        ) {
                            Text("Connexion", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { isSignUp = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignUp) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSignUp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("tab_signup")
                        ) {
                            Text("Créer Compte", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title Inside Card
                    Text(
                        text = if (isSignUp) "Création de compte" else "Portail d'identification",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Adresse e-mail professionnel") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        modifier = Modifier.fillMaxWidth().testTag("input_auth_email"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_auth_password"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Role Selector during Sign Up
                    if (isSignUp) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sélectionner votre Rôle OCP :",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "HEAD" to "Head (Accès complet / Pilotage)",
                                "CHEF_DE_PROJET" to "Chef de Projet Maintenance",
                                "INSPECTEUR" to "Inspecteur de Terrain / Expert"
                            ).forEach { (roleCode, roleName) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedRole = roleCode }
                                        .background(
                                            color = if (selectedRole == roleCode) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedRole == roleCode,
                                        onClick = { selectedRole = roleCode },
                                        modifier = Modifier.testTag("radio_role_$roleCode")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = roleName,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedRole == roleCode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedRole == roleCode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message
                    if (authError != null) {
                        Text(
                            text = authError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp).testTag("auth_error_text")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Main Submit Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                android.widget.Toast.makeText(context, "Saisissez votre e-mail et mot de passe", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isSignUp) {
                                viewModel.registerWithFirebase(email, password, selectedRole) { success, msg ->
                                    if (success) {
                                        android.widget.Toast.makeText(context, msg ?: "Compte créé avec succès !", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, msg ?: "Échec d'inscription", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                viewModel.loginWithFirebase(email, password) { success, msg ->
                                    if (success) {
                                        android.widget.Toast.makeText(context, msg ?: "Connexion réussie !", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, msg ?: "Échec d'authentification", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_btn"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "S'inscrire" else "Se connecter",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fallback / Fast login Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONNEXION LOCALE RAPIDE (DÉMO)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF5AC3F1),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Utilisez ces profils de test pour vous connecter instantanément et explorer l'application sans configurer de serveur Firebase :",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Profile: HEAD
                        Button(
                            onClick = {
                                viewModel.loginOfflineDirect("head.pilotage@ocp.ma", "HEAD")
                                android.widget.Toast.makeText(context, "Connecté en tant que Head", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D7098),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("fast_login_head"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AdminPanelSettings, "Head", modifier = Modifier.size(16.dp))
                                Text("HEAD", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Profile: CHEF_DE_PROJET
                        Button(
                            onClick = {
                                viewModel.loginOfflineDirect("chef.projet@ocp.ma", "CHEF_DE_PROJET")
                                android.widget.Toast.makeText(context, "Connecté en tant que Chef de Projet", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF135B7E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("fast_login_chef"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BusinessCenter, "Chef", modifier = Modifier.size(16.dp))
                                Text("CHEF PROJET", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Profile: INSPECTEUR
                        Button(
                            onClick = {
                                viewModel.loginOfflineDirect("ahmed.alami@ocp.ma", "INSPECTEUR")
                                android.widget.Toast.makeText(context, "Connecté en tant qu'Inspecteur", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00415C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("fast_login_inspector"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Engineering, "Inspecteur", modifier = Modifier.size(16.dp))
                                Text("INSPECTEUR", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "État SDK Firebase : " + if (isFirebaseInitialized) "✅ Initialisé" else "ℹ️ Non initialisé (Mode Local actif)",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
