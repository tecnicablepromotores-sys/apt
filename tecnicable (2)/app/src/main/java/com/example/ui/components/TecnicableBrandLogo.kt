package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun TecnicableBrandLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    textSize: TextUnit = 18.sp,
    textColorPrimary: Color = MaterialTheme.colorScheme.primary,
    textColorSecondary: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.tecnicable_logo),
            contentDescription = "Tecnicable Logo",
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tecni",
                color = textColorPrimary,
                fontSize = textSize,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "cable",
                color = textColorSecondary,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
