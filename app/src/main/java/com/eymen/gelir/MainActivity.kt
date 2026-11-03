package com.eymen.gelir

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import com.eymen.gelir.backup.MonthlyBackupScheduler
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eymen.gelir.data.FinanceCategory
import com.eymen.gelir.data.FinanceEntry
import com.eymen.gelir.domain.formatMoney
import com.eymen.gelir.domain.parseTurkishMoney
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val green = Color(0xFF19883F)
private val darkGreen = Color(0xFF125C33)
private val paleGreen = Color(0xFFE7F7EB)
private val red = Color(0xFFE34848)
private val paleRed = Color(0xFFFFEBED)
private val blue = Color(0xFF2475D4)
private val paleBlue = Color(0xFFE9F3FF)
private val gray = Color(0xFF60717B)
private val background = Color(0xFFF6FAF7)
private val turkishLocale = Locale("tr", "TR")
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", turkishLocale)
private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", turkishLocale)
private val dateZone = ZoneId.systemDefault()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EymenApp() }
    }
}

@Composable
private fun EymenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = green,
            onPrimary = Color.White,
            secondary = blue,
            background = background,
            surface = Color.White,
            onSurface = Color(0xFF192A23),
            surfaceContainerLow = Color.White,
            error = red
        ),
        typography = Typography(),
        content = content
    )
}

private fun onMonth(entry: FinanceEntry, month: YearMonth): Boolean =
    YearMonth.from(Instant.ofEpochMilli(entry.occurredAt).atZone(dateZone)) == month

private fun total(entries: List<FinanceEntry>, kind: String): Long =
    entries.filter { it.kind == kind }.sumOf { it.amountKurus }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EymenApp(vm: FinanceViewModel = viewModel()) {
    val entries by vm.entries.collectAsState()
    val categories by vm.categories.collectAsState()
    val update by vm.releaseStatus.collectAsState()
    val operation by vm.operationStatus.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf("HOME") }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var newEntryKind by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<FinanceEntry?>(null) }
    var deleting by remember { mutableStateOf<FinanceEntry?>(null) }
    var notificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.checkForUpdates() }
    LaunchedEffect(operation) {
        operation?.let {
            snackbar.showSnackbar(it)
            vm.clearOperationStatus()
        }
    }

    EymenTheme {
        Scaffold(
            containerColor = background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedBrand()
                            Spacer(Modifier.width(8.dp))
                            Text("EYMEN GELİR", color = darkGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    },
                    actions = {
                        TextButton(onClick = { notificationDialog = true }) {
                            Text(if (update.newVersion) "🔔 ●" else "🔔", fontSize = 21.sp)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (tab == "HOME" || tab == "ENTRIES") {
                    FloatingActionButton(
                        onClick = { editing = null; newEntryKind = "EXPENSE" },
                        containerColor = green,
                        contentColor = Color.White
                    ) { Text("＋", fontSize = 27.sp) }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 3.dp) {
                    listOf(
                        Triple("HOME", "⌂", "Ana Sayfa"),
                        Triple("ENTRIES", "⇄", "İşlemler"),
                        Triple("CATEGORIES", "▦", "Kategoriler"),
                        Triple("REPORTS", "▥", "Raporlar"),
                        Triple("SETTINGS", "☰", "Ayarlar")
                    ).forEach { (id, icon, label) ->
                        NavigationBarItem(
                            selected = tab == id,
                            onClick = { tab = id },
                            icon = { Text(icon, fontSize = 22.sp) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                if (update.newVersion) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { tab = "SETTINGS" },
                        colors = CardDefaults.cardColors(containerColor = paleGreen)
                    ) {
                        Text("🚀 Yeni güncelleme: v${update.version} · Ayrıntılar için dokun", modifier = Modifier.padding(12.dp), color = darkGreen)
                    }
                }
                when (tab) {
                    "HOME" -> HomeScreen(
                        entries, categories, selectedMonth, { selectedMonth = it },
                        onAdd = { editing = null; newEntryKind = it },
                        onEdit = { editing = it }
                    )
                    "ENTRIES" -> EntriesScreen(
                        entries, categories, selectedMonth, { selectedMonth = it },
                        onEdit = { editing = it }, onDelete = { deleting = it }
                    )
                    "CATEGORIES" -> CategoriesScreen(categories, vm)
                    "REPORTS" -> ReportsScreen(entries, selectedMonth, { selectedMonth = it })
                    "SETTINGS" -> SettingsScreen(vm, update, selectedMonth, { selectedMonth = it })
                }
            }
        }

        if (newEntryKind != null || editing != null) {
            EntryDialog(
                kind = editing?.kind ?: newEntryKind ?: "EXPENSE",
                initial = editing,
                categories = categories,
                onDismiss = { editing = null; newEntryKind = null },
                onSave = { kind, cat, money, desc, time, receiptUri, removeReceipt ->
                    vm.addOrUpdate(editing, kind, cat, money, desc, time, receiptUri, removeReceipt)
                    editing = null
                    newEntryKind = null
                }
            )
        }
        deleting?.let { entry ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("İşlem silinsin mi?") },
                text = { Text("${entry.category} · ${formatMoney(entry.amountKurus)} kaydı kalıcı olarak silinecek.") },
                confirmButton = {
                    TextButton(onClick = { vm.delete(entry); deleting = null }) {
                        Text("Sil", color = red)
                    }
                },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text("Vazgeç") } }
            )
        }
        if (notificationDialog) {
            AlertDialog(
                onDismissRequest = { notificationDialog = false },
                title = { Text("🔔 Güncelleme Bildirimi") },
                text = { Text(update.message.ifBlank { "Henüz güncelleme denetimi yapılmadı." }) },
                confirmButton = {
                    TextButton(onClick = { vm.checkForUpdates(); notificationDialog = false }) {
                        Text("Kontrol et")
                    }
                },
                dismissButton = { TextButton(onClick = { notificationDialog = false }) { Text("Kapat") } }
            )
        }
    }
}

