package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.*
import com.example.data.repository.ShopRepository
import com.example.domain.AiChatMessage
import com.example.domain.FinancialReport
import com.example.domain.SaleItem
import com.example.domain.ShopProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    val products: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDue: StateFlow<List<CustomerEntity>> = repository.getCustomersWithDue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopProfile = MutableStateFlow(ShopProfile())

    // Theme Mode
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    // Financial reporting
    private val _selectedPeriod = MutableStateFlow("TODAY") // TODAY, MONTH, YEAR, ALL
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _financialReport = MutableStateFlow(FinancialReport())
    val financialReport: StateFlow<FinancialReport> = _financialReport.asStateFlow()

    // AI Chat state
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "aslam_ai",
                message = "Hello Aslam Bhai! I am ASLAM AI, your shop accountant and business manager.\n\nAsk me anything:\n• Today's sales, cash received & net profit\n• Customer udhar & payment reminders\n• Product profit margins & recommended prices\n• Low stock items to reorder"
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    init {
        // Trigger initial report calculation whenever sales, payments or expenses update
        viewModelScope.launch {
            combine(sales, payments, expenses, _selectedPeriod) { _, _, _, period ->
                calculateReport(period)
            }.collect()
        }
    }

    fun setReportPeriod(period: String) {
        _selectedPeriod.value = period
        viewModelScope.launch {
            calculateReport(period)
        }
    }

    private suspend fun calculateReport(period: String) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        val startTime = when (period) {
            "TODAY" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "MONTH" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            "YEAR" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            else -> 0L // ALL
        }

        val label = when (period) {
            "TODAY" -> "Today"
            "MONTH" -> "This Month"
            "YEAR" -> "This Year"
            else -> "All Time"
        }

        try {
            val report = repository.generateFinancialReport(startTime, now, label)
            _financialReport.value = report
        } catch (_: Exception) {}
    }

    // Product actions
    fun addProduct(
        name: String,
        category: String,
        purchasePrice: Double,
        sellingPrice: Double,
        stock: Double,
        unit: String,
        description: String,
        lowStockThreshold: Double = 10.0,
        imageUri: String = ""
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                name = name.trim(),
                category = category.trim().ifEmpty { "General" },
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                stockQuantity = stock,
                unit = unit.trim().ifEmpty { "Packet" },
                description = description.trim(),
                lowStockThreshold = lowStockThreshold,
                imageUri = imageUri
            )
            repository.insertProduct(product)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Customer actions
    fun addCustomer(
        name: String,
        phone: String = "",
        address: String = "",
        notes: String = "",
        points: Int = 0,
        imageUri: String = "",
        onCreated: ((CustomerEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                notes = notes.trim(),
                points = points,
                imageUri = imageUri
            )
            val id = repository.insertCustomer(customer)
            val created = customer.copy(id = id)
            onCreated?.invoke(created)
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // Sales actions
    fun recordSale(
        customerId: Long?,
        customerName: String,
        items: List<SaleItem>,
        totalAmount: Double,
        amountPaid: Double,
        paymentMethod: String,
        notes: String = "",
        onSuccess: (SaleEntity, PaymentEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val (savedSale, savedPayment) = repository.recordSale(
                customerId = customerId,
                customerName = customerName,
                items = items,
                totalAmount = totalAmount,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod,
                notes = notes
            )
            calculateReport(_selectedPeriod.value)
            onSuccess(savedSale, savedPayment)
        }
    }

    // Balance payment actions (Udhar Vasooli)
    fun recordBalancePayment(
        customerId: Long,
        customerName: String,
        amount: Double,
        paymentMethod: String,
        notes: String = "",
        onSuccess: (PaymentEntity) -> Unit
    ) {
        viewModelScope.launch {
            val payment = repository.recordBalancePayment(
                customerId = customerId,
                customerName = customerName,
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            calculateReport(_selectedPeriod.value)
            onSuccess(payment)
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double, notes: String = "") {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                title = title.trim(),
                category = category.trim().ifEmpty { "General" },
                amount = amount,
                notes = notes.trim()
            )
            repository.insertExpense(expense)
            calculateReport(_selectedPeriod.value)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            calculateReport(_selectedPeriod.value)
        }
    }

    fun parseSaleItems(itemsJson: String): List<SaleItem> {
        return repository.parseSaleItems(itemsJson)
    }

    // AI actions
    fun sendAiPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val userMsg = AiChatMessage(sender = "user", message = trimmed)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val report = _financialReport.value
            val currentProducts = products.value
            val currentDues = customersWithDue.value
            val currentShop = shopProfile.value

            val reply = GeminiClient.consultAdvisor(
                userPrompt = trimmed,
                report = report,
                products = currentProducts,
                customersWithDue = currentDues,
                shop = currentShop
            )

            val aiMsg = AiChatMessage(sender = "aslam_ai", message = reply)
            _aiMessages.value = _aiMessages.value + aiMsg
            _isAiThinking.value = false
        }
    }

    fun clearAiChat() {
        _aiMessages.value = listOf(
            AiChatMessage(
                sender = "aslam_ai",
                message = "नमस्ते असलम भाई! चैट रीसेट कर दी गई है। पूछिए क्या हिसाब देखना है?"
            )
        )
    }

    class Factory(private val repository: ShopRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
                return ShopViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
