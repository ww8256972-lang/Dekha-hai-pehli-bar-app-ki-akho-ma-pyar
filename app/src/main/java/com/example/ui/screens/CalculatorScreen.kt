package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onNavigateBack: () -> Unit = {}
) {
    var expression by rememberSaveable { mutableStateOf("") }
    var currentInput by rememberSaveable { mutableStateOf("0") }
    var resultText by rememberSaveable { mutableStateOf("") }
    var isResultEvaluated by rememberSaveable { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (isResultEvaluated) {
            currentInput = d
            expression = ""
            resultText = ""
            isResultEvaluated = false
        } else {
            if (currentInput == "0" || currentInput == "Error" || currentInput == "Cannot divide by 0") {
                currentInput = d
            } else {
                currentInput += d
            }
        }
    }

    fun onDecimal() {
        if (isResultEvaluated) {
            currentInput = "0."
            expression = ""
            resultText = ""
            isResultEvaluated = false
        } else {
            if (!currentInput.contains(".")) {
                currentInput += "."
            }
        }
    }

    fun onClear() {
        expression = ""
        currentInput = "0"
        resultText = ""
        isResultEvaluated = false
    }

    fun onBackspace() {
        if (isResultEvaluated) {
            onClear()
        } else if (currentInput.length > 1 && currentInput != "Error" && currentInput != "Cannot divide by 0") {
            currentInput = currentInput.dropLast(1)
        } else {
            currentInput = "0"
        }
    }

    fun onToggleSign() {
        if (currentInput != "0" && currentInput != "Error" && currentInput != "Cannot divide by 0") {
            currentInput = if (currentInput.startsWith("-")) {
                currentInput.removePrefix("-")
            } else {
                "-$currentInput"
            }
        }
    }

    fun evaluateExpression(expr: String): String {
        return try {
            val tokens = tokenize(expr)
            if (tokens.isEmpty()) return ""
            val res = evaluateTokens(tokens)
            formatNumber(res)
        } catch (e: ArithmeticException) {
            "Cannot divide by 0"
        } catch (_: Exception) {
            "Error"
        }
    }

    fun onOperator(op: String) {
        if (currentInput == "Error" || currentInput == "Cannot divide by 0") {
            onClear()
            return
        }
        if (isResultEvaluated) {
            expression = "$currentInput $op "
            currentInput = "0"
            isResultEvaluated = false
            resultText = ""
        } else {
            val trimmedExpr = expression.trim()
            if (currentInput == "0" && trimmedExpr.isNotEmpty() && endsWithOperator(trimmedExpr)) {
                // Replace operator if user taps a different operator
                val withoutLastOp = trimmedExpr.dropLast(1).trim()
                expression = "$withoutLastOp $op "
            } else {
                expression += "$currentInput $op "
                currentInput = "0"
                // Live preview of calculation
                val preview = evaluateExpression(expression.dropLast(3))
                if (preview.isNotBlank() && preview != "Error" && preview != "Cannot divide by 0") {
                    resultText = "= $preview"
                }
            }
        }
    }

    fun onPercent() {
        try {
            val num = BigDecimal(currentInput)
            val percent = num.divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
            currentInput = formatNumber(percent)
        } catch (_: Exception) {}
    }

    fun onEquals() {
        if (currentInput == "Error" || currentInput == "Cannot divide by 0") return
        val fullExpr = if (expression.isNotBlank()) {
            "$expression $currentInput"
        } else {
            currentInput
        }
        val evaluated = evaluateExpression(fullExpr)
        resultText = ""
        expression = fullExpr
        currentInput = evaluated
        isResultEvaluated = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Calculator",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quick Shop & Bahi-Khata Arithmetic",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Calculator Display Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Formula / Running expression
                        val scrollStateExpr = rememberScrollState()
                        Text(
                            text = if (expression.isBlank()) " " else expression,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollStateExpr)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live preview result (if any)
                        if (resultText.isNotBlank()) {
                            Text(
                                text = resultText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Main Large Display
                        val scrollStateInput = rememberScrollState()
                        val isError = currentInput == "Error" || currentInput == "Cannot divide by 0"
                        Text(
                            text = currentInput,
                            fontSize = if (currentInput.length > 9) 32.sp else 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) StatusRed else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollStateInput)
                                .testTag("calculator_display")
                        )
                    }
                }
            }

            // Keypad Grid (4 Columns x 5 Rows)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: AC, ⌫, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "AC",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = StatusRed,
                        onClick = { onClear() }
                    )
                    CalcIconButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onBackspace() }
                    )
                    CalcButton(
                        text = "%",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onPercent() }
                    )
                    CalcButton(
                        text = "÷",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isOperator = true,
                        onClick = { onOperator("÷") }
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigit("7") })
                    CalcButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigit("8") })
                    CalcButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigit("9") })
                    CalcButton(
                        text = "×",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isOperator = true,
                        onClick = { onOperator("×") }
                    )
                }

                // Row 3: 4, 5, 6, −
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigit("4") })
                    CalcButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigit("5") })
                    CalcButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigit("6") })
                    CalcButton(
                        text = "−",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isOperator = true,
                        onClick = { onOperator("−") }
                    )
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigit("1") })
                    CalcButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigit("2") })
                    CalcButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigit("3") })
                    CalcButton(
                        text = "+",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isOperator = true,
                        onClick = { onOperator("+") }
                    )
                }

                // Row 5: ±, 0, ., =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "±",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onToggleSign() }
                    )
                    CalcButton(text = "0", modifier = Modifier.weight(1f), onClick = { onDigit("0") })
                    CalcButton(text = ".", modifier = Modifier.weight(1f), onClick = { onDecimal() })
                    CalcButton(
                        text = "=",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        isPrimary = true,
                        onClick = { onEquals() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isOperator: Boolean = false,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("calc_btn_$text"),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = if (isPrimary) 3.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = if (isOperator || isPrimary) 22.sp else 20.sp,
                fontWeight = if (isPrimary || isOperator) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CalcIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("calc_btn_backspace"),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Math Evaluation Engine with Standard Precedence
private fun endsWithOperator(s: String): Boolean {
    return s.endsWith("+") || s.endsWith("−") || s.endsWith("-") || s.endsWith("×") || s.endsWith("*") || s.endsWith("÷") || s.endsWith("/")
}

private fun tokenize(expr: String): List<String> {
    val tokens = mutableListOf<String>()
    var current = StringBuilder()
    var i = 0
    val cleaned = expr.replace("−", "-").replace("×", "*").replace("÷", "/")
    while (i < cleaned.length) {
        val c = cleaned[i]
        if (c.isWhitespace()) {
            i++
            continue
        }
        if (c == '+' || c == '*' || c == '/') {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current = StringBuilder()
            }
            tokens.add(c.toString())
        } else if (c == '-') {
            // Check if minus is negative sign or subtraction operator
            val isUnary = tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/")
            if (isUnary && current.isEmpty()) {
                current.append(c)
            } else {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current = StringBuilder()
                }
                tokens.add("-")
            }
        } else {
            current.append(c)
        }
        i++
    }
    if (current.isNotEmpty()) {
        tokens.add(current.toString())
    }
    return tokens
}

