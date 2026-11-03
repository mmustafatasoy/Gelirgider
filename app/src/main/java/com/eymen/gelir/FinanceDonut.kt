package com.eymen.gelir

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.gelir.domain.formatMoney

private val chartIncome = Color(0xFF1EAA59)
private val chartExpense = Color(0xFFF17B61)
private val chartBackground = Color(0xFFEAF2ED)
private val chartMuted = Color(0xFF66786C)
private val chartDark = Color(0xFF153C2B)

/** Donut ratio measures the share of total monthly income+expense movements. */
@Composable
fun CircularIncomeExpenseCard(incomeKurus: Long, expenseKurus: Long) {
    val total = incomeKurus.toDouble() + expenseKurus.toDouble()
    val incomeShare = if (total > 0.0) (incomeKurus.toDouble() / total).toFloat() else 0f
    val animated by animateFloatAsState(
        targetValue = incomeShare,
        animationSpec = tween(durationMillis = 850),
        label = "gelir-gider-halka"
    )
    val ratioIncome = (incomeShare * 100).toInt()
    val ratioExpense = 100 - ratioIncome
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(23.dp)
    ) {
        Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Aylık Para Hareketi", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = chartDark)
                    Text("Gelir ve gider dağılımı", color = chartMuted, fontSize = 12.sp)
                }
                Text("◉", fontSize = 28.sp, color = chartIncome)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(218.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val width = 27.dp.toPx()
                    drawArc(
                        color = chartBackground,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = width, cap = StrokeCap.Butt)
                    )
                    if (total > 0.0) {
                        val amount = animated.coerceIn(0f, 1f)
                        if (incomeKurus > 0) {
                            drawArc(
                                color = chartIncome,
                                startAngle = -90f,
                                sweepAngle = 360f * amount,
                                useCenter = false,
                                style = Stroke(width = width, cap = StrokeCap.Butt)
                            )
                        }
                        if (expenseKurus > 0) {
                            drawArc(
                                color = chartExpense,
                                startAngle = -90f + 360f * amount,
                                sweepAngle = 360f * (1f - amount),
                                useCenter = false,
                                style = Stroke(width = width, cap = StrokeCap.Butt)
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Kalan Bütçe", fontSize = 12.sp, color = chartMuted)
                    Text(
                        formatMoney(incomeKurus - expenseKurus),
                        fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (incomeKurus >= expenseKurus) chartIncome else chartExpense
                    )
                    Text("Gelir − Gider", fontSize = 11.sp, color = chartMuted)
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                LegendTile(
                    title = "GELİR",
                    amount = incomeKurus,
                    percent = if (total == 0.0) 0 else ratioIncome,
                    color = chartIncome,
                    modifier = Modifier.weight(1f)
                )
                LegendTile(
                    title = "GİDER",
                    amount = expenseKurus,
                    percent = if (total == 0.0) 0 else ratioExpense,
                    color = chartExpense,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Yüzdeler, gelir ile giderin toplam para hareketindeki payını gösterir.",
                color = chartMuted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun LegendTile(
    title: String,
    amount: Long,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Text(formatMoney(amount), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(percent.toString() + "%", color = chartMuted, fontSize = 12.sp)
    }
}
