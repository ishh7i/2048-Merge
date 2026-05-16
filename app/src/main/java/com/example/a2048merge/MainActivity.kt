package com.example.a2048merge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a2048merge.ui.theme._2048MergeTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioAttributes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSplash by remember {
                mutableStateOf(true)
            }
            _2048MergeTheme {
                if (showSplash) {
                    SplashScreen {
                        showSplash = false
                    }
                } else {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {

    LaunchedEffect(Unit) {
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF312E81)
                    )
                )
            ),

        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,

                modifier = Modifier
                    .size(180.dp),

                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "2048 Merge",

                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp,

                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFA9F1DF),
                            Color(0xFFD8B5FF),
                            Color(0xFFB5FFFF)
                        )
                    )
                )
            )
        }
    }
}

@Composable
fun GameScreen() {

    val context = LocalContext.current

    val sharedPreferences =
        context.getSharedPreferences(
            "game_data",
            Context.MODE_PRIVATE
        )

    val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    val moveSound = soundPool.load(
        context,
        R.raw.merge,
        1
    )

    soundPool.setOnLoadCompleteListener { _, _, _ ->

    }

    var board by remember {

        mutableStateOf(

            addRandomTile(
                addRandomTile(
                    List(4) { List(4) { 0 } }
                )
            )
        )
    }

    var score by remember {
        mutableStateOf(0)
    }

    var highScore by remember {

        mutableStateOf(
            sharedPreferences.getInt(
                "high_score",
                0
            )
        )
    }

    fun moveLeft(board: List<List<Int>>): List<List<Int>> {

        return board.map { row ->

            val filtered = row.filter { it != 0 }.toMutableList()

            var i = 0

            while (i < filtered.size - 1) {

                if (filtered[i] == filtered[i + 1]) {

                    filtered[i] *= 2
                    score += filtered[i]

                    if (score > highScore) {

                        highScore = score

                        sharedPreferences.edit()
                            .putInt(
                                "high_score",
                                highScore
                            )
                            .apply()
                    }
                    filtered.removeAt(i + 1)

                }

                i++
            }

            while (filtered.size < 4) {
                filtered.add(0)
            }

            filtered
        }
    }

    fun moveRight(board: List<List<Int>>): List<List<Int>> {

        return board.map { row ->

            val reversed = row.reversed()

            val merged = moveLeft(listOf(reversed))[0]

            merged.reversed()
        }
    }

    fun moveUp(board: List<List<Int>>): List<List<Int>> {

        val newBoard = MutableList(4) { MutableList(4) { 0 } }

        for (col in 0..3) {

            val column = board.map { it[col] }

            val merged = moveLeft(listOf(column))[0]

            for (row in 0..3) {

                newBoard[row][col] = merged[row]

            }
        }

        return newBoard
    }

    fun moveDown(board: List<List<Int>>): List<List<Int>> {

        val newBoard = MutableList(4) { MutableList(4) { 0 } }

        for (col in 0..3) {

            val column = board.map { it[col] }.reversed()

            val merged = moveLeft(listOf(column))[0].reversed()

            for (row in 0..3) {

                newBoard[row][col] = merged[row]

            }
        }

        return newBoard
    }

    val won = hasWon(board)

    val gameOver = isGameOver(board)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1B3A),
                        Color(0xFF23244D),
                        Color(0xFF2B2E5A)
                    )
                )
            )
            .pointerInput(Unit) {

                var totalDragX = 0f
                var totalDragY = 0f

                detectDragGestures(

                    onDragEnd = {

                        if (kotlin.math.abs(totalDragX) >
                            kotlin.math.abs(totalDragY)
                        ) {

                            if (totalDragX > 0) {

                                val newBoard = moveRight(board)

                                if (newBoard != board) {
                                    soundPool.play(
                                        moveSound,
                                        1f,
                                        1f,
                                        1,
                                        0,
                                        1f
                                    )
                                    board = addRandomTile(newBoard)
                                }

                            } else {

                                val newBoard = moveLeft(board)

                                if (newBoard != board) {
                                    board = addRandomTile(newBoard)
                                    soundPool.play(
                                        moveSound,
                                        1f,
                                        1f,
                                        1,
                                        0,
                                        1f
                                    )
                                }
                            }

                        } else {

                            if (totalDragY > 0) {

                                val newBoard = moveDown(board)

                                if (newBoard != board) {
                                    board = addRandomTile(newBoard)
                                    soundPool.play(
                                        moveSound,
                                        1f,
                                        1f,
                                        1,
                                        0,
                                        1f
                                    )
                                }

                            } else {

                                val newBoard = moveUp(board)

                                if (newBoard != board) {
                                    board = addRandomTile(newBoard)
                                    soundPool.play(
                                        moveSound,
                                        1f,
                                        1f,
                                        1,
                                        0,
                                        1f
                                    )
                                }
                            }
                        }

                        totalDragX = 0f
                        totalDragY = 0f
                    }

                ) { _, dragAmount ->

                    totalDragX += dragAmount.x
                    totalDragY += dragAmount.y

                }
            }
            .padding(16.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "2048 Merge",
            fontSize = 55.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 6.sp,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA9F1DF),
                        Color(0xFFD8B5FF),
                        Color(0xFFB5FFFF)
                    )
                )
            ),
            modifier = Modifier
                .padding(top = 40.dp, bottom = 20.dp),
        )

        Spacer(modifier = Modifier.height(100.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Color(0xFF3A3B5E),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Current Score: $score",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Color(0xFF3A3B5E),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "High Score: $highScore",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            sharedPreferences.edit()
                .putInt(
                    "high_score",
                    highScore
                )
                .apply()
        }

        Spacer(modifier = Modifier.height(30.dp))

        GameBoard(board)

        Spacer(modifier = Modifier.height(20.dp))

        if (won) {

            Text(
                text = "🎉 YOU WON!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDFCFC)
            )
        }

        if (gameOver) {

            Text(
                text = "GAME OVER",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )
        }

    }
}

