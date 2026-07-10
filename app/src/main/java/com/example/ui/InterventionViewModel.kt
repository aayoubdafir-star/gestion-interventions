package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.AppNotification
import com.example.data.model.Inspector
import com.example.data.model.Intervention
import com.example.data.model.UserAccount
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InterventionViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // Active User Role simulation
    val roles = listOf("CHEF_DE_PROJET", "HEAD", "INSPECTEUR")
    private val _currentRole = MutableStateFlow("HEAD") // Default to HEAD, gets updated after login/switch
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _selectedInspectorId = MutableStateFlow<Int?>(null) // Which inspector is "logged in"
    val selectedInspectorId: StateFlow<Int?> = _selectedInspectorId.asStateFlow()

    // Virtual Clock (Simulating time elapsed in minutes)
    private val _simulatedTimeMinutes = MutableStateFlow(0)
    val simulatedTimeMinutes: StateFlow<Int> = _simulatedTimeMinutes.asStateFlow()

    // UI flows from Repository
    val inspectors: StateFlow<List<Inspector>> = repository.allInspectors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interventions: StateFlow<List<Intervention>> = repository.allInterventions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAccounts: StateFlow<List<UserAccount>> = repository.allUserAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered notifications depending on selected role & inspector ID
    val filteredNotifications: StateFlow<List<AppNotification>> = combine(
        notifications,
        currentRole,
        selectedInspectorId
    ) { allNotifs, role, insId ->
        allNotifs.filter { notif ->
            when (role) {
                "CHEF_DE_PROJET" -> notif.recipientRole == "CHEF_DE_PROJET" || notif.recipientRole == "ALL"
                "HEAD" -> true // HEAD sees all notifications
                "INSPECTEUR" -> {
                    notif.recipientRole == "INSPECTEUR" && 
                    (notif.recipientInspectorId == null || notif.recipientInspectorId == insId)
                }
                else -> false
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Firebase Auth States
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    var isFirebaseInitialized = false
        private set

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null

    init {
        // Safe Firebase initialization
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firebaseFirestore = FirebaseFirestore.getInstance()
            isFirebaseInitialized = true

            val currentUser = firebaseAuth?.currentUser
            if (currentUser != null) {
                _currentUserEmail.value = currentUser.email
                _isAuthenticating.value = true
                viewModelScope.launch {
                    try {
                        firebaseFirestore?.collection("users")?.document(currentUser.uid)
                            ?.get()
                            ?.addOnSuccessListener { document ->
                                val role = document.getString("role") ?: "INSPECTEUR"
                                _currentRole.value = role
                                _currentUserEmail.value = currentUser.email
                                if (role == "INSPECTEUR" && currentUser.email != null) {
                                    resolveInspectorProfile(currentUser.email!!)
                                }
                                _isLoggedIn.value = true
                                _isAuthenticating.value = false
                            }
                            ?.addOnFailureListener {
                                val fallbackRole = if (currentUser.email?.contains("admin") == true || currentUser.email?.contains("head") == true) "HEAD" else "INSPECTEUR"
                                _currentRole.value = fallbackRole
                                _isLoggedIn.value = true
                                _isAuthenticating.value = false
                            }
                    } catch (e: Exception) {
                        _isLoggedIn.value = true
                        _isAuthenticating.value = false
                    }
                }
            }
        } catch (e: Exception) {
            isFirebaseInitialized = false
        }

        // Clear preloading block. Database starts empty so that Chef de Projet can invite inspectors manually.
        viewModelScope.launch {
            try {
                // Initialize default accounts for quick local logins
                val existingAccounts = repository.allUserAccounts.first()
                if (existingAccounts.isEmpty()) {
                    repository.insertUserAccount(UserAccount("head.pilotage@ocp.ma", "Head Pilotage", "HEAD", "0612345670"))
                    repository.insertUserAccount(UserAccount("chef.projet@ocp.ma", "Chef Projet", "CHEF_DE_PROJET", "0612345671"))
                    repository.insertUserAccount(UserAccount("ahmed.alami@ocp.ma", "Ahmed Alami", "INSPECTEUR", "0612345678", "Vibratoire & Thermographie"))
                }

                val mockEmails = listOf(
                    "ahmed.alami@ocp.ma",
                    "yassine.bennani@ocp.ma",
                    "sanaa.tazi@ocp.ma",
                    "omar.faridi@ocp.ma",
                    "amine.chraibi@ocp.ma"
                )
                val currentList = repository.allInspectors.first()
                currentList.filter { it.email in mockEmails }.forEach { ins ->
                    repository.deleteInspector(ins)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun resolveInspectorProfile(email: String) {
        viewModelScope.launch {
            val list = repository.allInspectors.first()
            val existing = list.find { it.email.equals(email, ignoreCase = true) }
            if (existing != null) {
                _selectedInspectorId.value = existing.id
            } else {
                val name = email.substringBefore("@").replace(".", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val id = repository.insertInspector(
                    Inspector(
                        name = name,
                        email = email,
                        phone = "0612345678",
                        specialty = "Contrôle Non Destructif (Vibratoire / Thermographie)"
                    )
                )
                _selectedInspectorId.value = id.toInt()
            }
        }
    }

    fun registerWithFirebase(email: String, password: String, role: String, onResult: (Boolean, String?) -> Unit) {
        _authError.value = null
        _isAuthenticating.value = true

        viewModelScope.launch {
            val localAccount = repository.getUserAccountByEmail(email)
            val finalRole = localAccount?.role ?: role

            if (!isFirebaseInitialized || firebaseAuth == null) {
                // Local fallback signup if Firebase isn't initialized
                _currentRole.value = finalRole
                _currentUserEmail.value = email
                if (finalRole == "INSPECTEUR") {
                    resolveInspectorProfile(email)
                }
                // Save it locally if not already there
                if (localAccount == null) {
                    val fallbackName = email.substringBefore("@").replace(".", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    repository.insertUserAccount(
                        UserAccount(email = email, name = fallbackName, role = role)
                    )
                }
                _isLoggedIn.value = true
                _isAuthenticating.value = false
                onResult(true, "Compte créé en mode démonstration local (Firebase non initialisé)")
                return@launch
            }

            firebaseAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        val fallbackName = email.substringBefore("@").replace(".", " ")
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        val finalName = localAccount?.name ?: fallbackName

                        val userData = hashMapOf(
                            "uid" to user.uid,
                            "email" to email,
                            "role" to finalRole,
                            "name" to finalName,
                            "createdAt" to System.currentTimeMillis()
                        )

                        firebaseFirestore?.collection("users")?.document(user.uid)
                            ?.set(userData)
                            ?.addOnSuccessListener {
                                _currentRole.value = finalRole
                                _currentUserEmail.value = email
                                if (finalRole == "INSPECTEUR") {
                                    resolveInspectorProfile(email)
                                }
                                _isLoggedIn.value = true
                                _isAuthenticating.value = false
                                onResult(true, null)
                            }
                            ?.addOnFailureListener { e ->
                                _currentRole.value = finalRole
                                _currentUserEmail.value = email
                                if (finalRole == "INSPECTEUR") {
                                    resolveInspectorProfile(email)
                                }
                                _isLoggedIn.value = true
                                _isAuthenticating.value = false
                                onResult(true, "Compte Firebase Auth créé mais erreur Firestore: ${e.localizedMessage}")
                            }
                    } else {
                        _isAuthenticating.value = false
                        _authError.value = "Erreur inattendue lors de la création."
                        onResult(false, "Utilisateur nul.")
                    }
                }
                .addOnFailureListener { exception ->
                    _isAuthenticating.value = false
                    _authError.value = exception.localizedMessage
                    onResult(false, exception.localizedMessage)
                }
        }
    }

    fun loginWithFirebase(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        _authError.value = null
        _isAuthenticating.value = true

        if (!isFirebaseInitialized || firebaseAuth == null) {
            // Local fallback login
            viewModelScope.launch {
                val localAccount = repository.getUserAccountByEmail(email)
                val inferredRole = localAccount?.role ?: when {
                    email.contains("head", ignoreCase = true) || email.contains("admin", ignoreCase = true) -> "HEAD"
                    email.contains("chef", ignoreCase = true) || email.contains("projet", ignoreCase = true) -> "CHEF_DE_PROJET"
                    else -> "INSPECTEUR"
                }
                _currentRole.value = inferredRole
                _currentUserEmail.value = email
                if (inferredRole == "INSPECTEUR") {
                    resolveInspectorProfile(email)
                }
                _isLoggedIn.value = true
                _isAuthenticating.value = false
                onResult(true, "Connecté en mode démonstration local (Firebase non initialisé)")
            }
            return
        }

        firebaseAuth!!.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    firebaseFirestore?.collection("users")?.document(user.uid)
                        ?.get()
                        ?.addOnSuccessListener { document ->
                            val role = document.getString("role")
                            if (role != null) {
                                _currentRole.value = role
                                _currentUserEmail.value = email
                                if (role == "INSPECTEUR") {
                                    resolveInspectorProfile(email)
                                }
                                _isLoggedIn.value = true
                                _isAuthenticating.value = false
                                onResult(true, null)
                            } else {
                                // Fallback to Room DB user account mapping
                                viewModelScope.launch {
                                    val localAccount = repository.getUserAccountByEmail(email)
                                    val finalRole = localAccount?.role ?: "INSPECTEUR"
                                    _currentRole.value = finalRole
                                    _currentUserEmail.value = email
                                    if (finalRole == "INSPECTEUR") {
                                        resolveInspectorProfile(email)
                                    }
                                    _isLoggedIn.value = true
                                    _isAuthenticating.value = false
                                    onResult(true, null)
                                }
                            }
                        }
                        ?.addOnFailureListener { e ->
                            viewModelScope.launch {
                                val localAccount = repository.getUserAccountByEmail(email)
                                val fallbackRole = localAccount?.role ?: when {
                                    email.contains("head", ignoreCase = true) || email.contains("admin", ignoreCase = true) -> "HEAD"
                                    email.contains("chef", ignoreCase = true) -> "CHEF_DE_PROJET"
                                    else -> "INSPECTEUR"
                                }
                                _currentRole.value = fallbackRole
                                _currentUserEmail.value = email
                                if (fallbackRole == "INSPECTEUR") {
                                    resolveInspectorProfile(email)
                                }
                                _isLoggedIn.value = true
                                _isAuthenticating.value = false
                                onResult(true, "Connecté avec succès. Erreur de lecture Firestore: ${e.localizedMessage}")
                            }
                        }
                } else {
                    _isAuthenticating.value = false
                    _authError.value = "Erreur d'authentification."
                    onResult(false, "Utilisateur nul.")
                }
            }
            .addOnFailureListener { exception ->
                _isAuthenticating.value = false
                _authError.value = exception.localizedMessage
                onResult(false, exception.localizedMessage)
            }
    }

    fun loginOfflineDirect(email: String, role: String) {
        viewModelScope.launch {
            val localAccount = repository.getUserAccountByEmail(email)
            val finalRole = localAccount?.role ?: role
            _currentRole.value = finalRole
            _currentUserEmail.value = email
            if (finalRole == "INSPECTEUR") {
                resolveInspectorProfile(email)
            }
            _isLoggedIn.value = true
        }
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {}
        _isLoggedIn.value = false
        _currentUserEmail.value = null
        _selectedInspectorId.value = null
    }

    fun setRole(role: String) {
        _currentRole.value = role
    }

    fun setSelectedInspectorId(id: Int?) {
        _selectedInspectorId.value = id
    }

    // --- ACTIONS ---

    // 1. Manage Inspectors (Invited by Chef de Projet)
    fun addInspector(name: String, email: String, phone: String, specialty: String) {
        viewModelScope.launch {
            val id = repository.insertInspector(
                Inspector(name = name, email = email, phone = phone, specialty = specialty)
            )
            // Also save as local UserAccount for automatic authentication/role determination
            repository.insertUserAccount(
                UserAccount(
                    email = email,
                    name = name,
                    role = "INSPECTEUR",
                    phone = phone,
                    specialty = specialty
                )
            )
            // Create invitation notifications (simulated email transmission)
            repository.insertNotification(
                AppNotification(
                    recipientRole = "CHEF_DE_PROJET",
                    title = "E-mail d'invitation transmis 📧",
                    message = "Un e-mail d'invitation avec instructions d'accès a été envoyé avec succès à l'inspecteur '$name' à l'adresse '$email'."
                )
            )
            repository.insertNotification(
                AppNotification(
                    recipientRole = "INSPECTEUR",
                    recipientInspectorId = id.toInt(),
                    title = "Bienvenue dans l'équipe OCP 📧",
                    message = "Bonjour $name, votre compte a été créé par invitation e-mail. Vous pouvez maintenant configurer vos spécialités et saisir vos informations professionnelles."
                )
            )
        }
    }

    // 1b. Manage Chefs de Projet (Invited by other Chef de Projet)
    fun addChefDeProjet(name: String, email: String, phone: String) {
        viewModelScope.launch {
            repository.insertUserAccount(
                UserAccount(
                    email = email,
                    name = name,
                    role = "CHEF_DE_PROJET",
                    phone = phone
                )
            )
            repository.insertNotification(
                AppNotification(
                    recipientRole = "CHEF_DE_PROJET",
                    title = "Nouveau Chef de Projet ajouté 📋",
                    message = "Le Chef de Projet '$name' ($email) a été invité et peut se connecter maintenant."
                )
            )
            repository.insertNotification(
                AppNotification(
                    recipientRole = "HEAD",
                    title = "Nouveau Chef de Projet ajouté 📋",
                    message = "Le Chef de Projet '$name' ($email) a été invité par un autre Chef de Projet."
                )
            )
        }
    }

    fun deleteUserAccount(userAccount: UserAccount) {
        viewModelScope.launch {
            repository.deleteUserAccount(userAccount)
            // If it's an inspector, keep database in sync
            if (userAccount.role == "INSPECTEUR") {
                val list = repository.allInspectors.first()
                val ins = list.find { it.email.equals(userAccount.email, ignoreCase = true) }
                if (ins != null) {
                    repository.deleteInspector(ins)
                    if (selectedInspectorId.value == ins.id) {
                        _selectedInspectorId.value = null
                    }
                }
            }
        }
    }

    fun updateInspector(inspector: Inspector) {
        viewModelScope.launch {
            repository.insertInspector(inspector) // OnConflictStrategy.REPLACE handles update
            // Sync with local UserAccount as well
            val existingAcc = repository.getUserAccountByEmail(inspector.email)
            if (existingAcc != null) {
                repository.insertUserAccount(
                    existingAcc.copy(
                        name = inspector.name,
                        phone = inspector.phone,
                        specialty = inspector.specialty
                    )
                )
            }
            repository.insertNotification(
                AppNotification(
                    recipientRole = "CHEF_DE_PROJET",
                    title = "Profil Inspecteur mis à jour",
                    message = "L'inspecteur '${inspector.name}' a mis à jour ses informations de profil (Spécialité : ${inspector.specialty}, Tél : ${inspector.phone})."
                )
            )
        }
    }

    fun deleteInspector(inspector: Inspector) {
        viewModelScope.launch {
            repository.deleteInspector(inspector)
            // Also delete corresponding local user account
            val acc = repository.getUserAccountByEmail(inspector.email)
            if (acc != null) {
                repository.deleteUserAccount(acc)
            }
            // If the deleted inspector was logged in, reset selection
            if (selectedInspectorId.value == inspector.id) {
                _selectedInspectorId.value = null
            }
        }
    }

    // 2. Interventions Management
    fun createIntervention(
        clientName: String,
        clientEmail: String,
        clientPhone: String,
        interventionType: String,
        factory: String,
        zone: String,
        equipment: String,
        selectedInspectorIds: List<Int>
    ) {
        viewModelScope.launch {
            val csv = selectedInspectorIds.joinToString(",")
            val intervention = Intervention(
                clientName = clientName,
                clientContactEmail = clientEmail,
                clientContactPhone = clientPhone,
                interventionType = interventionType,
                factory = factory,
                zone = zone,
                equipment = equipment,
                assignedInspectorIdsCsv = csv,
                status = "CREE"
            )
            val interventionId = repository.insertIntervention(intervention)

            // Notify each chosen inspector
            selectedInspectorIds.forEach { inspectorId ->
                repository.insertNotification(
                    AppNotification(
                        recipientRole = "INSPECTEUR",
                        recipientInspectorId = inspectorId,
                        title = "Nouvelle mission assignée",
                        message = "Vous avez été choisi par le chef de projet pour l'intervention [ID: $interventionId] chez le client '$clientName' ($interventionType)."
                    )
                )
            }
        }
    }

    // 3. Inspector Workflow progression
    fun updateInterventionStatusByInspector(
        intervention: Intervention,
        newStatus: String,
        remark: String? = null
    ) {
        viewModelScope.launch {
            val updated = intervention.copy(
                status = newStatus,
                remarkIfNotDone = if (newStatus == "PAS_FAIT") remark else null,
                lastStatusUpdateTime = System.currentTimeMillis() - (_simulatedTimeMinutes.value * 60000L) // Anchor to simulated time
            )
            repository.updateIntervention(updated)

            // Notifications
            when (newStatus) {
                "ACCEPTE" -> {
                    repository.insertNotification(
                        AppNotification(
                            recipientRole = "CHEF_DE_PROJET",
                            title = "Intervention prise en charge",
                            message = "L'intervention [ID: ${intervention.id}] chez '${intervention.clientName}' a été acceptée par l'inspecteur."
                        )
                    )
                }
                "EN_COURS" -> {
                    // Start working
                }
                "PAS_FAIT" -> {
                    repository.insertNotification(
                        AppNotification(
                            recipientRole = "CHEF_DE_PROJET",
                            title = "Intervention non effectuée",
                            message = "L'intervention [ID: ${intervention.id}] chez '${intervention.clientName}' est marquée 'Non Faite' avec la remarque: \"$remark\"."
                        )
                    )
                }
                "TERMINE" -> {
                    // Ready to fill report
                }
            }
        }
    }

    // 4. Update Report Status by Inspector
    fun updateReportStatusByInspector(
        intervention: Intervention,
        newReportStatus: String
    ) {
        viewModelScope.launch {
            val updated = intervention.copy(
                reportStatus = newReportStatus,
                lastReportUpdateTime = System.currentTimeMillis() - (_simulatedTimeMinutes.value * 60000L)
            )
            repository.updateIntervention(updated)

            if (newReportStatus == "FAIT") {
                // Notify Chef de Projet to validate
                repository.insertNotification(
                    AppNotification(
                        recipientRole = "CHEF_DE_PROJET",
                        title = "Rapport à valider",
                        message = "L'inspecteur a rédigé le rapport pour '${intervention.clientName}'. Veuillez le valider."
                    )
                )
            }
        }
    }

    // 5. Validate Report by Chef de Projet
    fun validateReport(intervention: Intervention, isApproved: Boolean) {
        viewModelScope.launch {
            if (isApproved) {
                val updated = intervention.copy(reportValidated = true)
                repository.updateIntervention(updated)

                // Notify inspectors to send the report
                intervention.assignedInspectorIds.forEach { inspectorId ->
                    repository.insertNotification(
                        AppNotification(
                            recipientRole = "INSPECTEUR",
                            recipientInspectorId = inspectorId,
                            title = "Rapport validé",
                            message = "Votre rapport pour '${intervention.clientName}' a été validé ! Veuillez l'envoyer au client et confirmer l'envoi."
                        )
                    )
                }
            }
        }
    }

    // 6. Confirm Report Sent to Client by Inspector
    fun confirmReportSent(intervention: Intervention) {
        viewModelScope.launch {
            val updated = intervention.copy(reportSentToClient = true)
            repository.updateIntervention(updated)

            // Notify Chef de Projet to close
            repository.insertNotification(
                AppNotification(
                    recipientRole = "CHEF_DE_PROJET",
                    title = "Rapport envoyé au client",
                    message = "L'inspecteur a envoyé le rapport pour '${intervention.clientName}' au client. Vous pouvez maintenant clôturer l'intervention."
                )
            )
        }
    }

    // 7. Cloture Intervention by Chef de Projet with Motivational Message
    fun closeIntervention(intervention: Intervention, motivationalMessage: String) {
        viewModelScope.launch {
            val updated = intervention.copy(
                isClosed = true,
                closeMessage = motivationalMessage
            )
            repository.updateIntervention(updated)

            // Notify inspectors with motivational message
            intervention.assignedInspectorIds.forEach { inspectorId ->
                repository.insertNotification(
                    AppNotification(
                        recipientRole = "INSPECTEUR",
                        recipientInspectorId = inspectorId,
                        title = "Intervention clôturée 🎉",
                        message = "L'intervention pour '${intervention.clientName}' est clôturée. Message du chef de projet : \"$motivationalMessage\""
                    )
                )
            }
        }
    }

    // 8. Clear Notifications
    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    // --- TIME SIMULATOR & RELANCES ENGINE ---
    // Every time we advance time, we evaluate if any reminders (relances) need to be sent!
    // Status is checked against the updated simulated time.
    fun advanceTime(minutesToAdd: Int) {
        val previousTime = _simulatedTimeMinutes.value
        val newTime = previousTime + minutesToAdd
        _simulatedTimeMinutes.value = newTime

        viewModelScope.launch {
            // Retrieve interventions list directly from StateFlow value
            val list = interventions.value
            list.forEach { intervention ->
                if (intervention.isClosed) return@forEach

                // 1. Relance for Intervention in progress (every 30 minutes)
                // Triggered if status is "EN_COURS" and time elapsed since lastStatusUpdateTime is multiple of 30
                if (intervention.status == "EN_COURS") {
                    val minutesElapsed = newTime - getMinutesFromTimestamp(intervention.lastStatusUpdateTime)
                    // Let's see how many 30-minute intervals have passed
                    val previouslyElapsed = previousTime - getMinutesFromTimestamp(intervention.lastStatusUpdateTime)
                    val prevIntervals = previouslyElapsed / 30
                    val currentIntervals = minutesElapsed / 30

                    if (currentIntervals > prevIntervals && currentIntervals > 0) {
                        // Relance to both Project Manager & Inspectors!
                        repository.insertNotification(
                            AppNotification(
                                recipientRole = "CHEF_DE_PROJET",
                                title = "Relance (30min): Intervention En Cours",
                                message = "L'intervention [ID: ${intervention.id}] pour '${intervention.clientName}' est toujours en cours depuis ${currentIntervals * 30} minutes."
                            )
                        )
                        intervention.assignedInspectorIds.forEach { inspectorId ->
                            repository.insertNotification(
                                AppNotification(
                                    recipientRole = "INSPECTEUR",
                                    recipientInspectorId = inspectorId,
                                    title = "Relance (30min): Intervention En Cours",
                                    message = "Votre intervention [ID: ${intervention.id}] pour '${intervention.clientName}' est en cours depuis ${currentIntervals * 30} minutes. Pensez à la finaliser dès que possible !"
                                )
                            )
                        }
                    }
                }

                // 2. Relance for Report in progress (every 2 hours / 120 minutes)
                // Triggered if reportStatus is "EN_COURS_REDACTION" and time elapsed is multiple of 120
                if (intervention.reportStatus == "EN_COURS_REDACTION") {
                    val minutesElapsed = newTime - getMinutesFromTimestamp(intervention.lastReportUpdateTime)
                    val previouslyElapsed = previousTime - getMinutesFromTimestamp(intervention.lastReportUpdateTime)
                    val prevIntervals = previouslyElapsed / 120
                    val currentIntervals = minutesElapsed / 120

                    if (currentIntervals > prevIntervals && currentIntervals > 0) {
                        // Relance to Inspectors!
                        intervention.assignedInspectorIds.forEach { inspectorId ->
                            repository.insertNotification(
                                AppNotification(
                                    recipientRole = "INSPECTEUR",
                                    recipientInspectorId = inspectorId,
                                    title = "Relance (2h): Rapport en cours de rédaction",
                                    message = "Le rapport de l'intervention pour '${intervention.clientName}' est en cours de rédaction depuis ${currentIntervals * 2} heures."
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetTimeSimulation() {
        _simulatedTimeMinutes.value = 0
    }

    private fun getMinutesFromTimestamp(timestamp: Long): Int {
        // Simple conversion to virtual offset minutes relative to start time of intervention
        val deltaMs = System.currentTimeMillis() - timestamp
        return (deltaMs / 60000L).toInt()
    }
}

class InterventionViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InterventionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InterventionViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
