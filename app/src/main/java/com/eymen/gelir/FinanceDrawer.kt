package com.eymen.gelir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Dashboard and planning navigation; keeps the existing bottom tabs available. */
@Composable
fun FinanceDrawerContent(
    activeTab: String,
    username: String,
    onSelect: (String) -> Unit,
    onLock: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("🏡 EYMEN GELİR", color = Color(0xFF14562E), fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp, modifier = Modifier.padding(12.dp))
        Text("👤 " + username + " · Çevrimdışı hesap",
            fontSize = 12.sp, color = Color(0xFF60717B), modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.height(10.dp))
        val groups = listOf(
            "GENEL" to listOf(
                Triple("HOME", "🏠", "Ana Sayfa"),
                Triple("ENTRIES", "🧾", "Gelir & Gider"),
                Triple("TRANSFERS", "💸", "Gönderilen Paralar"),
                Triple("CATEGORIES", "▦", "Kategoriler")
            ),
            "ANALİZ VE BÜTÇE" to listOf(
                Triple("REPORTS", "📊", "Aylık / Yıllık Rapor"),
                Triple("BUDGET", "🎯", "Bütçe Limitleri")
            ),
            "TAKİP VE YEDEK" to listOf(
                Triple("REMINDERS", "🔔", "Ödeme Hatırlatıcıları"),
                Triple("SETTINGS", "⚙️", "Ayarlar / ZIP / Hakkında")
            )
        )
        groups.forEach { (title, tabs) ->
            Text(title, color = Color(0xFF82908B), fontSize = 10.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 4.dp))
            tabs.forEach { (id, icon, label) ->
                NavigationDrawerItem(
                    label = { Text(label, fontSize = 13.sp) },
                    icon = { Text(icon, fontSize = 19.sp) },
                    selected = activeTab == id,
                    onClick = { onSelect(id) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
        TextButton(modifier = Modifier.fillMaxWidth(), onClick = onLock) {
            Text("🔒 Oturumu Kilitle", color = Color(0xFF9F3B3B))
        }
        Text("Atasoy Yazılım  ·  Muhammet Atasoy  ·  v0.5.0",
            fontSize = 10.sp, color = Color(0xFF7C8E82), modifier = Modifier.padding(16.dp))
    }
}
