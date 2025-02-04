package com.cmu.a75hard.views

import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraXScreen(
    navController: NavController,
    dayDataDao: DayDataDao,
    dayNumber: Int,
    userId: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val outputDirectory = context.getExternalFilesDir(null)

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Photo") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Preview da câmera
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Botão de Captura
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CameraCaptureButton {
                    val photoFile = File(
                        outputDirectory,
                        "progress_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                capturedUri = Uri.fromFile(photoFile)
                                showConfirmationDialog = true // Abre o diálogo de confirmação
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                }
            }
        }

        // Diálogo de Confirmação
        if (showConfirmationDialog && capturedUri != null) {
            ConfirmationDialog(
                uri = capturedUri!!,
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val dayData = dayDataDao.getDayDataForUser(dayNumber, userId)
                        val updatedData = dayData?.copy(progressPictureUrl = capturedUri.toString())
                            ?: DayData(dayNumber = dayNumber, userId = userId, progressPictureUrl = capturedUri.toString())
                        dayDataDao.insertDayData(updatedData)
                    }
                    showConfirmationDialog = false
                    navController.popBackStack()
                },
                onRetake = {
                    capturedUri = null
                    showConfirmationDialog = false
                }
            )
        }
    }
}

@Composable
fun CameraCaptureButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .border(4.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(MaterialTheme.colorScheme.onBackground, CircleShape)
        )
    }
}

@Composable
fun ConfirmationDialog(
    uri: Uri,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRetake,
        title = { Text("Photo Captured") },
        text = { Text("Do you want to save this photo or take another one?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onRetake) {
                Text("repeat")
            }
        }
    )
}
