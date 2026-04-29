package com.formbuddy.android.ui.screens.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.formbuddy.android.data.billing.BillingManager
import com.formbuddy.android.ui.components.ios.FillinPressContainer
import com.formbuddy.android.ui.components.ios.FillinShapes
import com.formbuddy.android.ui.components.ios.FillinSpacing
import com.formbuddy.android.ui.components.ios.PaywallDeviceHero
import com.formbuddy.android.ui.theme.PaywallBottom
import com.formbuddy.android.ui.theme.PaywallTop

/**
 * iOS-matching paywall (IMG_9046) — violet vertical gradient background, hero
 * "Get More With FormBuddy Pro" headline, bullet-list of features, a
 * horizontally-pageable plan picker (Weekly / Monthly / Yearly with the
 * neighbouring page peeking on the right edge), and a white "Try It Free"
 * CTA pill near the bottom.
 */
@Composable
fun PaywallScreen(
    navController: NavHostController,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val offers by viewModel.offers.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val planOrder = remember {
        listOf(BillingManager.Plan.WEEKLY, BillingManager.Plan.MONTHLY, BillingManager.Plan.YEARLY)
    }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { planOrder.size })
    val features = remember {
        listOf(
            "Multiple Profiles, Family & Business",
            "Agent Mode — Let AI autofill your forms",
            "Voice Mode (beta) — Fill forms by voice",
            "Face ID — Keep your profile private",
            "Backup — Never lose a form again",
            "Watermark-free PDFs",
            "Priority Support"
        )
    }

    LaunchedEffect(isPro) { if (isPro) navController.popBackStack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(listOf(PaywallTop, PaywallBottom))
            )
    ) {
        // Layered iOS device hero — fans up-left from the top half so the headline
        // and feature list sit on top of a faded composition of in-app screenshots.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.TopEnd)
        ) {
            PaywallDeviceHero()
        }

        // Close button — top-right.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(FillinSpacing.padding16)
        ) {
            FillinPressContainer(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 200.dp) // make room for hero device mocks (we render none here)
                .padding(horizontal = FillinSpacing.padding24)
        ) {
            Text(
                text = "Get More With\nFormBuddy Pro",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(FillinSpacing.padding16))
            features.forEach { f ->
                FeatureRow(text = f)
                Spacer(Modifier.height(FillinSpacing.padding8))
            }
            Spacer(Modifier.height(FillinSpacing.padding16))

            // Horizontally-pageable plan cards. The neighbour peeks on the
            // right edge thanks to the inner contentPadding.
            HorizontalPager(
                state = pagerState,
                pageSpacing = FillinSpacing.padding12,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 64.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) { page ->
                val plan = planOrder[page]
                val live = offers.firstOrNull { it.plan == plan }
                PlanCard(
                    title = plan.displayTitle(),
                    price = live?.formattedPrice ?: plan.fallbackPrice(),
                    cadence = plan.cadenceText(),
                    isSelected = pagerState.currentPage == page
                )
            }

            // Page indicator dots.
            Spacer(Modifier.height(FillinSpacing.padding12))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(planOrder.size) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(FillinSpacing.padding24))

            // Try It Free pill button.
            FillinPressContainer(
                onClick = {
                    activity?.let { viewModel.purchase(it, planOrder[pagerState.currentPage]) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(FillinShapes.capsule)
                    .background(Color.White)
            ) {
                Text(
                    text = "Try It Free",
                    color = PaywallBottom,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(FillinSpacing.padding16))
            Text(
                text = "Privacy policy  ·  Terms and conditions",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.height(0.dp))
        Box(modifier = Modifier.padding(start = FillinSpacing.padding12)) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    cadence: String,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.16f else 0.08f))
            .padding(FillinSpacing.padding16)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (isSelected) {
                    Spacer(Modifier.height(0.dp))
                    Box(modifier = Modifier.padding(start = FillinSpacing.padding8)) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = PaywallBottom,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(FillinSpacing.padding8))
            Text(
                text = price,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(FillinSpacing.padding4))
            Text(
                text = cadence,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun BillingManager.Plan.displayTitle(): String = when (this) {
    BillingManager.Plan.WEEKLY -> "Weekly"
    BillingManager.Plan.MONTHLY -> "Monthly"
    BillingManager.Plan.YEARLY -> "Yearly"
}

private fun BillingManager.Plan.cadenceText(): String = when (this) {
    BillingManager.Plan.WEEKLY -> "Subscribe for a Week"
    BillingManager.Plan.MONTHLY -> "Subscribe for a Month"
    BillingManager.Plan.YEARLY -> "Subscribe for a Year"
}

private fun BillingManager.Plan.fallbackPrice(): String = when (this) {
    BillingManager.Plan.WEEKLY -> "$4.99/wk"
    BillingManager.Plan.MONTHLY -> "$9.99/mo"
    BillingManager.Plan.YEARLY -> "$49.99/yr"
}

/** Walks ContextWrappers to find the host Activity. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
