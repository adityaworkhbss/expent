package com.aditya.expent.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aditya.expent.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

class AppUtils {

    @Composable
    fun ShowProgressAnimation() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.sandy_loading)
            )
            val progress by animateLottieCompositionAsState(
                composition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )
        }
    }

    fun getDayWithSuffix(dateString: String): String {

        val parser = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.ENGLISH
        )

        parser.timeZone = TimeZone.getTimeZone("UTC")

        val date = parser.parse(dateString)

        val formatter = SimpleDateFormat("d", Locale.ENGLISH)

        val day = formatter.format(date!!).toInt()

        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }

        return "$day$suffix"
    }
}