private fun evaluateTokens(tokens: List<String>): BigDecimal {
    // Shunting-yard or 2-pass precedence evaluation:
    // Pass 1: Handle * and /
    val pass1 = mutableListOf<String>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token == "*" || token == "/") {
            val prev = pass1.removeAt(pass1.size - 1)
            val next = tokens[i + 1]
            val a = BigDecimal(prev)
            val b = BigDecimal(next)
            val res = if (token == "*") {
                a.multiply(b)
            } else {
                if (b.compareTo(BigDecimal.ZERO) == 0) {
                    throw ArithmeticException("Division by zero")
                }
                a.divide(b, 8, RoundingMode.HALF_UP)
            }
            pass1.add(res.stripTrailingZeros().toPlainString())
            i += 2
        } else {
            pass1.add(token)
            i++
        }
    }

    // Pass 2: Handle + and -
    if (pass1.isEmpty()) return BigDecimal.ZERO
    var accumulator = BigDecimal(pass1[0])
    var j = 1
    while (j < pass1.size) {
        val op = pass1[j]
        val next = BigDecimal(pass1[j + 1])
        accumulator = if (op == "+") {
            accumulator.add(next)
        } else {
            accumulator.subtract(next)
        }
        j += 2
    }
    return accumulator
}

private fun formatNumber(bd: BigDecimal): String {
    val stripped = bd.stripTrailingZeros()
    return if (stripped.scale() <= 0) {
        stripped.toBigInteger().toString()
    } else {
        // Limit to max 4 decimal places for clean display
        val scaled = stripped.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        scaled.toPlainString()
    }
}
