package com.eymen.gelir.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val mainGreen = Color(0xFF19883F)
private val lightGreen = Color(0xFFE8F7EE)
private val background = Color(0xFFF5FAF7)
private val helperGray = Color(0xFF61766B)

/** Account gate mounts finance screens only after local authentication. */
@Composable
fun LocalAccountGate(
    content: @Composable (username: String, lock: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { LocalAccountStore(context) }
    var authenticated by remember { mutableStateOf(false) }
    var revision by remember { mutableStateOf(0) }
    if (authenticated) {
        content(store.username()) {
            authenticated = false
            revision++
        }
    } else {
        androidx.compose.runtime.key(revision) {
            LocalAuthenticationScreen(
                store = store,
                onAuthenticated = {
                    authenticated = true
                    revision++
                }
            )
        }
    }
}

@Composable
private fun PasswordInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(128)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

@Composable
fun RecoveryCodeSheet(
    codes: List<String>,
    title: String,
    onSaved: () -> Unit
) {
    var acknowledged by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("Bu sekiz kod yalnızca şimdi gösterilir. Her kod tek kullanımlıktır.",
            color = helperGray, fontSize = 13.sp)
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            codes.forEachIndexed { index, code ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}.", fontSize = 12.sp, color = helperGray)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        SelectionContainer {
                            Text(code, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Text("Kopyalarken yalnızca kodu seç. Sıra numarası koda dahil değil.",
            fontSize = 12.sp, color = helperGray)
        Text("Kodları telefon dışında güvenli bir yere yaz. Kod ve şifreler sunucuya gönderilmez.",
            color = helperGray, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
            Text("Kurtarma kodlarımı güvenli bir yere kaydettim.", fontSize = 12.sp)
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = acknowledged,
            onClick = onSaved
        ) { Text("Devam Et") }
    }
}

@Composable
private fun LocalAuthenticationScreen(
    store: LocalAccountStore,
    onAuthenticated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf(if (store.hasAccount()) "LOGIN" else "REGISTER") }
    var user by remember { mutableStateOf(store.username()) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf("") }
    var temporary by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newConfirm by remember { mutableStateOf("") }
    var generatedCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun task(block: suspend () -> Unit) {
        busy = true
        message = ""
        scope.launch {
            try {
                block()
            } catch (err: Exception) {
                message = err.message?.take(140) ?: "İşlem tamamlanamadı."
            } finally {
                busy = false
            }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = mainGreen, background = background)) {
        Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏡", fontSize = 54.sp)
                Text("EYMEN GELİR", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold,
                    color = mainGreen)
                Text("Aile bütçen, sadece senin telefonunda", color = helperGray, fontSize = 13.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (route) {
                            "REGISTER" -> {
                                Text("Yeni hesap oluştur", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("Bu cihaz için tek yerel hesap. İnternet gerekmez.",
                                    color = helperGray, fontSize = 12.sp)
                                OutlinedTextField(
                                    user, { user = it.take(32) },
                                    label = { Text("Kullanıcı adı") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                PasswordInput(password, { password = it }, "Şifre (en az 10 karakter)")
                                PasswordInput(confirm, { confirm = it }, "Şifreyi tekrar gir")
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !busy && user.isNotBlank() && password.length >= 10 &&
                                        confirm == password,
                                    onClick = {
                                        task {
                                            val codes = withContext(Dispatchers.Default) {
                                                store.register(user, password)
                                            }
                                            generatedCodes = codes
                                            password = ""
                                            confirm = ""
                                            route = "CODES"
                                        }
                                    }
                                ) { Text("Kayıt Ol ve Kurtarma Kodlarını Oluştur") }
                            }
                            "CODES" -> RecoveryCodeSheet(
                                codes = generatedCodes,
                                title = "🔑 Kurtarma Kodların",
                                onSaved = {
                                    generatedCodes = emptyList()
                                    onAuthenticated()
                                }
                            )
                            "LOGIN" -> {
                                Text("Güvenli giriş", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                if (store.mustChangeTemporaryPassword()) {
                                    Text("Şifre sıfırlama başlatılmış. Geçici şifren ile yeni şifre belirle.",
                                        color = helperGray, fontSize = 12.sp)
                                    TextButton(onClick = { route = "TEMP" }) { Text("Yeni şifre belirle") }
                                }
                                OutlinedTextField(
                                    user, { user = it.take(32) },
                                    label = { Text("Kullanıcı adı") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                PasswordInput(password, { password = it }, "Şifre")
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !busy && user.isNotBlank() && password.isNotBlank() &&
                                        !store.mustChangeTemporaryPassword(),
                                    onClick = {
                                        task {
                                            val valid = withContext(Dispatchers.Default) {
                                                store.authenticate(user, password)
                                            }
                                            if (valid) {
                                                password = ""
                                                onAuthenticated()
                                            } else {
                                                message = "Kullanıcı adı veya şifre yanlış."
                                            }
                                        }
                                    }
                                ) { Text("Giriş Yap") }
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { route = "RECOVER"; message = ""; password = "" }
                                ) { Text("Şifremi Unuttum · Kurtarma Kodu") }
                            }
                            "RECOVER" -> {
                                Text("Şifreyi kurtar", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("Kayıtta verilen tek kullanımlık kurtarma kodlarından birini gir.",
                                    color = helperGray, fontSize = 13.sp)
                                OutlinedTextField(
                                    user, { user = it.take(32) },
                                    label = { Text("Kullanıcı adı") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    recovery, { recovery = it.take(65) },
                                    label = { Text("Kurtarma kodu") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    enabled = !busy && user.isNotBlank() && recovery.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        task {
                                            val newTemp = withContext(Dispatchers.Default) {
                                                store.redeemRecovery(user, recovery)
                                            }
                                            if (newTemp == null) {
                                                message = "Kullanıcı adı veya kurtarma kodu geçersiz."
                                            } else {
                                                temporary = newTemp
                                                password = newTemp
                                                recovery = ""
                                                route = "TEMP"
                                            }
                                        }
                                    }
                                ) { Text("Kurtarma Kodunu Kullan") }
                                TextButton(onClick = { route = "LOGIN"; message = "" }) { Text("Girişe Dön") }
                            }
                            "TEMP" -> {
                                Text("Yeni kalıcı şifre oluştur", fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp)
                                if (temporary.isNotBlank()) {
                                    Text("Bu geçici şifre yalnızca şimdi gösterilir.", color = helperGray, fontSize = 12.sp)
                                    SelectionContainer {
                                        Text(temporary, fontWeight = FontWeight.Bold, color = mainGreen)
                                    }
                                } else {
                                    Text("Daha önce verilen geçici şifreyi gir. Kaybettiysen başka " +
                                        "bir kullanılmamış kurtarma koduyla yeniden oluştur.",
                                        color = helperGray, fontSize = 12.sp)
                                }
                                PasswordInput(password, { password = it }, "Geçici şifre (otomatik dolduruldu)")
                                HorizontalDivider()
                                PasswordInput(newPassword, { newPassword = it }, "Yeni kalıcı şifre (en az 10)")
                                PasswordInput(newConfirm, { newConfirm = it }, "Yeni şifre tekrar")
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !busy && password.isNotBlank() &&
                                        newPassword.length >= 10 && newPassword == newConfirm,
                                    onClick = {
                                        task {
                                            val success = withContext(Dispatchers.Default) {
                                                store.replaceTemporaryPassword(password, newPassword)
                                            }
                                            if (success) {
                                                password = ""
                                                newPassword = ""
                                                newConfirm = ""
                                                temporary = ""
                                                route = "LOGIN"
                                                message = "Yeni şifren kaydedildi. Şimdi yeni şifrenle giriş yap."
                                            } else {
                                                message = "Geçici şifre yanlış veya yeni şifre geçersiz."
                                            }
                                        }
                                    }
                                ) { Text("Yeni Şifremi Kaydet") }
                                TextButton(onClick = {
                                    temporary = ""
                                    route = "RECOVER"
                                }) { Text("Başka Kurtarma Kodu Kullan") }
                            }
                        }
                        if (message.isNotBlank()) {
                            Text(message, color = if (message.startsWith("Yeni şifren")) mainGreen
                                else MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
                Text("🔒 Yerel hesap kilidi · Atasoy Yazılım · Muhammet Atasoy",
                    color = helperGray, fontSize = 11.sp)
                Text("Not: Bu giriş ekranı veritabanını veya dışa aktarılan ZIP dosyalarını şifrelemez.",
                    color = helperGray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun LocalAccountSettings(store: LocalAccountStore, username: String, onLock: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showing by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var codes by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = lightGreen)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔐 Hesap ve Güvenlik", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Yerel kullanıcı: " + username, fontSize = 13.sp)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showing = true; current = ""; codes = emptyList(); error = "" }
            ) { Text("Yeni Kurtarma Kodları Oluştur") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLock
            ) { Text("🔒 Oturumu Kilitle") }
            Text("Kodları yenilersen önceki kurtarma kodlarının tamamı geçersiz olur.",
                color = helperGray, fontSize = 11.sp)
        }
    }

    if (showing) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!busy) showing = false },
            title = { Text("Kurtarma kodlarını yönet") },
            text = {
                if (codes.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Yeni kodlar oluşturmak için mevcut hesap şifreni gir.",
                            fontSize = 13.sp)
                        PasswordInput(current, { current = it }, "Mevcut şifre", !busy)
                        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    RecoveryCodeSheet(codes, "Yeni Kurtarma Kodların") {
                        codes = emptyList()
                        current = ""
                        showing = false
                    }
                }
            },
            confirmButton = {
                if (codes.isEmpty()) {
                    Button(
                        enabled = !busy && current.isNotEmpty(),
                        onClick = {
                            busy = true
                            error = ""
                            scope.launch {
                                val generated = withContext(Dispatchers.Default) {
                                    store.regenerateRecoveryCodes(current)
                                }
                                busy = false
                                current = ""
                                if (generated != null) codes = generated
                                else error = "Mevcut şifre yanlış."
                            }
                        }
                    ) { Text("Kodları Yenile") }
                }
            },
            dismissButton = {
                if (codes.isEmpty()) {
                    TextButton(onClick = { showing = false }, enabled = !busy) { Text("Vazgeç") }
                }
            }
        )
    }
}
