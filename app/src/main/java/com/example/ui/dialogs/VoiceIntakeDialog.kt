package com.example.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.api.GeminiAudioService
import com.example.data.api.VoiceParsedData
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VoiceIntakeDialog(
    geminiAudioService: GeminiAudioService,
    onDismiss: () -> Unit,
    onApplyParsedData: (VoiceParsedData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isGuidedMode by remember { mutableStateOf(true) } // Default to step-by-step for absolute accuracy
    var guidedStep by remember { mutableStateOf(0) } // 0: Name, 1: Phone, 2: Device, 3: Issue, 4: Cost

    var isListening by remember { mutableStateOf(false) }
    var spokenLiveText by remember { mutableStateOf("") }
    var isGeminiProcessing by remember { mutableStateOf(false) }
    var parsedResult by remember { mutableStateOf<VoiceParsedData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editable fields for review before applying
    var editCustomerName by remember { mutableStateOf("") }
    var editCustomerPhone by remember { mutableStateOf("") }
    var editDeviceName by remember { mutableStateOf("") }
    var editIssueDesc by remember { mutableStateOf("") }
    var editCost by remember { mutableStateOf("") }

    val steps = listOf(
        Triple("اسم العميل", "تحدث الآن باسم العميل بالكامل...", "مثل: محمد أحمد"),
        Triple("رقم الهاتف", "تحدث الآن برقم هاتف العميل...", "مثل: 01025588334"),
        Triple("نوع الجهاز", "تحدث الآن بنوع وموديل الجهاز بدقة...", "مثل: ايفون 13 برو ماكس"),
        Triple("وصف العطل", "تحدث الآن بوصف العطل أو مشكلة الهاتف...", "مثل: شاشة مكسورة ولا يشحن"),
        Triple("التكلفة التقديرية", "تحدث الآن بالتكلفة التقديرية لعملية الصيانة...", "مثل: 1200 جنيه")
    )

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    fun startListening() {
        errorMessage = null
        if (speechRecognizer == null) {
            errorMessage = "التعرف الصوتي غير مدعوم على هذا الجهاز"
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                spokenLiveText = "جاري الاستماع... تفضل بالتحدث"
            }
            override fun onBeginningOfSpeech() {
                isListening = true
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                val desc = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت"
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت. الرجاء التحدث بوضوح وإعادة المحاولة."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت مهلة الاستماع."
                    else -> "تعذر الاستماع (رمز $error)"
                }
                if (spokenLiveText.isBlank() || spokenLiveText.startsWith("جاري")) {
                    errorMessage = desc
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val fullText = matches?.firstOrNull() ?: spokenLiveText
                if (fullText.isNotBlank() && !fullText.startsWith("جاري")) {
                    spokenLiveText = fullText
                    
                    if (isGuidedMode) {
                        // Guided step-by-step logic
                        val textWithEnglishDigits = convertArabicDigits(fullText)
                        when (guidedStep) {
                            0 -> {
                                editCustomerName = fullText
                            }
                            1 -> {
                                editCustomerPhone = cleanSpokenPhoneNumber(textWithEnglishDigits)
                            }
                            2 -> {
                                editDeviceName = fullText
                            }
                            3 -> {
                                editIssueDesc = fullText
                            }
                            4 -> {
                                editCost = cleanSpokenCost(textWithEnglishDigits)
                            }
                        }
                        // Create dummy parsedResult to enable fields preview
                        parsedResult = VoiceParsedData(
                            customerName = editCustomerName,
                            customerPhone = editCustomerPhone,
                            deviceName = editDeviceName,
                            issueDescription = editIssueDesc,
                            estimatedCost = editCost,
                            fullTranscription = "إدخال موجه خطوة بخطوة"
                        )
                    } else {
                        // Free-form intelligent processing
                        scope.launch {
                            isGeminiProcessing = true
                            val res = geminiAudioService.parseSpokenTextWithGemini(fullText)
                            isGeminiProcessing = false
                            res.onSuccess { data ->
                                parsedResult = data
                                editCustomerName = data.customerName
                                editCustomerPhone = data.customerPhone
                                editDeviceName = data.deviceName
                                editIssueDesc = data.issueDescription.ifBlank { fullText }
                                editCost = data.estimatedCost
                            }.onFailure { err ->
                                errorMessage = err.message
                            }
                        }
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    spokenLiveText = text
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
            isListening = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            errorMessage = "يلزم منح إذن الميكروفون للتعرف الصوتي"
        }
    }

    fun checkPermissionAndListen() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Mic Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("voice_intake_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "الإدخال الصوتي الاحترافي",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "سجل بيانات كارت الصيانة بصوتك بكل سهولة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Slate400)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Mode Selector Tabs (Guided vs Freeform)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isGuidedMode) EmeraldPrimary else Color.Transparent)
                            .clickable {
                                isGuidedMode = true
                                spokenLiveText = ""
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "خطوة بخطوة (دقة 100%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGuidedMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isGuidedMode) EmeraldPrimary else Color.Transparent)
                            .clickable {
                                isGuidedMode = false
                                spokenLiveText = ""
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "إدخال حر (ذكاء اصطناعي)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isGuidedMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Step progress indicators (Guided Mode Only)
                if (isGuidedMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        steps.forEachIndexed { index, pair ->
                            val isActive = index == guidedStep
                            val isCompleted = index < guidedStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isActive -> EmeraldPrimary
                                            isCompleted -> EmeraldPrimary.copy(alpha = 0.6f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }

                    // Guided Step Guide Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.05f))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الخطوة ${guidedStep + 1} من 5: " + steps[guidedStep].first,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = steps[guidedStep].second,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "نصيحة: " + steps[guidedStep].third,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Navigation arrows inside the step
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (guidedStep > 0) {
                                        guidedStep--
                                        spokenLiveText = ""
                                    }
                                },
                                enabled = guidedStep > 0,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("السابق", fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    if (guidedStep < 4) {
                                        guidedStep++
                                        spokenLiveText = ""
                                    }
                                },
                                enabled = guidedStep < 4,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("تخطي الخطوة", fontSize = 10.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Pulsing Mic Circle Button
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (isListening) RedAccent.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.15f))
                        .clickable {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                checkPermissionAndListen()
                            }
                        }
                        .testTag("record_voice_intake_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isListening) RedAccent else EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "سجل",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Status text
                Text(
                    text = when {
                        isListening -> "جاري الاستماع... تحدّث بوضوح الآن"
                        isGeminiProcessing -> "جاري التحليل والتفريغ بذكاء Gemini..."
                        spokenLiveText.isNotBlank() -> "تم الالتقاط! تابع التسجيل أو قم بتعديل الحقول بالأسفل"
                        isGuidedMode -> "اضغط على الميكروفون وانطق: " + steps[guidedStep].first
                        else -> "اضغط وانطق بيانات الهاتف كاملة ليقوم الذكاء الاصطناعي بتصنيفها"
                    },
                    fontSize = 12.sp,
                    color = if (isListening) RedAccent else if (isGeminiProcessing) StatusBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                // Quick Example Hint Box (Free-form Only)
                if (!isGuidedMode && parsedResult == null && !isGeminiProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusBlue.copy(alpha = 0.08f))
                            .border(1.dp, StatusBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "💡 مثال لما يمكنك قوله في جملة واحدة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusBlue
                            )
                            Text(
                                text = "«استلمت ايفون 13 برو ماكس من العميل محمود حسن رقمه 01023456789 الشاشة طافية والتكلفة التقديرية 1200 جنيه»",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Error Banner
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedAccent.copy(alpha = 0.1f))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedAccent, modifier = Modifier.size(16.dp))
                            Text(text = errorMessage ?: "", fontSize = 11.sp, color = RedAccent)
                        }
                    }
                }

                // Live Spoken Transcription Box
                if (spokenLiveText.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "النص الصوتي الملتَقط حاليًا:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(text = spokenLiveText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Loading Indicator for Gemini
                if (isGeminiProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        Text("جاري استخراج الحقول الذكية بواسطة Gemini...", fontSize = 11.sp, color = EmeraldPrimary)
                    }
                }

                // Extracted Results & Preview Form (Always visible for editing and review)
                if (parsedResult != null || isGuidedMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StatusEmerald.copy(alpha = 0.03f))
                            .border(1.dp, StatusEmerald.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                            Text(
                                text = "مراجعة وتعديل بيانات كارت الاستلام:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusEmerald
                            )
                        }

                        // Customer Name Field
                        val isNameActive = isGuidedMode && guidedStep == 0
                        OutlinedTextField(
                            value = editCustomerName,
                            onValueChange = { editCustomerName = it },
                            label = { Text("اسم العميل" + if (isNameActive) " 🌟 (نشط)" else "") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isNameActive) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (isNameActive) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Customer Phone Field
                        val isPhoneActive = isGuidedMode && guidedStep == 1
                        OutlinedTextField(
                            value = editCustomerPhone,
                            onValueChange = { editCustomerPhone = it },
                            label = { Text("رقم الهاتف" + if (isPhoneActive) " 🌟 (نشط)" else "") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isPhoneActive) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (isPhoneActive) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Device Name Field
                        val isDeviceActive = isGuidedMode && guidedStep == 2
                        OutlinedTextField(
                            value = editDeviceName,
                            onValueChange = { editDeviceName = it },
                            label = { Text("نوع وموديل الجهاز" + if (isDeviceActive) " 🌟 (نشط)" else "") },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDeviceActive) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (isDeviceActive) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Issue Description Field
                        val isIssueActive = isGuidedMode && guidedStep == 3
                        OutlinedTextField(
                            value = editIssueDesc,
                            onValueChange = { editIssueDesc = it },
                            label = { Text("وصف العطل" + if (isIssueActive) " 🌟 (نشط)" else "") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isIssueActive) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (isIssueActive) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Estimated Cost Field
                        val isCostActive = isGuidedMode && guidedStep == 4
                        OutlinedTextField(
                            value = editCost,
                            onValueChange = { editCost = it },
                            label = { Text("التكلفة التقديرية (جنيه)" + if (isCostActive) " 🌟 (نشط)" else "") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isCostActive) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (isCostActive) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            spokenLiveText = ""
                            parsedResult = null
                            errorMessage = null
                            if (isGuidedMode) {
                                guidedStep = 0
                                editCustomerName = ""
                                editCustomerPhone = ""
                                editDeviceName = ""
                                editIssueDesc = ""
                                editCost = ""
                            }
                            checkPermissionAndListen()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isGuidedMode) "البدء من جديد" else "إعادة المحاولة", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val finalData = VoiceParsedData(
                                customerName = editCustomerName,
                                customerPhone = editCustomerPhone,
                                deviceName = editDeviceName,
                                issueDescription = editIssueDesc.ifBlank { spokenLiveText },
                                estimatedCost = editCost,
                                fullTranscription = spokenLiveText
                            )
                            onApplyParsedData(finalData)
                            onDismiss()
                        },
                        enabled = editDeviceName.isNotBlank() || editCustomerName.isNotBlank() || spokenLiveText.isNotBlank(),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(46.dp)
                            .testTag("apply_voice_data_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعبئة الحقول مباشرة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Private helper to map Eastern Arabic digits (٠-٩) to Western (0-9)
private fun convertArabicDigits(input: String): String {
    var result = input
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    for (i in 0..9) {
        result = result.replace(arabicDigits[i], (i + '0'.code).toChar())
    }
    return result
}

// Robust fallback phone cleaning and parsing
private fun cleanSpokenPhoneNumber(text: String): String {
    var phone = text.replace(Regex("[^0-9]"), "")
    if (phone.isEmpty()) {
        var temp = text
        val wordsMap = mapOf(
            "صفر" to "0", "زيرو" to "0", "واحد" to "1",
            "اثنين" to "2", "اتنين" to "2", "ثلاثة" to "3", "تلاتة" to "3",
            "أربعة" to "4", "اربعة" to "4", "خمسة" to "5", "ستة" to "6",
            "سبعة" to "7", "ثمانية" to "8", "تمنية" to "8", "تسعة" to "9"
        )
        for ((word, digit) in wordsMap) {
            temp = temp.replace(word, digit)
        }
        phone = temp.replace(Regex("[^0-9]"), "")
    }
    return phone
}

// Robust fallback cost cleaning and parsing
private fun cleanSpokenCost(text: String): String {
    var cost = text.replace(Regex("[^0-9]"), "")
    if (cost.isEmpty()) {
        var temp = text
        val wordsMap = mapOf(
            "ألفين" to "2000", "الفين" to "2000",
            "ألف" to "1000", "الف" to "1000",
            "مليون" to "1000000",
            "جنيه" to "", "جنية" to "",
            "صفر" to "0", "واحد" to "1", "اثنين" to "2", "اتنين" to "2",
            "ثلاثة" to "3", "تلاتة" to "3", "أربعة" to "4", "اربعة" to "4",
            "خمسة" to "5", "ستة" to "6", "سبعة" to "7", "ثمانية" to "8", "تسعة" to "9"
        )
        for ((word, digit) in wordsMap) {
            temp = temp.replace(word, digit)
        }
        cost = temp.replace(Regex("[^0-9]"), "")
    }
    return cost
}
