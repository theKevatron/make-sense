package com.example.makesense

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import com.google.gson.Gson
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(navController, startDestination = "sensorList") {
                        composable("sensorList") {
                            SensorListScreen(sensors, navController)
                        }
                        composable(
                            route = "sensorDetail/{sensorJson}",
                            arguments = listOf(navArgument("sensorJson") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val json = backStackEntry.arguments?.getString("sensorJson")
                            val sensor = Gson().fromJson(json, SensorData::class.java)
                            SensorDetailScreen(sensor, sensorManager, navController)
                        }
                    }
                }
            }
        }
    }
}

data class SensorData(
    val name: String,
    val vendor: String,
    val type: Int,
    val version: Int,
    val power: Float,
    val resolution: Float,
    val maxRange: Float
)

@Composable
fun SensorListScreen(sensors: List<Sensor>, navController: NavController) {
    Column {
        Text(
            text = "Detected ${sensors.size} sensors",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp)
        )
        LazyColumn {
            items(sensors) { sensor ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val sensorData = SensorData(
                                name = sensor.name,
                                vendor = sensor.vendor,
                                type = sensor.type,
                                version = sensor.version,
                                power = sensor.power,
                                resolution = sensor.resolution,
                                maxRange = sensor.maximumRange
                            )
                            val json = URLEncoder.encode(Gson().toJson(sensorData), StandardCharsets.UTF_8.toString())
                            navController.navigate("sensorDetail/$json")
                        }
                        .padding(12.dp)
                ) {
                    Text(text = sensor.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Vendor: ${sensor.vendor}", style = MaterialTheme.typography.bodySmall)
                }
                Divider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailScreen(sensor: SensorData, sensorManager: SensorManager, navController: NavController) {
    var sensorValues by remember { mutableStateOf(listOf<Float>()) }

    // Register a listener when this screen appears
    DisposableEffect(sensor.name) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    sensorValues = it.values.toList()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val targetSensor = sensorManager.getDefaultSensor(sensor.type)
        sensorManager.registerListener(listener, targetSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sensor.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "Vendor: ${sensor.vendor}")
            Text(text = "Type: ${sensor.type}")
            Text(text = "Version: ${sensor.version}")
            Text(text = "Power: ${sensor.power} mA")
            Text(text = "Resolution: ${sensor.resolution}")
            Text(text = "Max Range: ${sensor.maxRange}")
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            Text(text = "Live Values:", style = MaterialTheme.typography.titleMedium)
            if (sensorValues.isNotEmpty()) {
                sensorValues.forEachIndexed { index, value ->
                    Text(text = "Value[$index]: $value")
                }
            } else {
                Text(text = "Waiting for sensor data...")
            }
        }
    }
}