@Composable
private fun AnimatedBrand() {
    val motion = rememberInfiniteTransition(label = "brand")
    val angle by motion.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "tilt"
    )
    Text("🏡", modifier = Modifier.graphicsLayer(rotationZ = angle), fontSize = 28.sp)
}

@Composable
private fun MonthSelector(month: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(onClick = { onMonthChange(month.minusMonths(1)) }) { Text("‹", fontSize = 22.sp) }
        Text(month.format(monthFormat).replaceFirstChar { it.uppercase(turkishLocale) }, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = { onMonthChange(month.plusMonths(1)) }) { Text("›", fontSize = 22.sp) }
    }
}

@Composable
private fun SummaryCard(label: String, value: Long, emoji: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = tint)
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 14.dp)) {
            Text(emoji, fontSize = 23.sp)
            Text(label, color = gray, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            Text(formatMoney(value), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun HomeScreen(
    entries: List<FinanceEntry>,
    categories: List<FinanceCategory>,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (FinanceEntry) -> Unit
) {
    val current = remember(entries, month) { entries.filter { onMonth(it, month) } }
    val income = total(current, "INCOME")
    val expense = total(current, "EXPENSE")
    val expenses = current.filter { it.kind == "EXPENSE" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Aile bütçeni kolayca yönet", color = gray, fontSize = 13.sp)
        MonthSelector(month, onMonthChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Gelir", income, "💰", paleGreen, Modifier.weight(1f))
            SummaryCard("Gider", expense, "🛒", paleRed, Modifier.weight(1f))
            SummaryCard("Kalan", income - expense, "💼", paleBlue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(onClick = { onAdd("INCOME") }, modifier = Modifier.weight(1f)) { Text("＋ Gelir Ekle") }
            OutlinedButton(onClick = { onAdd("EXPENSE") }, modifier = Modifier.weight(1f)) { Text("＋ Gider Ekle") }
        }
        Spacer(Modifier.height(20.dp))
        HeaderText("Gider Dağılımı")
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (expenses.isEmpty()) {
                    Text("Bu ay henüz gider kaydı yok.", color = gray)
                } else {
                    val byCategory = expenses.groupBy { it.category }
                        .mapValues { (_, group) -> group.sumOf { it.amountKurus } }
                        .toList().sortedByDescending { it.second }.take(6)
                    byCategory.forEach { (name, amount) ->
                        val emoji = categories.find { it.name == name && it.kind == "EXPENSE" }?.emoji ?: "•"
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Medium)
                                RatioBar(amount.toFloat() / expense.coerceAtLeast(1L).toFloat(), green)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(formatMoney(amount), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        HeaderText("Son İşlemler")
        if (current.isEmpty()) {
            Text("İlk gelir veya gider kaydını ekleyerek başla.", color = gray, modifier = Modifier.padding(12.dp))
        } else {
            current.take(6).forEach { entry ->
                EntryRow(entry, categories, onClick = { onEdit(entry) })
            }
        }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun RatioBar(ratio: Float, color: Color) {
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(CircleShape).background(Color(0xFFEAF0ED))) {
        Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun HeaderText(label: String) {
    Text(label, fontWeight = FontWeight.Bold, color = darkGreen, fontSize = 18.sp, modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
private fun EntryRow(
    entry: FinanceEntry,
    categories: List<FinanceCategory>,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val emoji = categories.find { it.name == entry.category && it.kind == entry.kind }?.emoji
        ?: if (entry.kind == "INCOME") "💰" else "🧾"
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(if (entry.kind == "INCOME") paleGreen else paleRed),
                contentAlignment = Alignment.Center) { Text(emoji, fontSize = 23.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.category, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.description.ifBlank { "Açıklama eklenmedi" },
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = gray, fontSize = 12.sp
                )
                if (entry.receiptFileName != null) {
                    Text("📎 Fiş/fatura ekli", color = blue, fontSize = 11.sp)
                }
                Text(Instant.ofEpochMilli(entry.occurredAt).atZone(dateZone).toLocalDate().format(dayFormat),
                    color = gray, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (entry.kind == "INCOME") "+ " else "− ") + formatMoney(entry.amountKurus),
                    color = if (entry.kind == "INCOME") green else red,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                trailing?.invoke()
            }
        }
    }
}

@Composable
private fun EntriesScreen(
    entries: List<FinanceEntry>,
    categories: List<FinanceCategory>,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onEdit: (FinanceEntry) -> Unit,
    onDelete: (FinanceEntry) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val filtered = entries.filter { onMonth(it, month) && (filter == "ALL" || it.kind == filter) }
    Column {
        HeaderText("Gelir ve Gider İşlemleri")
        MonthSelector(month, onMonthChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "Tümü", "INCOME" to "Gelir", "EXPENSE" to "Gider").forEach { (key, title) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(title) })
            }
        }
        Text("Kayda dokunarak düzenleyebilirsin.", color = gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (filtered.isEmpty()) {
            Text("Seçilen aya ait işlem yok.", color = gray, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    EntryRow(entry, categories, { onEdit(entry) }, trailing = {
                        Text(
                            "Sil", color = red, fontSize = 12.sp,
                            modifier = Modifier.clickable { onDelete(entry) }.padding(top = 5.dp)
                        )
                    })
                }
            }
        }
    }
}

@Composable
private fun CategoriesScreen(categories: List<FinanceCategory>, vm: FinanceViewModel) {
    var kind by rememberSaveable { mutableStateOf("EXPENSE") }
    var showAdd by remember { mutableStateOf(false) }
    val display = categories.filter { it.kind == kind }
    Column {
        HeaderText("Kategori Yönetimi")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = kind == "EXPENSE", onClick = { kind = "EXPENSE" }, label = { Text("Gider Kategorileri") })
            FilterChip(selected = kind == "INCOME", onClick = { kind = "INCOME" }, label = { Text("Gelir Kategorileri") })
        }
        OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Yeni Kategori Ekle") }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)) {
            items(display, key = { it.id }) { category ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.emoji, fontSize = 25.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(category.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        if (category.isDefault) {
                            Text("Varsayılan", fontSize = 11.sp, color = gray)
                        } else {
                            TextButton(onClick = { vm.deleteCategory(category) }) { Text("Sil", color = red) }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("🟢") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Yeni Kategori") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it.take(48) }, label = { Text("Kategori adı") }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(emoji, { emoji = it.take(5) }, label = { Text("Simge (emoji)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { vm.addCategory(name, kind, emoji.ifBlank { "•" }); showAdd = false },
                    enabled = name.isNotBlank()) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
        )
    }
}

@Composable
private fun ReportsScreen(
    entries: List<FinanceEntry>,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    var yearly by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        HeaderText("Gelir – Gider Raporları")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !yearly, onClick = { yearly = false }, label = { Text("Aylık") })
            FilterChip(selected = yearly, onClick = { yearly = true }, label = { Text("Yıllık (12 ay)") })
        }
        if (yearly) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { onMonthChange(month.minusYears(1)) }) { Text("‹") }
                Text("${month.year} Yılı", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedButton(onClick = { onMonthChange(month.plusYears(1)) }) { Text("›") }
            }
            val annual = entries.filter {
                Instant.ofEpochMilli(it.occurredAt).atZone(dateZone).year == month.year
            }
            val income = total(annual, "INCOME")
            val expense = total(annual, "EXPENSE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("Yıllık Gelir", income, "💰", paleGreen, Modifier.weight(1f))
                SummaryCard("Yıllık Gider", expense, "🛒", paleRed, Modifier.weight(1f))
                SummaryCard("Yıllık Kalan", income - expense, "💼", paleBlue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            HeaderText("12 Aylık Karşılaştırma")
            (1..12).forEach { number ->
                val ym = YearMonth.of(month.year, number)
                val perMonth = annual.filter { onMonth(it, ym) }
                val i = total(perMonth, "INCOME")
                val e = total(perMonth, "EXPENSE")
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(ym.format(DateTimeFormatter.ofPattern("MMMM", turkishLocale))
                            .replaceFirstChar { it.uppercase(turkishLocale) }, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Gelir: ${formatMoney(i)}", color = green, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("Gider: ${formatMoney(e)}", color = red, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                        val max = maxOf(i, e, 1L).toFloat()
                        RatioBar(i / max, green)
                        RatioBar(e / max, red)
                        Text("Kalan: ${formatMoney(i - e)}", fontSize = 12.sp, color = gray, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        } else {
            MonthSelector(month, onMonthChange)
            val current = entries.filter { onMonth(it, month) }
            val income = total(current, "INCOME")
            val expense = total(current, "EXPENSE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("Gelir", income, "💰", paleGreen, Modifier.weight(1f))
                SummaryCard("Gider", expense, "🛒", paleRed, Modifier.weight(1f))
                SummaryCard("Kalan", income - expense, "💼", paleBlue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            HeaderText("Kategori Bazında Giderler")
            val breakdown = current.filter { it.kind == "EXPENSE" }.groupBy { it.category }
            if (breakdown.isEmpty()) {
                Text("Bu ay kayıtlı gider bulunmuyor.", color = gray)
            }
            breakdown.forEach { (name, values) ->
                val amount = values.sumOf { it.amountKurus }
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, fontWeight = FontWeight.Medium)
                            Text(formatMoney(amount), fontWeight = FontWeight.SemiBold)
                        }
                        RatioBar(amount.toFloat() / expense.coerceAtLeast(1L).toFloat(), blue)
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}


@Composable
private fun SettingsScreen(
    vm: FinanceViewModel,
    update: ReleaseStatus,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    val context = LocalContext.current
    val pending by vm.pendingImport.collectAsState()
    val busy by vm.backupBusy.collectAsState()
    var autoFolder by remember { mutableStateOf(MonthlyBackupScheduler.folder(context)) }
    var autoError by remember { mutableStateOf<String?>(null) }
    val pickBackupFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { picked ->
        if (picked != null) {
            try {
                MonthlyBackupScheduler.enable(context, picked)
                autoFolder = picked
                autoError = null
            } catch (error: Exception) {
                autoError = "Klasör izni alınamadı. Başka bir klasör seç."
            }
        }
    }

    val saveZip = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) vm.exportMonth(uri, month) }

    val openZip = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.inspectBackup(uri) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeaderText("Ayarlar ve Hakkında")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) {
                Text("🏡 EYMEN GELİR", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = darkGreen)
                Text("Aile Bütçesi ve Gelir – Gider Takip Programı", color = gray, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                InfoRow("Yazılım sürümü", "v${BuildConfig.VERSION_NAME}")
                InfoRow("Geliştirici", "Muhammet Atasoy")
                InfoRow("Yayıncı", "Atasoy Yazılım")
                InfoRow("Veri konumu", "Telefon (çevrimdışı)")
                InfoRow("Lisans", "MIT")
            }
        }
        Spacer(Modifier.height(16.dp))
        HeaderText("💾 Yedekleme Merkezi")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text("Aylık ZIP arşivi", fontWeight = FontWeight.Bold, color = darkGreen)
                Text("Yedeklemek istediğin ayı seç. ZIP dosyasını telefonunda istediğin klasöre kaydedebilirsin.",
                    fontSize = 13.sp, color = gray)
                MonthSelector(month, onMonthChange)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = {
                        val filename = "EymenGelir_${month.year}_${month.monthValue.toString().padStart(2, '0')}.zip"
                        saveZip.launch(filename)
                    }
                ) { Text("📦 Seçilen Ayı ZIP Olarak Kaydet") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = { openZip.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) }
                ) { Text("↩ ZIP Dosyasından Geri Yükle") }
                if (busy) Text("ZIP işleniyor...", color = blue, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Geri yükleme mevcut kayıtları silmez. Aynı işlem ikinci kez içe aktarılmaz. " +
                        "ZIP dosyaları varsayılan olarak şifresizdir; özel finans verilerinizi güvenli bir yerde saklayın.",
                    color = gray, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
                )
                Text("Otomatik aylık yedekleme henüz yok; bu sürümde yedek elle alınır.",
                    color = red, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(16.dp))
        HeaderText("🗓 Otomatik Aylık Yedekleme")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Her ay otomatik ZIP", fontWeight = FontWeight.Bold)
                        Text(if (autoFolder != null) "Açık" else "Kapalı", color = gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoFolder != null,
                        onCheckedChange = { enabled ->
                            if (enabled) pickBackupFolder.launch(null)
                            else {
                                MonthlyBackupScheduler.disable(context)
                                autoFolder = null
                            }
                        }
                    )
                }
                Text(
                    "Android'in izin verdiği zamanda, önceki tamamlanmış ayın verilerini " +
                        "seçtiğin klasöre otomatik ZIP olarak kaydeder.",
                    fontSize = 12.sp, color = gray
                )
                if (autoFolder != null) {
                    Text(
                        "Son yedek: " + MonthlyBackupScheduler.lastCompletedMonth(context),
                        color = darkGreen, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedButton(onClick = { pickBackupFolder.launch(null) }) {
                        Text("Yedek klasörünü değiştir")
                    }
                }
                if (autoError != null) {
                    Text(autoError.orEmpty(), color = red, fontSize = 12.sp)
                }
                Text(
                    "Otomatik yedekler şifreli değildir. Yalnızca izin verdiğin klasör kullanılır.",
                    color = gray, fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HeaderText("🔔 Güncelleme Bildirimi")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = paleGreen)) {
            Column(Modifier.padding(18.dp)) {
                Text(update.message.ifBlank { "Sürüm bilgisi kontrol edilmeye hazır." }, color = darkGreen)
                if (update.newVersion) {
                    Text("Yeni sürüm: v${update.version}", fontWeight = FontWeight.Bold, color = darkGreen)
                    if (update.releaseNotes.isNotBlank()) {
                        Text(update.releaseNotes.take(450), fontSize = 12.sp, color = gray)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                    }, enabled = update.releaseUrl.isNotBlank()) { Text("GitHub'dan güncellemeyi aç") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.checkForUpdates() }, enabled = !update.isChecking) {
                    Text(if (update.isChecking) "Denetleniyor..." else "⟳ Güncellemeleri kontrol et")
                }
                Text("Güncelleme yalnızca resmî GitHub Releases sayfasından yapılır. Otomatik APK kurulumu yoktur.",
                    color = gray, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    pending?.let { info ->
        AlertDialog(
            onDismissRequest = { vm.cancelPendingImport() },
            title = { Text("ZIP yedeğini geri yükle?") },
            text = {
                Text(
                    "Ay: ${info.snapshot.month}\n" +
                        "Kayıt sayısı: ${info.snapshot.entries.size}\n" +
                        "Kategori sayısı: ${info.snapshot.categories.size}\n\n" +
                        "Mevcut kayıtların silinmeyecek. Aynı kimliğe sahip kayıtlar atlanacak."
                )
            },
            confirmButton = {
                Button(onClick = { vm.restorePendingBackup() }) { Text("Geri Yükle") }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelPendingImport() }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
    HorizontalDivider(color = Color(0xFFF0F3F1))
}

@Composable
private fun EntryDialog(
    kind: String,
    initial: FinanceEntry?,
    categories: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, Long, Uri?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var actualKind by remember(initial?.id) { mutableStateOf(kind) }
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.let { "${it.amountKurus / 100},${(it.amountKurus % 100).toString().padStart(2, '0')}" } ?: "")
    }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var occurredAt by remember(initial?.id) { mutableStateOf(initial?.occurredAt ?: System.currentTimeMillis()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var selectedPhoto by remember(initial?.id) { mutableStateOf<Uri?>(null) }
    var removeExistingPhoto by remember(initial?.id) { mutableStateOf(false) }
    val choosePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { selected ->
        if (selected != null) {
            selectedPhoto = selected
            removeExistingPhoto = false
        }
    }
    val existingPhoto = remember(initial?.receiptFileName) {
        initial?.receiptFileName?.let { name ->
            val file = File(File(context.filesDir, "receipts"), name)
            if (file.isFile) {
                BitmapFactory.decodeFile(
                    file.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = 4 }
                )?.asImageBitmap()
            } else null
        }
    }
    val options = categories.filter { it.kind == actualKind }
    val currentCategory = category.takeIf { chosen -> options.any { it.name == chosen } }
        ?: options.firstOrNull()?.name.orEmpty()
    val parsed = parseTurkishMoney(amount)
    val date = Instant.ofEpochMilli(occurredAt).atZone(dateZone).toLocalDate()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Yeni İşlem Ekle" else "İşlemi Düzenle") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = actualKind == "INCOME", onClick = {
                        actualKind = "INCOME"; category = ""
                    }, label = { Text("💰 Gelir") })
                    FilterChip(selected = actualKind == "EXPENSE", onClick = {
                        actualKind = "EXPENSE"; category = ""
                    }, label = { Text("🛒 Gider") })
                }
                Spacer(Modifier.height(8.dp))
                Text("Kategori *", fontWeight = FontWeight.SemiBold)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(options.find { it.name == currentCategory }?.emoji ?: "•")
                        Spacer(Modifier.width(8.dp))
                        Text(currentCategory.ifBlank { "Kategori seç" }, modifier = Modifier.weight(1f))
                        Text("⌄")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.emoji}  ${option.name}") },
                                onClick = { category = option.name; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    amount, { amount = it.take(24) },
                    label = { Text("Tutar (₺) *") },
                    placeholder = { Text("Örn: 1.250,75") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotBlank() && parsed == null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    description, { description = it.take(500) },
                    label = { Text("Açıklama (ne için?)") },
                    placeholder = { Text("Örn: Kasım ayı elektrik faturası") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Fiş / Fatura Fotoğrafı (isteğe bağlı)", fontWeight = FontWeight.SemiBold)
                if (existingPhoto != null && selectedPhoto == null && !removeExistingPhoto) {
                    Image(
                        bitmap = existingPhoto,
                        contentDescription = "Kayıtlı fiş fotoğrafı",
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (selectedPhoto != null) {
                    Text("📷 Yeni fotoğraf seçildi; kaydedilince arşive eklenecek.",
                        color = blue, fontSize = 12.sp)
                } else if (initial?.receiptFileName != null && !removeExistingPhoto) {
                    Text("📎 Fotoğraf kayıtlı", color = green, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { choosePhoto.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (initial?.receiptFileName != null) "📷 Fotoğrafı Değiştir" else "📷 Fiş/Fatura Ekle") }
                if (selectedPhoto != null || (initial?.receiptFileName != null && !removeExistingPhoto)) {
                    TextButton(onClick = {
                        selectedPhoto = null
                        removeExistingPhoto = initial?.receiptFileName != null
                    }) { Text("Fotoğrafı Kaldır", color = red) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                occurredAt = LocalDate.of(year, month + 1, day)
                                    .atStartOfDay(dateZone).toInstant().toEpochMilli()
                            },
                            date.year, date.monthValue - 1, date.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📅 ${date.format(dayFormat)}") }
            }
        },
        confirmButton = {
            Button(
                enabled = parsed != null && currentCategory.isNotBlank(),
                onClick = { if (parsed != null) onSave(actualKind, currentCategory, parsed, description, occurredAt, selectedPhoto, removeExistingPhoto) }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
