package com.google.android.apps.photos

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

import androidx.core.content.IntentCompat
import androidx.core.net.toUri

import com.google.android.apps.photos.ui.theme.GPhotosShimTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val receivedIntent = intent
        val sessionId = receivedIntent.getStringExtra("external_session_id")
        val uri = receivedIntent.data?.toString()?.toUri()
        val type = receivedIntent.type
        val processingURI = IntentCompat.getParcelableExtra(receivedIntent, "processing_uri_intent_extra", Uri::class.java)
        val cameraRelaunchIntent = IntentCompat.getParcelableExtra(receivedIntent, "CAMERA_RELAUNCH_INTENT_EXTRA", PendingIntent::class.java)

        if (sessionId == null || uri == null) {
            finish()
            return
        }

        Log.i(
            "PhotosShim", "SessionId: '$sessionId', URI:'$uri', type: '$type', processingURI: '$processingURI'"
        )

        setContent {
            GPhotosShimTheme {
                MainScreen(
                    uri = uri,
                    onFinished = { success ->
                        if (success) {
                            launchReviewIntent(uri)
                        } else {
                            quit(cameraRelaunchIntent)
                        }
                    },
                    onRequestPermission = { requestSpecialPermission() }
                )
            }
        }
    }

    private fun requestSpecialPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Special permission not required for this Android version", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchReviewIntent(uri: Uri) {
        // We use ACTION_VIEW instead of ACTION_REVIEW here because:
        // 1. It allows the system to show the "Always" / "Just once" buttons.
        // 2. GPhotosShim handles ACTION_REVIEW (to be the shim), but it does NOT
        //    handle ACTION_VIEW. This naturally excludes us from the list.
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("PhotosShim", "No app found to handle ACTION_VIEW", e)
        }
        finish()
    }

    private fun quit(cameraRelaunchIntent: PendingIntent?) {
        try {
            cameraRelaunchIntent?.send()
        } catch (e: Exception) {
            Log.e("PhotosShim", "Error sending intent", e)
        }
        finish()
    }
}

@Composable
fun MainScreen(
    uri: Uri,
    onFinished: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    var hasPermission by remember { mutableStateOf(checkPermissionStatus()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                hasPermission = checkPermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (hasPermission) {
        Dialog(
            onDismissRequest = { onFinished(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .safeDrawingPadding(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                ProcessingScreen(
                    uri = uri,
                    onFinished = onFinished
                )
            }
        }
    } else {
        Dialog(
            onDismissRequest = { onFinished(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .safeDrawingPadding(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                PermissionRequestScreen(
                    onRequestPermission = onRequestPermission
                )
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(modifier: Modifier = Modifier, onRequestPermission: () -> Unit) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_gallery_link),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Permission Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.permission_explanation_gphotoshim),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}

@Composable
fun ProcessingScreen(
    modifier: Modifier = Modifier,
    uri: Uri,
    onFinished: (Boolean) -> Unit
) {
    var statusText by remember { mutableStateOf("Processing photo...") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uri) {
        var success = false
        val waitDuration = 100.milliseconds
        val maximumDuration = 30.seconds
        val maxIterations = (maximumDuration / waitDuration).toInt()

        for (i in 0 until maxIterations) {
            if (!isUriPending(context, uri)) {
                success = true
                break
            }
            Log.i("PhotosShim", "Pending... iteration $i")
            delay(waitDuration)
        }
        
        if (!success) {
            statusText = "Timeout waiting for photo"
            delay(1.seconds) // Show error briefly
        }
        onFinished(success)
    }

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = statusText, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun checkPermissionStatus(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}

// Check if a URI for an image is still being processed
private fun isUriPending(context: Context, contentUri: Uri): Boolean {
    val proj = arrayOf(MediaStore.Images.Media.IS_PENDING)
    return try {
        context.contentResolver.query(contentUri, proj, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_PENDING)
                cursor.getInt(columnIndex) != 0
            } else {
                false
            }
        } ?: false
    } catch (e: Exception) {
        Log.e("PhotosShim", "Error checking pending status", e)
        false
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRequestScreenPreview() {
    GPhotosShimTheme {
        PermissionRequestScreen(onRequestPermission = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ProcessingScreenPreview() {
    GPhotosShimTheme {
        ProcessingScreen(
            uri = Uri.EMPTY,
            onFinished = {}
        )
    }
}
