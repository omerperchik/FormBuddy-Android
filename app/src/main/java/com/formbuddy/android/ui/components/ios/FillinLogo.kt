package com.formbuddy.android.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tiny app-icon widget that sits at the top-left of the home/profile/settings
 * screens in the iOS app — a white circle with a black rounded square inside.
 * Matches the asset in `Fillin/SupportingFiles/Assets.xcassets/Logo.imageset/64.png`.
 */
@Composable
fun FillinLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    onClick: (() -> Unit)? = null
) {
    val container: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black)
            )
        }
    }
    Box(modifier = modifier.padding(FillinSpacing.padding4)) {
        if (onClick != null) {
            FillinPressContainer(onClick = onClick, modifier = Modifier.size(size)) { container() }
        } else {
            container()
        }
    }
}
