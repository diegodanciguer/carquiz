package com.example.carquiz

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.carquiz.ui.theme.CarQuizTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar o Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        setContent {
            CarQuizTheme {
                CarQuizScreen(textToSpeech)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("pt", "BR")  // Configurar para português
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun CarQuizScreen(textToSpeech: TextToSpeech) {
    val coroutineScope = rememberCoroutineScope()

    // Lista de carros e suas marcas associadas (modelo e marca)
    val cars = listOf(
        "jeep_jeep" to "Jeep",
        "celta_chevrolet" to "Chevrolet"
    )

    // Lista de todas as marcas disponíveis
    val allBrands = listOf(
        "Toyota",
        "Chevrolet",
        "Audi",
        "Volkswagen",
        "Jeep"
    )

    // Estados do jogo
    var gameStarted by remember { mutableStateOf(false) }
    var selectedCar by remember { mutableStateOf(cars.random()) }
    var correctBrand by remember { mutableStateOf(selectedCar.second) }
    var feedbackText by remember { mutableStateOf("") }
    var timeUp by remember { mutableStateOf(false) }
    var previousCar by remember { mutableStateOf(selectedCar) } // Armazena o carro anterior

    // Função para carregar um novo carro e garantir que ele seja diferente do anterior
    fun loadNewCar() {
        var newCar: Pair<String, String>
        do {
            newCar = cars.random()
        } while (newCar == previousCar) // Garante que o novo carro seja diferente do anterior

        selectedCar = newCar
        previousCar = newCar // Atualiza o carro anterior
        correctBrand = newCar.second
        feedbackText = ""
        timeUp = false
        speakCarModel(newCar.first, textToSpeech)
    }

    // Função para iniciar o jogo
    fun startGame() {
        gameStarted = true
        loadNewCar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))  // Fundo claro
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botão "Iniciar" visível apenas quando o jogo não começou
        if (!gameStarted) {
            Button(
                onClick = { startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),  // Cor roxa
                shape = RoundedCornerShape(12.dp),  // Botão com cantos arredondados
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(150.dp, 50.dp)
            ) {
                Text(text = "Iniciar", fontSize = 18.sp, color = Color.White)
            }
        }

        if (gameStarted) {
            // Pergunta
            Text(
                text = "Qual a marca do carro?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF333333),  // Cor do texto
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Imagem do carro
            val carImageName = selectedCar.first
            val carImageResId = getImageResource(carImageName)
            Image(
                painter = painterResource(id = carImageResId),
                contentDescription = "Imagem do carro",
                modifier = Modifier
                    .size(300.dp)
                    .shadow(8.dp, RoundedCornerShape(8.dp))  // Sombra na imagem
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Crop
            )

            // Garantir que a marca correta está entre as opções
            val incorrectBrands = allBrands.filter { it != correctBrand }.shuffled().take(3)
            val options = (incorrectBrands + correctBrand).shuffled()

            // Organizar os botões em 2 linhas (2 em cima, 2 embaixo)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primeira linha de botões (2 primeiros)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.take(2).forEach { brand ->
                        Button(
                            onClick = {
                                if (!timeUp) {
                                    feedbackText = if (brand == correctBrand) {
                                        speakFeedback("Isso mesmo, a marca do ${selectedCar.first.split("_")[0]} é $correctBrand", textToSpeech)
                                        "Correto! A marca é $correctBrand"
                                    } else {
                                        speakFeedback("Errado, a marca correta do ${selectedCar.first.split("_")[0]} é $correctBrand", textToSpeech)
                                        "Errado! A marca correta é $correctBrand"
                                    }
                                    timeUp = true

                                    // Após o feedback, esperar um tempo suficiente para o feedback ser falado
                                    coroutineScope.launch {
                                        delay(3000L)  // Tempo para o Text-to-Speech terminar de falar (ajuste conforme necessário)
                                        loadNewCar()  // Carregar o próximo carro
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),  // Cor azul claro
                            shape = RoundedCornerShape(16.dp),  // Botões com cantos arredondados
                            modifier = Modifier
                                .size(120.dp, 60.dp)  // Tamanho maior dos botões
                        ) {
                            val brandImageResId = getImageResource("marca_${brand.lowercase()}")
                            Image(
                                painter = painterResource(id = brandImageResId),
                                contentDescription = "Logo da marca $brand",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Segunda linha de botões (últimos 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.takeLast(2).forEach { brand ->
                        Button(
                            onClick = {
                                if (!timeUp) {
                                    feedbackText = if (brand == correctBrand) {
                                        speakFeedback("Isso mesmo, a marca do ${selectedCar.first.split("_")[0]} é $correctBrand", textToSpeech)
                                        "Correto! A marca é $correctBrand"
                                    } else {
                                        speakFeedback("Errado, a marca correta do ${selectedCar.first.split("_")[0]} é $correctBrand", textToSpeech)
                                        "Errado! A marca correta é $correctBrand"
                                    }
                                    timeUp = true

                                    // Após o feedback, esperar um tempo suficiente para o feedback ser falado
                                    coroutineScope.launch {
                                        delay(3000L)  // Tempo para o Text-to-Speech terminar de falar (ajuste conforme necessário)
                                        loadNewCar()  // Carregar o próximo carro
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),  // Cor azul claro
                            shape = RoundedCornerShape(16.dp),  // Botões com cantos arredondados
                            modifier = Modifier
                                .size(120.dp, 60.dp)  // Tamanho maior dos botões
                        ) {
                            val brandImageResId = getImageResource("marca_${brand.lowercase()}")
                            Image(
                                painter = painterResource(id = brandImageResId),
                                contentDescription = "Logo da marca $brand",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            // Exibir o feedback se a resposta está certa ou errada
            if (feedbackText.isNotEmpty()) {
                Text(
                    text = feedbackText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF444444),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

// Função para pegar o ID da imagem com base no nome do arquivo
@Composable
fun getImageResource(name: String): Int {
    val context = LocalContext.current
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

// Função para falar o modelo do carro
fun speakCarModel(carModel: String, textToSpeech: TextToSpeech) {
    val modelName = carModel.split("_")[0].replaceFirstChar { it.uppercase() }
    val message = "Qual a marca do $modelName?"
    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
}

// Função para dar feedback com voz após a resposta
fun speakFeedback(message: String, textToSpeech: TextToSpeech) {
    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
}
