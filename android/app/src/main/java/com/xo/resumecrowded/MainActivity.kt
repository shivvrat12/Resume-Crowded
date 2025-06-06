package com.xo.resumecrowded

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xo.resumecrowded.ui.theme.ResumeCrowdedTheme
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResumeCrowdedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val viewModel: MViewModel = viewModel()
    val context = LocalContext.current
    val apiResponse by viewModel.apiResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadPdf(uri, context)
        }
    }

    // Parse and clean the API response
    val markdown = apiResponse?.let { response ->
        try {
            val json = JSONObject(response)
            val feedback = json.getString("feedback")
            cleanMarkdownContent(feedback)
        } catch (e: Exception) {
            "Error parsing feedback: ${e.message}"
        }
    } ?: ""

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Resume Crowded",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(top = 20.dp)
            )

            // Upload Button
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = "Upload Icon",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Upload Resume (PDF)",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            // Content Area
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                        }

                        markdown.isEmpty() -> {
                            Text(
                                text = "Upload a resume to get detailed feedback.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        markdown.startsWith("Error") -> {
                            Text(
                                text = markdown,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                item {
                                    SimpleMarkdownRenderer(
                                        markdown = markdown,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleMarkdownRenderer(markdown: String, modifier: Modifier = Modifier) {
    val annotatedString = buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEach { line ->
            when {
                // Handle headings (e.g., "# Heading")
                line.startsWith("# ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                        append(line.removePrefix("# ").trim() + "\n")
                    }
                }
                // Handle main bullet points (e.g., "* Item")
                line.trim().startsWith("* ") -> {
                    withStyle(style = SpanStyle(fontSize = 16.sp)) {
                        append("• ")
                        val content = line.trim().removePrefix("* ").trim()
                        parseInlineMarkdown(content, this)
                        append("\n")
                    }
                }
                // Handle nested bullet points (e.g., "  * Sub項")
                line.trim().startsWith("  * ") -> {
                    withStyle(style = SpanStyle(fontSize = 16.sp)) {
                        append("    • ")
                        val content = line.trim().removePrefix("  * ").trim()
                        parseInlineMarkdown(content, this)
                        append("\n")
                    }
                }
                // Handle plain text or other lines
                else -> {
                    withStyle(style = SpanStyle(fontSize = 16.sp)) {
                        parseInlineMarkdown(line.trim(), this)
                        append("\n")
                    }
                }
            }
        }
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// Helper function to parse inline Markdown (e.g., **bold**, *italic*)
private fun parseInlineMarkdown(content: String, builder: AnnotatedString.Builder) {
    val parts = content.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            // Bold text
            builder.withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(part)
            }
        } else {
            // Check for italic text (*text*)
            val italicParts = part.split("*")
            italicParts.forEachIndexed { italicIndex, italicPart ->
                if (italicIndex % 2 == 1) {
                    builder.withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicPart)
                    }
                } else {
                    builder.append(italicPart)
                }
            }
        }
    }
}

fun cleanMarkdownContent(raw: String): String {
    // Crop exactly 15 characters from the beginning
    val cleaned = if (raw.length >= 15) raw.substring(0) else raw

    // Remove content after "**Explanation:**" if present
    val endMarker = "**Explanation:**"
    val endIndex = cleaned.indexOf(endMarker).takeIf { it >= 0 } ?: cleaned.length
    var result = cleaned.substring(0, endIndex)

    // Replace escaped newlines (\n) with actual newlines
    result = result.replace("\\n", "\n")

    // Fix bullet point formatting (e.g., "*   " to "* ")
    result = result.replace(Regex("\\*\\s{2,}"), "* ")

    return result.trim()
}

fun prepareFilePart(uri: Uri, context: Context): MultipartBody.Part {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri)
    val fileBytes = inputStream?.readBytes()
    val requestBody = fileBytes?.toRequestBody("application/pdf".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("resume", "resume.pdf", requestBody!!)
}