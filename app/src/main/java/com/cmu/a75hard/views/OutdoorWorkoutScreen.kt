package com.cmu.a75hard.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cmu.a75hard.R
import com.cmu.a75hard.model.DayData.DayDataDao
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource


@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OutdoorWorkoutScreen(
    navController: NavController,
    dayDataDao: DayDataDao,
    userId: Int,
    dayNumber: Int
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coroutineScope = rememberCoroutineScope()

    // Instanciar o ViewModel
    val viewModel: OutdoorWorkoutViewModel = viewModel(
        factory = OutdoorWorkoutViewModelFactory(dayDataDao, userId, dayNumber)
    )

    // Estado do mapa
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(39.5, -8.0), 6f)
    }

    // Observando os estados do ViewModel
    val isRunning by viewModel.isRunning
    val timerText by viewModel.timerText
    val distance by viewModel.distance
    val pace by viewModel.pace
    val calories by viewModel.calories
    val maxSpeed by viewModel.maxSpeed
    val heartRate by viewModel.heartRate
    val acceleration by viewModel.acceleration
    val path = viewModel.path

    // Warm-Up Strings
    val preRunWarmupStr = stringResource(R.string.pre_run_warmup)
    val preventInjuryStr = stringResource(R.string.prevent_injury)

    // Configuração do GPS
    val locationRequest = LocationRequest.create().apply {
        interval = 2000L
        fastestInterval = 1000L
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }



    // Sensores
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    val gravity = FloatArray(3)
    val linearAcceleration = FloatArray(3)
    var stepThreshold = 1.5 // Valor inicial padrão
    val calibrationData = mutableListOf<Double>() // Dados coletados para calibração inicial
    var calibrated = false // Indica se o threshold foi calibrado
    val calibrationSteps = 20 // Número de passos necessários para calibrar


    // Listener do Acelerômetro
    val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.values?.let { values ->
                val alpha = 0.8f

                // Filtrar gravidade (baixa frequência)
                gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2]

                // Subtrair gravidade da aceleração total
                linearAcceleration[0] = values[0] - gravity[0]
                linearAcceleration[1] = values[1] - gravity[1]
                linearAcceleration[2] = values[2] - gravity[2]

                // Calcular aceleração linear resultante
                val currentAcceleration = Math.sqrt(
                    (linearAcceleration[0] * linearAcceleration[0] +
                            linearAcceleration[1] * linearAcceleration[1] +
                            linearAcceleration[2] * linearAcceleration[2]).toDouble()
                )

                if (!calibrated) {
                    // Coletar dados para calibração
                    calibrationData.add(currentAcceleration)
                    if (calibrationData.size >= calibrationSteps) {
                        // Calcular o threshold com base nos dados coletados
                        stepThreshold = calibrateStepThreshold(calibrationData)
                        calibrated = true // Marcar como calibrado
                        Log.d("FitnessApp", "Calibração completa. Novo stepThreshold: $stepThreshold")
                    }
                } else {
                    // Detectar passos com base no threshold calibrado
                    if (currentAcceleration - viewModel.getLastAcceleration() > stepThreshold) {
                        val newStepCount = viewModel.stepCount.value + 1
                        viewModel.updateStepCount(newStepCount)
                    }
                }
                viewModel.setLastAcceleration(currentAcceleration)

                // Atualizar aceleração no ViewModel
                viewModel.addAcceleration(currentAcceleration)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }



    // Listener do Frequência Cardíaca
    val heartRateListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.let { values ->
                    val currentHeartRate = values[0].toInt()
                    viewModel.addHeartRate(currentHeartRate)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // Registrar listeners dos sensores
    DisposableEffect(Unit) {
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        if (heartRateSensor != null) {
            sensorManager.registerListener(heartRateListener, heartRateSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            viewModel.setHeartRateUnavailable()
        }

        onDispose {
            sensorManager.unregisterListener(accelerometerListener)
            sensorManager.unregisterListener(heartRateListener)
        }
    }

    // Callback de localização
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    if (location.latitude in -90.0..90.0 &&
                        location.longitude in -180.0..180.0 &&
                        location.accuracy <= 50
                    ) {
                        val newLocation = LatLng(location.latitude, location.longitude)
                        viewModel.addLocation(newLocation, location.speed)

                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(newLocation, 15f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Solicitar permissão ao carregar a tela
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Iniciar atualizações de localização quando permitido
    DisposableEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Outdoor Workout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            WarmUpButton(navController, preRunWarmupStr, preventInjuryStr)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Temporizador
            Text(
                text = timerText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Estatísticas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(value = String.format("%.2f", distance), label = "KM")
                StatColumn(value = pace, label = "Pace (min/km)")
                StatColumn(value = "$calories", label = "KCAL")
                StatColumn(value = "${viewModel.stepCount.value}", label = "Passos")
                StatColumn(value = if (heartRate == -1) "N/A" else "$heartRate BPM", label = "Heart Rate")
                StatColumn(value = String.format("%.2f", acceleration), label = "Acceleration")
            }



            // Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true)
                ) {
                    if (path.isNotEmpty()) {
                        Polyline(points = path.toList())
                    }
                }

                // Botão de início/parada
                IconButton(
                    onClick = {
                        if (isRunning) {
                            viewModel.stopWorkout()
                        } else {
                            viewModel.startWorkout()
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = if (isRunning) R.drawable.ic_stop else R.drawable.ic_flag),
                        contentDescription = if (isRunning) "Stop" else "Start",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp
        )
    }
}

fun calibrateStepThreshold(data: List<Double>): Double {
    // Ignorar valores muito baixos (ruído) e calcular a média
    val validData = data.filter { it > 1.0 }
    val averageAcceleration = if (validData.isNotEmpty()) validData.average() else 1.5
    return averageAcceleration * 1.2 // Adicionar margem de segurança
}

@Composable
fun WarmUpButton(navController: NavController, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .clickable { navController.navigate("warmUp") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_exercise),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

