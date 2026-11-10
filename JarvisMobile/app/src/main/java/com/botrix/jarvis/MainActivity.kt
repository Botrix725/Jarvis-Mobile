
package com.botrix.jarvis

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

private val bg = Color(0xFF050B16)
private val panel = Color(0xFF101D31)
private val cyan = Color(0xFF43D9FF)

private data class ChatLine(
    val fromUser: Boolean,
    val text: String
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        setContent {
            JarvisApp(
                speak = { say(it) },
                launchApp = { launchNamedApp(it) },
                openAccessibility = {
                    startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    )
                },
                openSystemSettings = {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                },
                setVolume = { setPhoneVolume(it) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun say(text: String) {
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis"
        )
    }

    private fun setPhoneVolume(percent: Int) {
        val audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val maxVolume =
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val volume =
            (maxVolume * percent.coerceIn(0, 100) / 100f).toInt()

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            0
        )
    }

    private fun launchNamedApp(raw: String): String {
        val query = raw.lowercase(Locale.ROOT)

        val candidates = when {
            "youtube" in query ->
                listOf("com.google.android.youtube")

            "whatsapp" in query ->
                listOf("com.whatsapp")

            "chrome" in query ->
                listOf(
                    "com.android.chrome",
                    "com.google.android.chrome"
                )

            "settings" in query -> {
                return try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                    "Opening Android Settings."
                } catch (_: Exception) {
                    "Could not open Settings."
                }
            }

            else -> emptyList()
        }

        for (packageName in candidates) {
            val intent =
                packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                startActivity(intent)

                return when {
                    "youtube" in query -> "Opening YouTube."
                    "whatsapp" in query -> "Opening WhatsApp."
                    else -> "Opening Chrome."
                }
            }
        }

        return "I couldn't find that app on this phone."
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisApp(
    speak: (String) -> Unit,
    launchApp: (String) -> String,
    openAccessibility: () -> Unit,
    openSystemSettings: () -> Unit,
    setVolume: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = remember {
        securePrefs(context)
    }

    var provider by remember {
        mutableStateOf(
            prefs.getString("provider", "Gemini") ?: "Gemini"
        )
    }

    var apiKey by remember {
        mutableStateOf(
            prefs.getString("api_key", "") ?: ""
        )
    }

    var configured by remember {
        mutableStateOf(apiKey.isNotBlank())
    }

    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    var status by remember {
        mutableStateOf("SYSTEM ONLINE • AWAITING INPUT")
    }

    var attachment by remember {
        mutableStateOf<Pair<String, String>?>(null)
    }

    val lines = remember {
        mutableStateListOf<ChatLine>()
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            try {
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use {
                        attachment = Pair(
                            uri.lastPathSegment ?: "document",
                            it.readText().take(18000)
                        )

                        status = "FILE LOADED • ${attachment!!.first}"
                    }

            } catch (_: Exception) {
                status =
                    "Could not read this file. Try a text-based TXT file."
            }
        }
    }

    val voice = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val spoken = result.data
            ?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            ?.firstOrNull()

        if (!spoken.isNullOrBlank()) {
            input = spoken
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = bg,
            surface = panel,
            primary = cyan,
            onBackground = Color(0xFFE8F7FF),
            onSurface = Color(0xFFE8F7FF)
        )
    ) {

        if (!configured) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    "J A R V I S",
                    color = cyan,
                    fontSize = 34.sp
                )

                Text(
                    "BOTRIX STUDIOS • MOBILE INTELLIGENCE",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    "INITIAL SYSTEM CONFIGURATION",
                    color = cyan
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Choose your AI provider and enter your API key."
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    FilterChip(
                        selected = provider == "Gemini",
                        onClick = { provider = "Gemini" },
                        label = { Text("Google Gemini") }
                    )

                    Spacer(Modifier.width(8.dp))

                    FilterChip(
                        selected = provider == "OpenAI",
                        onClick = { provider = "OpenAI" },
                        label = { Text("OpenAI") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Select the correct provider for your API key. " +
                    "API usage may incur charges.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (apiKey.isNotBlank()) {

                            prefs.edit()
                                .putString("provider", provider)
                                .putString("api_key", apiKey.trim())
                                .apply()

                            configured = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("INITIALIZE JARVIS")
                }
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(14.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            "J A R V I S",
                            color = cyan,
                            fontSize = 28.sp
                        )

                        Text(
                            "BOTRIX STUDIOS / MOBILE CORE",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        "◉ ONLINE",
                        color = Color(0xFF67F5C2),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(
                            panel,
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            "◉",
                            color = cyan,
                            fontSize = 44.sp
                        )

                        Text(
                            status,
                            color = cyan,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedButton(
                        onClick = openSystemSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SETTINGS", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = openAccessibility,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ENABLE CONTROL", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            picker.launch(
                                arrayOf("text/*", "application/pdf")
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ATTACH FILE", fontSize = 10.sp)
                    }
                }

                attachment?.let {
                    Text(
                        "ATTACHED: ${it.first}",
                        color = cyan,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    if (lines.isEmpty()) {

                        item {
                            Text(
                                "Good evening. I am JARVIS. Ask me anything. " +
                                "Try: Open YouTube, Open WhatsApp, " +
                                "Open Settings, or Set volume to 40 percent.",
                                color = Color.LightGray
                            )
                        }
                    }

                    items(lines) { line ->

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment =
                                if (line.fromUser) Alignment.End
                                else Alignment.Start
                        ) {

                            Text(
                                if (line.fromUser) "YOU" else "JARVIS",
                                color = cyan,
                                fontSize = 9.sp
                            )

                            Text(
                                line.text,
                                color = Color(0xFFE8F7FF),
                                modifier = Modifier
                                    .background(
                                        panel,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {

                            val intent = Intent(
                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                            ).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                            }

                            try {
                                voice.launch(intent)
                            } catch (_: Exception) {
                                status =
                                    "Speech recognition is unavailable."
                            }
                        }
                    ) {
                        Text("🎙")
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask JARVIS...") },
                        maxLines = 3
                    )

                    IconButton(
                        enabled = !busy,
                        onClick = {

                            val prompt = input.trim()

                            if (prompt.isNotEmpty()) {

                                input = ""
                                lines.add(ChatLine(true, prompt))
                                busy = true

                                scope.launch {

                                    val local = handleLocalCommand(
                                        prompt,
                                        launchApp,
                                        setVolume
                                    )

                                    val answer = local ?: runCatching {
                                        AiClient.ask(
                                            provider,
                                            apiKey,
                                            prompt,
                                            attachment?.second
                                        )
                                    }.getOrElse {
                                        "Connection error: " +
                                        "${it.message ?: "Unknown error"}. " +
                                        "Check your API key and internet connection."
                                    }

                                    attachment = null

                                    lines.add(
                                        ChatLine(false, answer)
                                    )

                                    status =
                                        "RESPONSE RECEIVED • SYSTEM READY"

                                    speak(answer)
                                    busy = false
                                }
                            }
                        }
                    ) {
                        Text(
                            "➤",
                            color = cyan
                        )
                    }
                }

                if (busy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    "Provider: $provider • API key stored on this device",
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun securePrefs(
    context: Context
): android.content.SharedPreferences {

    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "jarvis_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

private fun handleLocalCommand(
    text: String,
    launch: (String) -> String,
    volume: (Int) -> Unit
): String? {

    val query = text.lowercase()

    if (
        listOf(
            "open youtube",
            "open chrome",
            "open whatsapp",
            "open settings"
        ).any { query.contains(it) }
    ) {
        return launch(query)
    }

    val match = Regex(
        """(?:volume|sound).*?(\d{1,3})\s*%?"""
    ).find(query)

    if (match != null) {

        val number = match.groupValues[1]
            .toIntOrNull()
            ?.coerceIn(0, 100)
            ?: return null

        volume(number)

        return "Media volume set to approximately $number percent."
    }

    if (
        "notification settings" in query ||
        "alert settings" in query
    ) {
        return "Open Android Settings and choose Notifications " +
            "to manage your alerts."
    }

    return null
}

private object AiClient {

    private val client = okhttp3.OkHttpClient()

    suspend fun ask(
        provider: String,
        key: String,
        prompt: String,
        fileText: String?
    ): String = withContext(Dispatchers.IO) {

        val fullPrompt = if (fileText.isNullOrBlank()) {
            prompt
        } else {
            "$prompt\n\nAttached file text:\n$fileText"
        }

        val json = if (provider == "Gemini") {

            org.json.JSONObject()
                .put(
                    "contents",
                    org.json.JSONArray().put(
                        org.json.JSONObject()
                            .put(
                                "parts",
                                org.json.JSONArray().put(
                                    org.json.JSONObject()
                                        .put("text", fullPrompt)
                                )
                            )
                    )
                )
                .toString()

        } else {

            org.json.JSONObject()
                .put("model", "gpt-4o-mini")
                .put(
                    "messages",
                    org.json.JSONArray().put(
                        org.json.JSONObject()
                            .put("role", "user")
                            .put("content", fullPrompt)
                    )
                )
                .toString()
        }

        val url = if (provider == "Gemini") {

            "https://generativelanguage.googleapis.com/v1beta/" +
                "models/gemini-2.0-flash:generateContent?key=$key"

        } else {

            "https://api.openai.com/v1/chat/completions"
        }

        val requestBody = json.toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val builder = okhttp3.Request.Builder()
            .url(url)
            .post(requestBody)

        if (provider == "OpenAI") {
            builder.addHeader(
                "Authorization",
                "Bearer $key"
            )
        }

        val response = client.newCall(
            builder.build()
        ).execute()

        val body = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw IllegalStateException(
                "API error ${response.code}: ${body.take(500)}"
            )
        }

        val result = org.json.JSONObject(body)

        if (provider == "Gemini") {

            result.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

        } else {

            result.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
