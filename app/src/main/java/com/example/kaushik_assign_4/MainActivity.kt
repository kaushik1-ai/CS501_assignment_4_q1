package com.example.kaushik_assign_4

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaushik_assign_4.ui.theme.Kaushik_assign_4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Locking orientation to Portrait is standard for tilt/gyro games
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            Kaushik_assign_4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BallGame(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BallGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Assignment Requirement: Use Gyroscope (TYPE_GYROSCOPE)
    val gyroscope = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    // Ball state: Position and Velocity
    var ballX by remember { mutableFloatStateOf(150f) }
    var ballY by remember { mutableFloatStateOf(150f) }
    var velocityX by remember { mutableFloatStateOf(0f) }
    var velocityY by remember { mutableFloatStateOf(0f) }

    // Debugging values to see if sensor is working
    var gyroX by remember { mutableFloatStateOf(0f) }
    var gyroY by remember { mutableFloatStateOf(0f) }

    // Screen dimensions (will be set in Canvas)
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }

    var gameStatus by remember { mutableStateOf("Rotate phone to move!") }

    // Game constants
    val ballRadius = 35f
    val friction = 0.97f // Air resistance
    val sensitivity = 2.5f // Gyroscope needs higher sensitivity than accelerometer

    // Walls and Obstacles (Maze elements)
    val obstacles = remember {
        listOf(
            Rect(0f, 400f, 650f, 450f),
            Rect(450f, 800f, 1100f, 850f),
            Rect(200f, 1100f, 260f, 1500f),
            Rect(600f, 1350f, 1000f, 1400f),
            Rect(750f, 250f, 800f, 600f)
        )
    }

    // Goal zone (Bottom Right)
    val goalRect = remember(screenWidth, screenHeight) {
        if (screenWidth > 0 && screenHeight > 0) {
            Rect(
                screenWidth - 180f,
                screenHeight - 180f,
                screenWidth - 40f,
                screenHeight - 40f
            )
        } else {
            Rect(0f, 0f, 0f, 0f)
        }
    }

    // Sensor Lifecycle Management
    DisposableEffect(gyroscope) {
        if (gyroscope == null) {
            gameStatus = "ERROR: Gyroscope not found!"
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    gyroX = event.values[0] // Rotation around X (Pitch)
                    gyroY = event.values[1] // Rotation around Y (Roll)

                    // Update velocity based on angular velocity
                    // axisX rotation moves ball Up/Down
                    // axisY rotation moves ball Left/Right
                    velocityY += gyroX * sensitivity
                    velocityX += gyroY * sensitivity
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Safe registration
        if (gyroscope != null) {
            sensorManager.registerListener(
                listener,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Physics Engine Loop
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                // Apply friction
                velocityX *= friction
                velocityY *= friction

                var nextX = ballX + velocityX
                var nextY = ballY + velocityY

                if (screenWidth > 0f && screenHeight > 0f) {

                    // 1. Boundary Collisions (Walls of the screen)
                    if (nextX - ballRadius < 0f) {
                        nextX = ballRadius
                        velocityX = -velocityX * 0.4f // Bounce
                    } else if (nextX + ballRadius > screenWidth) {
                        nextX = screenWidth - ballRadius
                        velocityX = -velocityX * 0.4f
                    }

                    if (nextY - ballRadius < 0f) {
                        nextY = ballRadius
                        velocityY = -velocityY * 0.4f
                    } else if (nextY + ballRadius > screenHeight) {
                        nextY = screenHeight - ballRadius
                        velocityY = -velocityY * 0.4f
                    }

                    // 2. Obstacle Collisions
                    obstacles.forEach { wall ->
                        if (
                            nextX + ballRadius > wall.left &&
                            nextX - ballRadius < wall.right &&
                            nextY + ballRadius > wall.top &&
                            nextY - ballRadius < wall.bottom
                        ) {
                            // Determine collision side
                            val overlapX = if (ballX < wall.left) (nextX + ballRadius) - wall.left else wall.right - (nextX - ballRadius)
                            val overlapY = if (ballY < wall.top) (nextY + ballRadius) - wall.top else wall.bottom - (nextY - ballRadius)

                            if (overlapX < overlapY) {
                                nextX = ballX
                                velocityX = -velocityX * 0.5f
                            } else {
                                nextY = ballY
                                velocityY = -velocityY * 0.5f
                            }
                        }
                    }

                    // 3. Win Condition (Goal)
                    if (goalRect.contains(Offset(nextX, nextY))) {
                        gameStatus = "YOU WIN! 🎉"
                    }
                }

                ballX = nextX
                ballY = nextY
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidth = size.width
            screenHeight = size.height

            // Background
            drawRect(color = Color(0xFFF5F5F5))

            // Draw Goal
            drawRect(
                color = Color(0xFF43A047),
                topLeft = Offset(goalRect.left, goalRect.top),
                size = Size(goalRect.width, goalRect.height)
            )

            // Draw Maze Walls
            obstacles.forEach { wall ->
                drawRect(
                    color = Color(0xFF3E2723),
                    topLeft = Offset(wall.left, wall.top),
                    size = Size(wall.width, wall.height)
                )
            }

            // Draw the Ball
            drawCircle(
                color = Color(0xFFD32F2F),
                radius = ballRadius,
                center = Offset(ballX, ballY)
            )
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = gameStatus, 
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))
            
            // Debug Info: Helps verify sensor activity
            Text(text = "Gyro X (Speed): ${"%.2f".format(gyroX)}", fontSize = 12.sp, color = Color.Gray)
            Text(text = "Gyro Y (Speed): ${"%.2f".format(gyroY)}", fontSize = 12.sp, color = Color.Gray)
            
            if (gameStatus.contains("WIN")) {
                Button(
                    onClick = {
                        ballX = 150f
                        ballY = 150f
                        velocityX = 0f
                        velocityY = 0f
                        gameStatus = "Rotate phone to move!"
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Play Again")
                }
            }
        }
        
        Text(
            text = "GOAL",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 80.dp),
            color = Color.White,
            fontSize = 18.sp
        )
    }
}
