package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val TecnicableShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // chips pequeños, inputs
    small = RoundedCornerShape(12.dp),       // --border-radius: .75rem (botones, chips)
    medium = RoundedCornerShape(16.dp),      // --border-radius: 1rem (tarjetas)
    large = RoundedCornerShape(20.dp),       // tarjetas grandes / diálogos
    extraLarge = RoundedCornerShape(32.dp)   // --border-radius: 2rem (hojas inferiores, modal de vinculación)
)

// Píldora/circular, para botones tipo "pill" y avatares (border-radius: 9999px en la web)
val TecnicablePillShape = RoundedCornerShape(percent = 50)
