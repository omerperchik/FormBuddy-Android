package com.formbuddy.android.ui.screens.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.formbuddy.android.R
import com.formbuddy.android.data.billing.BillingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    navController: NavHostController,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    var selectedPlan by remember { mutableIntStateOf(1) } // 0=weekly, 1=monthly, 2=yearly
    val offers by viewModel.offers.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Auto-dismiss when subscription becomes active.
    LaunchedEffect(isPro) {
        if (isPro) navController.popBackStack()
    }

    val planOrder = listOf(BillingManager.Plan.WEEKLY, BillingManager.Plan.MONTHLY, BillingManager.Plan.YEARLY)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features list
            val features = listOf(
                Icons.Filled.SmartToy to stringResource(R.string.paywall_feature_agent),
                Icons.Filled.RecordVoiceOver to stringResource(R.string.paywall_feature_voice),
                Icons.Filled.AutoAwesome to stringResource(R.string.paywall_feature_profiles),
                Icons.Filled.Fingerprint to stringResource(R.string.paywall_feature_biometric)
            )

            features.forEach { (icon, text) ->
                FeatureRow(icon = icon, text = text)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan selector — uses live Google Play prices when available, falling back
            // to the local resource strings if Play hasn't returned details yet.
            val planLabels = listOf(
                stringResource(R.string.paywall_plan_weekly),
                stringResource(R.string.paywall_plan_monthly),
                stringResource(R.string.paywall_plan_yearly)
            )
            val fallbackPrices = listOf(
                stringResource(R.string.paywall_price_weekly),
                stringResource(R.string.paywall_price_monthly),
                stringResource(R.string.paywall_price_yearly)
            )

            planOrder.forEachIndexed { index, plan ->
                val live = offers.firstOrNull { it.plan == plan }
                PlanCard(
                    name = planLabels[index],
                    price = live?.formattedPrice ?: fallbackPrices[index],
                    isSelected = index == selectedPlan,
                    isBestValue = plan == BillingManager.Plan.YEARLY,
                    onClick = { selectedPlan = index }
                )
                if (index < planOrder.size - 1) Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    activity?.let { viewModel.purchase(it, planOrder[selectedPlan]) }
                },
                enabled = activity != null && offers.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.paywall_subscribe),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            OutlinedButton(
                onClick = { viewModel.restore() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.paywall_restore))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Walks ContextWrappers to find the host Activity (needed for launchBillingFlow). */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(
    name: String,
    price: String,
    isSelected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    if (isBestValue) {
                        Text(
                            text = stringResource(R.string.paywall_best_value),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
