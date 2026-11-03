package com.eymen.gelir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.gelir.data.FinanceEntry
import com.eymen.gelir.domain.formatMoney
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val transfersGreen = Color(0xFF147D42)
private val transferGray = Color(0xFF586E62)

/** Transfer history remains part of the expense ledger and monthly/yearly reports. */
@Composable
fun TransfersScreen(
    entries: List<FinanceEntry>,
    onNewTransfer: () -> Unit,
    onEdit: (FinanceEntry) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val allTransfers = entries.filter {
        it.kind == "EXPENSE" && it.category == "Gönderilen Para"
    }
    val visible = allTransfers.filter {
        search.isBlank() ||
            it.recipientName.orEmpty().contains(search.trim(), ignoreCase = true) ||
            it.description.contains(search.trim(), ignoreCase = true)
    }
    val grouped = visible.groupBy { it.recipientName?.takeIf(String::isNotBlank) ?: "Alıcı belirtilmedi" }
        .mapValues { (_, records) -> records.sumOf { it.amountKurus } }
        .toList().sortedByDescending { it.second }
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("tr", "TR"))
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("💸 Gönderilen Paralar", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = transfersGreen)
        Text("Kimlere, ne zaman ve hangi açıklamayla para gönderdin?",
            color = transferGray, fontSize = 12.sp)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7ED))
        ) {
            Column(Modifier.padding(15.dp)) {
                Text("Toplam gönderilen", color = transferGray, fontSize = 12.sp)
                Text(formatMoney(allTransfers.sumOf { it.amountKurus }),
                    fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = transfersGreen)
                Text(allTransfers.size.toString() + " gönderim kaydı", fontSize = 12.sp, color = transferGray)
            }
        }
        TextButton(onClick = onNewTransfer, modifier = Modifier.fillMaxWidth()) {
            Text("＋ Yeni Para Gönderimi Kaydet")
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it.take(100) },
            label = { Text("Alıcı adı veya açıklama ara") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Text("Alıcıya göre toplamlar", fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        if (grouped.isEmpty()) {
            Text("Henüz gönderilen para kaydı yok.", color = transferGray)
        }
        grouped.forEach { (person, total) ->
            Card(Modifier.fillMaxWidth().padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👤 " + person, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Text(formatMoney(total), color = transfersGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Gönderim geçmişi", fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 15.dp, bottom = 8.dp))
        visible.forEach { transfer ->
            Card(Modifier.fillMaxWidth().padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(13.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("👤 " + transfer.recipientName.orEmpty().ifBlank { "Alıcı belirtilmedi" },
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(formatMoney(transfer.amountKurus), color = Color(0xFFC64B48),
                            fontWeight = FontWeight.Bold)
                    }
                    Text(transfer.description.ifBlank { "Açıklama yok" },
                        color = transferGray, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Instant.ofEpochMilli(transfer.occurredAt)
                            .atZone(ZoneId.systemDefault()).format(dateFormat),
                            color = transferGray, fontSize = 12.sp)
                        TextButton(onClick = { onEdit(transfer) }) { Text("Düzenle") }
                    }
                }
            }
        }
        Spacer(Modifier.padding(bottom = 60.dp))
    }
}
