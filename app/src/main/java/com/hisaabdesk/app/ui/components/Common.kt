
package com.hisaabdesk.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.core.Money

@Composable
fun MoneyText(poisha: Long, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    val label = Money.formatPoisha(poisha)
    Text(label, modifier = modifier.semantics { contentDescription = label }, style = style)
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun EmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
fun LabeledField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
                 isError: Boolean = false, supporting: String? = null, keyboard: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp), isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard), singleLine = true)
}

fun typeLabelBn(type: String): String = when (type) {
    "INCOME" -> "আয়"; "EXPENSE" -> "ব্যয়"; "TRANSFER_IN", "TRANSFER_OUT" -> "স্থানান্তর"
    "SAVINGS_DEPOSIT" -> "সঞ্চয়ে জমা"; "SAVINGS_WITHDRAW" -> "সঞ্চয় উত্তোলন"
    "LOAN_RECEIVED" -> "ধার গ্রহণ"; "LOAN_GIVEN" -> "ধার প্রদান"
    "DEBT_REPAYMENT_OUT" -> "ধার পরিশোধ"; "DEBT_REPAYMENT_IN" -> "ধার ফেরত প্রাপ্তি"
    "SHOP_PAYMENT" -> "দোকান পেমেন্ট"; "NGO_DISBURSEMENT" -> "এনজিও ঋণ গ্রহণ"; "NGO_INSTALLMENT" -> "এনজিও কিস্তি"
    "BANK_DISBURSEMENT" -> "ব্যাংক ঋণ গ্রহণ"; "BANK_INSTALLMENT" -> "ব্যাংক কিস্তি"; "FEE_INTEREST" -> "সুদ/ফি"
    else -> type
}
