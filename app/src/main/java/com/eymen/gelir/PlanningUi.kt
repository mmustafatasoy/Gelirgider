package com.eymen.gelir

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.gelir.data.FinanceCategory
import com.eymen.gelir.data.FinanceEntry
import com.eymen.gelir.data.BudgetLimit
import com.eymen.gelir.data.PaymentReminder
import com.eymen.gelir.domain.BudgetRules
import com.eymen.gelir.domain.ReminderRules
import com.eymen.gelir.domain.formatMoney
import com.eymen.gelir.domain.parseTurkishMoney
import com.eymen.gelir.reminders.ReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val planGreen = Color(0xFF19883F)
private val planRed = Color(0xFFDE4848)
private val planAmber = Color(0xFFAF6912)
private val planBlue = Color(0xFF2475D4)
private val planGray = Color(0xFF60717B)
private val planSurface = Color.White

@Composable
fun BudgetAlerts(entries: List<FinanceEntry>, budgets: List<BudgetLimit>, month: YearMonth) {
    val alerts = budgets.mapNotNull { limit ->
        val spent = BudgetRules.spentInMonth(entries, limit.category, month)
        val ratio = BudgetRules.ratio(spent, limit.monthlyLimitKurus)
        if (ratio >= 0.8f) Triple(limit.category, spent, ratio) else null
    }
    if (alerts.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E4))
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("⚠️ Aylık bütçe uyarıları", fontWeight = FontWeight.Bold, color = planAmber)
                alerts.take(4).forEach { (category, spent, ratio) ->
                    Text(
                        category + ": " + formatMoney(spent) + " · " +
                            (ratio * 100).toInt().toString() + "% kullanıldı",
                        fontSize = 12.sp,
                        color = if (ratio >= 1f) planRed else planAmber
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetPlanner(
    vm: FinanceViewModel,
    entries: List<FinanceEntry>,
    categories: List<FinanceCategory>,
    month: YearMonth
) {
    val budgets by vm.budgetLimits.collectAsState()
    var editing by remember { mutableStateOf<BudgetLimit?>(null) }
    var showingDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎯 Aylık Kategori Bütçeleri", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = { editing = null; showingDialog = true }) { Text("＋ Limit Ekle") }
        }
        Text(
            "Bu limitler her ay yeniden uygulanır. Harcamalar kaydedildikçe çubuklar güncellenir.",
            fontSize = 12.sp, color = planGray
        )
        if (budgets.isEmpty()) {
            Text("Henüz bütçe limiti belirlenmedi.", modifier = Modifier.padding(vertical = 12.dp), color = planGray)
        }
        budgets.forEach { limit ->
            val spent = BudgetRules.spentInMonth(entries, limit.category, month)
            val ratio = BudgetRules.ratio(spent, limit.monthlyLimitKurus)
            val color = when {
                ratio >= 1f -> planRed
                ratio >= 0.8f -> planAmber
                else -> planGreen
            }
            Card(
                Modifier.fillMaxWidth().padding(top = 7.dp),
                colors = CardDefaults.cardColors(containerColor = planSurface)
            ) {
                Column(Modifier.padding(13.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(limit.category, fontWeight = FontWeight.Bold)
                        Text(BudgetRules.status(spent, limit.monthlyLimitKurus), color = color, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        formatMoney(spent) + " / " + formatMoney(limit.monthlyLimitKurus),
                        fontSize = 13.sp, color = planGray
                    )
                    PlanProgress(ratio, color)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editing = limit; showingDialog = true }) { Text("Düzenle") }
                        TextButton(onClick = { vm.deleteMonthlyBudget(limit.category) }) {
                            Text("Kaldır", color = planRed)
                        }
                    }
                }
            }
        }
    }

    if (showingDialog) {
        val options = categories.filter { it.kind == "EXPENSE" }
        var selected by remember(editing?.category) {
            mutableStateOf(editing?.category ?: options.firstOrNull()?.name.orEmpty())
        }
        var rawAmount by remember(editing?.category) {
            mutableStateOf(editing?.monthlyLimitKurus?.let {
                (it / 100).toString() + "," + (it % 100).toString().padStart(2, '0')
            } ?: "")
        }
        var expanded by remember { mutableStateOf(false) }
        val parsed = parseTurkishMoney(rawAmount)
        AlertDialog(
            onDismissRequest = { showingDialog = false },
            title = { Text(if (editing == null) "Yeni Aylık Bütçe" else "Bütçe Limitini Düzenle") },
            text = {
                Column {
                    Text("Gider kategorisi", fontWeight = FontWeight.Medium)
                    Box {
                        OutlinedButton(onClick = { if (editing == null) expanded = true }, enabled = editing == null) {
                            Text(selected.ifBlank { "Kategori seç" })
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach {
                                DropdownMenuItem(text = { Text(it.emoji + " " + it.name) },
                                    onClick = { selected = it.name; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = rawAmount,
                        onValueChange = { rawAmount = it.take(24) },
                        label = { Text("Aylık limit (₺)") },
                        placeholder = { Text("Örn: 5.000,00") },
                        singleLine = true,
                        isError = rawAmount.isNotBlank() && parsed == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = selected.isNotBlank() && parsed != null,
                    onClick = {
                        if (parsed != null) vm.setMonthlyBudget(selected, parsed)
                        showingDialog = false
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showingDialog = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun PlanProgress(value: Float, color: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(9.dp)
            .background(Color(0xFFECF2ED), RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(9.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun MonthlyWeeklyTrend(entries: List<FinanceEntry>, month: YearMonth) {
    val filtered = entries.filter {
        YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault())) == month
    }
    val periods = (0..4).map { week ->
        val start = week * 7 + 1
        val end = minOf(start + 6, month.lengthOfMonth())
        val group = filtered.filter {
            val date = Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault()).dayOfMonth
            date in start..end
        }
        Triple(start.toString() + "–" + end, group.filter { it.kind == "INCOME" }.sumOf { it.amountKurus },
            group.filter { it.kind == "EXPENSE" }.sumOf { it.amountKurus })
    }.filter { it.first.substringBefore("–").toInt() <= month.lengthOfMonth() }
    val max = periods.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1L) ?: 1L

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text("📊 Haftalık Gelir–Gider Karşılaştırması", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Yeşil: Gelir   ·   Kırmızı: Gider", fontSize = 11.sp, color = planGray)
        periods.forEach { (name, income, expense) ->
            Card(
                Modifier.fillMaxWidth().padding(top = 7.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(name + " " + month.month.name.lowercase() + " · G: " +
                        formatMoney(income) + " / H: " + formatMoney(expense),
                        fontSize = 11.sp, color = planGray)
                    PlanProgress(income.toFloat() / max.toFloat(), planGreen)
                    PlanProgress(expense.toFloat() / max.toFloat(), planRed)
                }
            }
        }
    }
}

@Composable
fun PaymentReminderPanel(vm: FinanceViewModel) {
    val context = LocalContext.current
    val categories by vm.categories.collectAsState()
    val reminders by vm.paymentReminders.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (it) ReminderScheduler.checkNow(context)
    }

    Column(Modifier.fillMaxWidth()) {
        Text("🔔 Fatura ve Aidat Hatırlatıcıları",
            fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text("Ödeme gününü belirle; her ay tekrar hatırlatılır. Bildirim günü Android tarafından geciktirilebilir.",
            color = planGray, fontSize = 12.sp)
        if (!permissionGranted) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E4))) {
                Column(Modifier.padding(12.dp)) {
                    Text("Bildirim izni kapalı. Hatırlatıcılar kaydedilir ancak telefon bildirimi gönderilemez.",
                        fontSize = 12.sp, color = planAmber)
                    Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Bildirim İzni Ver")
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("＋ Ödeme Hatırlatıcısı Ekle") }
        if (reminders.isEmpty()) {
            Text("Henüz hatırlatıcı eklenmedi.", color = planGray, modifier = Modifier.padding(10.dp))
        }
        val today = LocalDate.now()
        reminders.forEach { reminder ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(reminder.category + " · " + reminder.title, fontWeight = FontWeight.SemiBold)
                            val due = ReminderRules.dueDate(YearMonth.from(today), reminder.dayOfMonth)
                            val days = ReminderRules.daysUntil(today, reminder.dayOfMonth)
                            Text("Her ay " + reminder.dayOfMonth + ". gün" +
                                " (bu ay " + due.dayOfMonth + ") · " +
                                if (days >= 0) days.toString() + " gün kaldı" else "Bu ayın günü geçti",
                                fontSize = 11.sp, color = planGray)
                        }
                        Switch(
                            checked = reminder.enabled,
                            onCheckedChange = { vm.setPaymentReminderEnabled(reminder, it) }
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.deletePaymentReminder(reminder) }) {
                            Text("Sil", color = planRed)
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        val defaults = listOf("Elektrik", "Su", "Doğalgaz", "Bina Aidatı")
        val options = categories.filter { it.kind == "EXPENSE" }.sortedBy {
            val rank = defaults.indexOf(it.name)
            if (rank == -1) 999 else rank
        }
        var category by remember { mutableStateOf(options.firstOrNull()?.name.orEmpty()) }
        var title by remember { mutableStateOf("") }
        var day by remember { mutableStateOf("15") }
        var expanded by remember { mutableStateOf(false) }
        val dateValid = day.toIntOrNull()?.let { it in 1..31 } ?: false
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Ödeme Hatırlatıcısı") },
            text = {
                Column {
                    Text("Kategori", fontWeight = FontWeight.Medium)
                    Box {
                        OutlinedButton(onClick = { expanded = true }) { Text(category.ifBlank { "Kategori seç" }) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach {
                                DropdownMenuItem(text = { Text(it.emoji + " " + it.name) },
                                    onClick = { category = it.name; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = title, onValueChange = { title = it.take(80) },
                        label = { Text("Açıklama") },
                        placeholder = { Text("Örn: Ev elektrik faturası") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text("Ödeme günü (1–31)") },
                        isError = day.isNotBlank() && !dateValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "31 seçilirse kısa aylarda son gün kullanılır.",
                        fontSize = 11.sp, color = planGray
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = category.isNotBlank() && title.isNotBlank() && dateValid,
                    onClick = {
                        val number = day.toIntOrNull()
                        if (number != null) vm.addPaymentReminder(category, title, number)
                        showAdd = false
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
        )
    }
}