@Composable
fun GameBoard(board: List<List<Int>>) {

    Column(
        modifier = Modifier
            .background(
                Color(0xFF2A2B4D),
                RoundedCornerShape(12.dp)
            )
            .padding(6.dp)
    ) {

        board.forEach { row ->

            Row {

                row.forEach { value ->

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(80.dp)
                            .background(
                                color = getTileColor(value),
                                shape = RoundedCornerShape(8.dp)
                            ),

                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = if (value == 0) "" else value.toString(),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

fun getTileColor(value: Int): Color {

    return when (value) {

        0 -> Color(0xFF3A3B5E)

        2 -> Color(0xFF9DE8D6)
        4 -> Color(0xFFD4BAF1)

        8 -> Color(0xFFD0E79B)
        16 -> Color(0xFFEE8689)

        32 -> Color(0xFFF1EAB9)
        64 -> Color(0xFFE79191)

        128 -> Color(0xFF85B6F1)
        256 -> Color(0xFFA069A6)

        512 -> Color(0xFF7E4358)
        1024 -> Color(0xFF6760A8)

        2048 -> Color(0xFF9A5A6C)

        else -> Color(0xFFFFBBBB)
    }
}

fun addRandomTile(board: List<List<Int>>): List<List<Int>> {

    val emptyCells = mutableListOf<Pair<Int, Int>>()

    board.forEachIndexed { rowIndex, row ->

        row.forEachIndexed { colIndex, value ->

            if (value == 0) {

                emptyCells.add(rowIndex to colIndex)

            }
        }
    }

    if (emptyCells.isEmpty()) return board

    val (row, col) = emptyCells.random()

    val newBoard = board.map { it.toMutableList() }

    newBoard[row][col] =
        if ((0..9).random() < 9) 2 else 4

    return newBoard
}

fun hasWon(board: List<List<Int>>): Boolean {

    return board.flatten().contains(2048)

}

fun isGameOver(board: List<List<Int>>): Boolean {

    if (board.flatten().contains(0)) {
        return false
    }

    for (row in 0..3) {

        for (col in 0..2) {

            if (board[row][col] ==
                board[row][col + 1]
            ) {
                return false
            }
        }
    }

    for (col in 0..3) {

        for (row in 0..2) {

            if (board[row][col] ==
                board[row + 1][col]
            ) {
                return false
            }
        }
    }

    return true
}