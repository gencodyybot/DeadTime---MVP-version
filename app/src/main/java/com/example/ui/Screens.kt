package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ClaimedDeal
import com.example.data.Deal
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.random.Random

// --- SYSTEM-WIDE UTILS & RENDERING COMPONENTALS ---

@Composable
fun ProceduralQRCode(claimId: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color.White)) {
        val canvasSize = this.size.width
        val blocks = 14
        val blockSize = canvasSize / blocks
        val random = java.util.Random(claimId.hashCode().toLong())

        // 1. Draw standard QR Positioning Marks (Outer and Inner cubes)
        // Top-Left corner
        drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(blockSize * 4, blockSize * 4))
        drawRect(Color.White, topLeft = Offset(blockSize, blockSize), size = Size(blockSize * 2, blockSize * 2))
        drawRect(Color.Black, topLeft = Offset(blockSize * 1.51f, blockSize * 1.51f), size = Size(blockSize, blockSize))

        // Top-Right corner
        drawRect(Color.Black, topLeft = Offset(canvasSize - blockSize * 4, 0f), size = Size(blockSize * 4, blockSize * 4))
        drawRect(Color.White, topLeft = Offset(canvasSize - blockSize * 3, blockSize), size = Size(blockSize * 2, blockSize * 2))
        drawRect(Color.Black, topLeft = Offset(canvasSize - blockSize * 2.51f, blockSize * 1.51f), size = Size(blockSize, blockSize))

        // Bottom-Left corner
        drawRect(Color.Black, topLeft = Offset(0f, canvasSize - blockSize * 4), size = Size(blockSize * 4, blockSize * 4))
        drawRect(Color.White, topLeft = Offset(blockSize, canvasSize - blockSize * 3), size = Size(blockSize * 2, blockSize * 2))
        drawRect(Color.Black, topLeft = Offset(blockSize * 1.51f, canvasSize - blockSize * 2.51f), size = Size(blockSize, blockSize))

        // 2. Fill the body spaces pseudo-randomly
        for (r in 0 until blocks) {
            for (c in 0 until blocks) {
                // Skip the areas reserved for corner squares
                val isCorner = (r < 4 && c < 4) || (r < 4 && c >= blocks - 4) || (r >= blocks - 4 && c < 4)
                if (!isCorner) {
                    if (random.nextBoolean()) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * blockSize, r * blockSize),
                            size = Size(blockSize + 0.3f, blockSize + 0.3f)
                        )
                    }
                }
            }
        }
    }
}

// Visual category helpers
fun getCategoryIcon(cat: String): ImageVector {
    return when (cat) {
        "Restaurant" -> Icons.Default.Restaurant
        "Nail Studio" -> Icons.Default.Spa
        "Hair Salon" -> Icons.Default.Face
        "Gym" -> Icons.Default.FitnessCenter
        "Yoga Studio" -> Icons.Default.SelfImprovement
        "Barbershop" -> Icons.Default.ContentCut
        else -> Icons.Default.ShoppingBag
    }
}

// --- MASTER LAYOUT COMPOSABLE ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainMarketplayLayout(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier
) {
    val isMerchantMode by viewModel.isMerchantMode.collectAsState()
    val activeClaimId by viewModel.selectedClaimId.collectAsState()
    val alertMessages by viewModel.alertMessages.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    // Map view tab toggle in Consumer screen to toggle layout dynamically
    var isShowingMapView by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header component
            AppHeaderSection(
                isMerchantMode = isMerchantMode,
                hasActiveProfile = profile?.hasCompletedSetup == true,
                onModeToggle = { merchant -> viewModel.setMerchantMode(merchant) }
            )

            // Alert banner system for incoming deal triggers & actions
            // Fixed length comparison
            val alertsPresent = alertMessages.isNotEmpty()
            AnimatedVisibility(
                visible = alertsPresent,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                alertMessages.firstOrNull()?.let { alertText ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("alert_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Alert",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = alertText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { viewModel.removeAlert(0) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Central Workspace Router
            Box(modifier = Modifier.weight(1f)) {
                if (isMerchantMode) {
                    val setupCompleted = profile?.hasCompletedSetup == true
                    if (!setupCompleted) {
                        MerchantSetupScreen(
                            onCompleteSetup = { bName, category, address ->
                                viewModel.completeMerchantSetup(bName, category, address)
                            }
                        )
                    } else {
                        MerchantDashboardScreen(
                            profile = profile!!,
                            historyDeals = viewModel.allDealsHistory.collectAsState().value,
                            claimedDeals = viewModel.claimedDeals.collectAsState().value,
                            onPostDeal = { offer, spots, hours ->
                                viewModel.postMerchantDeal(offer, spots, hours)
                            },
                            onRedeemCode = { code, onFinish ->
                                viewModel.redeemCode(code, onFinish)
                            },
                            onDisconnect = { viewModel.disconnectStripe() }
                        )
                    }
                } else {
                    // Consumer Suite
                    if (isShowingMapView) {
                        ConsumerMapLayout(
                            viewModel = viewModel,
                            onToggleFeed = { isShowingMapView = false }
                        )
                    } else {
                        ConsumerFeedScreen(
                            viewModel = viewModel,
                            onToggleMap = { isShowingMapView = true }
                        )
                    }
                }
            }
        }

        // Active Secured claim dynamic dialog modal
        if (activeClaimId != null) {
            val claims = viewModel.claimedDeals.collectAsState().value
            val currentClaim = claims.find { it.id == activeClaimId }
            if (currentClaim != null) {
                SecuredClaimDetailsDialog(
                    claim = currentClaim,
                    onDismiss = { viewModel.setSelectedClaimId(null) }
                )
            }
        }
    }
}

// --- HEADER COMPONENT ---

@Composable
fun AppHeaderSection(
    isMerchantMode: Boolean,
    hasActiveProfile: Boolean,
    onModeToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Branded Title with Location
                Column {
                    Text(
                        text = "DeadTime",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LOWER MANHATTAN • 1.5km",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Mode toggle badges
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val consumerLabelColor = if (!isMerchantMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    val consumerBgColor = if (!isMerchantMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    Text(
                        text = "Consumer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = consumerLabelColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(consumerBgColor)
                            .clickable { onModeToggle(false) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("consumer_mode_tab")
                    )
                    val merchantLabelColor = if (isMerchantMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    val merchantBgColor = if (isMerchantMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    Text(
                        text = "Business",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = merchantLabelColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(merchantBgColor)
                            .clickable { onModeToggle(true) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("merchant_mode_tab")
                    )
                }
            }
        }
    }
}

// --- CONSUMER SIDE: FEED SCREEN ---

@Composable
fun ConsumerFeedScreen(
    viewModel: MarketplaceViewModel,
    onToggleMap: () -> Unit
) {
    val deals by viewModel.filteredDeals.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val radiusKm by viewModel.radiusKm.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val claimsHistory by viewModel.claimedDeals.collectAsState()

    val categories = listOf("All", "Restaurant", "Nail Studio", "Hair Salon", "Gym", "Yoga Studio", "Barbershop")

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal filter elements and distance selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Radius control slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Deals Radius Walk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f km radius walk", radiusKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = radiusKm,
                    onValueChange = { viewModel.setRadius(it) },
                    valueRange = 1.0f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.height(24.dp)
                )

                // Category scroll filters
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        val isSel = cat == selectedCategory
                        val bgColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        val textColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        val borderColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(50))
                                .clickable { viewModel.selectCategory(cat) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Action Toolbar: Map Toggle & Claim History
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${deals.size} slow windows discounted nearby",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Toggle Map button
                Button(
                    onClick = onToggleMap,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map view switch",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interactive Map", fontSize = 11.sp, color = Color.Black)
                }

                // Saved dynamic claims badge
                val unredeemedClaims = claimsHistory.count { !it.scanned }
                if (unredeemedClaims > 0) {
                    FilledTonalButton(
                        onClick = {
                            val activeClaim = claimsHistory.find { !it.scanned }
                            if (activeClaim != null) {
                                viewModel.setSelectedClaimId(activeClaim.id)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active QR ($unredeemedClaims)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }

        // Feeds listing (or empty layout)
        if (deals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Empty deals list",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No active deals found in this coordinate",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Try expanding the radius slider or changing filters. New slow-hour offers are simulation-triggered regularly!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(deals, key = { it.id }) { deal ->
                    ConsumerDealCard(
                        deal = deal,
                        viewModel = viewModel,
                        currentTime = currentTime,
                        onClaim = {
                            viewModel.claimDeal(deal.id) {}
                        }
                    )
                }
            }
        }
    }
}

// --- CONSUMER CARD COMPONENT WITH GRAPHIC TIMER COUNTDOWNS ---

@Composable
fun ConsumerDealCard(
    deal: Deal,
    viewModel: MarketplaceViewModel,
    currentTime: Long,
    onClaim: () -> Unit
) {
    val durationLeft = max(0L, deal.endTime - currentTime)
    val hours = durationLeft / (1000 * 60 * 60)
    val minutes = (durationLeft / (1000 * 60)) % 60

    val isUrgent = durationLeft < 30 * 60 * 1000 // Less than 30 minutes left
    val isSoldOut = deal.spotsRemaining <= 0

    val borderColor = if (isUrgent && !isSoldOut) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deal_card_${deal.id}")
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .alpha(if (isSoldOut) 0.45f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = deal.businessName,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${deal.category} · ${viewModel.getFormattedDistance(deal)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                // Timer
                val timerBgColor = if (isSoldOut) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer
                val timerTextColor = if (isSoldOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(timerBgColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = String.format("%d:%02d", hours, minutes),
                        color = timerTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "left",
                        color = timerTextColor.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        lineHeight = 10.sp
                    )
                }
            }

            // Offer
            val words = deal.offer.split(" ")
            val firstPart = if (words.size > 2) words.take(2).joinToString(" ") else deal.offer
            val secondPart = if (words.size > 2) " " + words.drop(2).joinToString(" ") else ""
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = if (!isSoldOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))) {
                        append(firstPart)
                    }
                    if (secondPart.isNotEmpty()) {
                        append(secondPart)
                    }
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isSoldOut) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textDecoration = if (isSoldOut) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSoldOut) {
                    Text(
                        text = "Sold out",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 9.sp
                    )
                } else {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = DtSuccess, fontWeight = FontWeight.SemiBold)) {
                                append(deal.spotsRemaining.toString())
                            }
                            append(" spots left")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                if (!isSoldOut) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .clickable(onClick = onClaim)
                            .testTag("claim_btn")
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Claim",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

// --- CONSUMER SIDE: MAP VIEW ---

@Composable
fun ConsumerMapLayout(
    viewModel: MarketplaceViewModel,
    onToggleFeed: () -> Unit
) {
    val deals by viewModel.filteredDeals.collectAsState()
    var selectedMapDeal by remember { mutableStateOf<Deal?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Simple map header switch back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Map view of Lower Manhattan",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onToggleFeed,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Switch",
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Show Feed list", fontSize = 11.sp, color = Color.Black)
            }
        }

        // Custom drawn Canvas map representing surrounding NYC grid
        val mapBgColor = MaterialTheme.colorScheme.background
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val w = size.width
                val h = size.height

                // Draw central grids / cross streets representing New York Blocks
                val gridColor = DtSurfaceVariant
                // Horizontals
                for (i in 1..4) {
                    val y = h * (i / 5.0f)
                    drawLine(gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 8f)
                }
                // Verticals
                for (i in 1..4) {
                    val x = w * (i / 5.0f)
                    drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 8f)
                }

                // Draw Hudson park river block on left edge
                drawRect(
                    color = mapBgColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(w * 0.15f, h)
                )

                // 2. Draw active consumer pin in center
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = 48f,
                    center = Offset(w / 2, h / 2)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 12f,
                    center = Offset(w / 2, h / 2)
                )
            }

            // Real-time floating deal pins overlaid on the street grid
            deals.forEachIndexed { _, deal ->
                // Map latitude/longitude offsets around user coordinate (40.7128, -74.0060)
                // Map space is roughly [40.705 to 40.720] for latitude, and [-74.015 to -73.995] for longitude
                val scaleLat = (deal.latitude - 40.7128) / 0.015
                val scaleLng = (deal.longitude - (-74.0060)) / 0.020

                // Center is (0.5, 0.5)
                val xFraction = 0.5f + scaleLng.toFloat()
                val yFraction = 0.5f - scaleLat.toFloat() // invert y axis, lat goes up

                // Constrain values within map container padding
                val xClamped = xFraction.coerceIn(0.18f, 0.88f)
                val yClamped = yFraction.coerceIn(0.12f, 0.88f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 12.dp)
                ) {
                    val paddingX = (xClamped * 280).dp
                    val paddingY = (yClamped * 350).dp

                    Card(
                        onClick = { selectedMapDeal = deal },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = paddingX, y = paddingY),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMapDeal?.id == deal.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(deal.category),
                                contentDescription = null,
                                tint = if (selectedMapDeal?.id == deal.id) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${deal.spotsRemaining} Spot",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMapDeal?.id == deal.id) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Float panel summarizing selected marker
        AnimatedVisibility(
            visible = selectedMapDeal != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            selectedMapDeal?.let { deal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(deal.category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    deal.businessName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = { selectedMapDeal = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                            }
                        }

                        Text(
                            text = deal.offer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${deal.spotsRemaining} spots left • ${viewModel.getFormattedDistance(deal)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    viewModel.claimDeal(deal.id) {}
                                    selectedMapDeal = null
                                },
                                shape = RoundedCornerShape(30.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Claim Deal", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ACTIVE SECURED CLAIM DETAILS DIALOG ---

@Composable
fun SecuredClaimDetailsDialog(
    claim: ClaimedDeal,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("claimed_details"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (claim.scanned) Color.Gray else Color.Green, CircleShape)
                        )
                        Text(
                            text = if (claim.scanned) "REDEEMED SUCCESSFULLY" else "SECURED & ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (claim.scanned) MaterialTheme.colorScheme.onSurfaceVariant else Color.Green,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }

                // Promo outline
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = claim.businessName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = claim.offer,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.2).sp
                    )
                }

                // Procedural single-use QR Code Canvas drawing
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (claim.scanned) {
                        // Blurred out with checkmark indicating scan processed
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Scanned",
                                    tint = DtSuccess,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    "REDEEMED",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DtSuccess,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        // Fully active dynamic QR Code
                        ProceduralQRCode(claimId = claim.id, modifier = Modifier.fillMaxSize())
                    }
                }

                // Secure ID display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "SINGLE-USE REDEMPTION CODE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = claim.id,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Redemption steps guidance
                Text(
                    text = if (claim.scanned) {
                        "This transaction was successfully verified. Stripe Connect direct payouts are processed to your balance."
                    } else {
                        "Show this digital ticket to the checkout representative at the business. They will input or scan it to instant-validate in their console!"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center
                )

                if (!claim.scanned) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Keep QR Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// --- BUSINESS SIDE: PORTAL SETUP ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MerchantSetupScreen(
    onCompleteSetup: (bName: String, category: String, address: String) -> Unit
) {
    var brandName by remember { mutableStateOf("") }
    var selectCategory by remember { mutableStateOf("Restaurant") }
    var brandAddress by remember { mutableStateOf("") }

    var isStripeConnecting by remember { mutableStateOf(false) }
    var hasConnectedStripe by remember { mutableStateOf(false) }

    val merchantCategories = listOf("Restaurant", "Nail Studio", "Hair Salon", "Gym", "Yoga Studio", "Barbershop")

    val prefilledAddresses = listOf(
        "120 Broadway, New York, NY",
        "42 Spring St, New York, NY",
        "189 Orchard St, New York, NY",
        "56 Broad St, New York, NY"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("merchant_setup_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Welcome to the Business Suite",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Onboard your merchant account in under 4 minutes to instantly monetize slow-hours capacity.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Basic configuration fields
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "1. Business Profile Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = brandName,
                        onValueChange = { brandName = it },
                        label = { Text("Business Brand Name") },
                        placeholder = { Text("e.g., Tony's Pizza") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Horizontal Category selecting pills
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Business Service Category",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            maxItemsInEachRow = 3
                        ) {
                            merchantCategories.forEach { cat ->
                                val isChosen = selectCategory == cat
                                FilterChip(
                                    selected = isChosen,
                                    onClick = { selectCategory = cat },
                                    label = { Text(cat, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // Address selector
                    OutlinedTextField(
                        value = brandAddress,
                        onValueChange = { brandAddress = it },
                        label = { Text("Business Standard address") },
                        placeholder = { Text("Type address or tap suggestions below") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        prefilledAddresses.take(2).forEach { addr ->
                            FilledTonalButton(
                                onClick = { brandAddress = addr },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(addr.substringBefore(","), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Stripe Connect Linker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "2. Payout Integrations",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Connect bank checking account link via Stripe Connect. Platform transaction fee is 4.5%. First 30 redemptions have absolutely 0% commission fees.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hasConnectedStripe) {
                        Button(
                            onClick = {
                                isStripeConnecting = true
                                hasConnectedStripe = false
                            },
                            enabled = !isStripeConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF)), // Stripe Brand Purple
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            if (isStripeConnecting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting Stripe API...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect Payouts via Stripe Connect", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isStripeConnecting) {
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1200) // simulated loading redirect
                                isStripeConnecting = false
                                hasConnectedStripe = true
                            }
                        }
                    } else {
                        // Connected success state
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DtSuccess.copy(alpha=0.1f)),
                            border = BorderStroke(1.dp, DtSuccess.copy(alpha=0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success sign",
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("Stripe Gateway Connected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                    Text("Express Account ID: acct_1Nzk9M", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Complete Setup final trigger
        item {
            Button(
                onClick = {
                    onCompleteSetup(brandName, selectCategory, brandAddress)
                },
                enabled = brandName.isNotBlank() && brandAddress.isNotBlank() && hasConnectedStripe,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_setup_btn")
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Launch", modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Onboard & Activate suite", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            if (!(brandName.isNotBlank() && brandAddress.isNotBlank() && hasConnectedStripe)) {
                Text(
                    text = "* Fill brand name, address, and connect your Stripe bank account above to activate screen.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- BUSINESS SIDE: REAL-TIME DASHBOARD & METRICS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MerchantDashboardScreen(
    profile: com.example.data.BusinessProfile,
    historyDeals: List<Deal>,
    claimedDeals: List<ClaimedDeal>,
    onPostDeal: (offer: String, spots: String, hours: String) -> Unit,
    onRedeemCode: (code: String, (Boolean) -> Unit) -> Unit,
    onDisconnect: () -> Unit
) {
    // Input states for custom manual posts
    var postOfferMessage by remember { mutableStateOf("") }
    var postSpotsCount by remember { mutableStateOf("8") }
    var postDurationHrs by remember { mutableStateOf("2.0") }

    // Input state for redemption scan debugger console
    var redemptionManualCodeInput by remember { mutableStateOf("") }
    var isCheckingRedemption by remember { mutableStateOf(false) }

    // Preset selection helper
    val prefilledTemplates = listOf(
        "30% off any midday entry/service" to Pair("6", "2.0"),
        "40% off entry + bonus perk" to Pair("10", "1.5"),
        "Buy-One-Get-One slower entry spot" to Pair("5", "3.0")
    )

    // Compute aggregated dynamic overview analytics for current merchant
    val currentBrandDeals = historyDeals.filter { it.businessName == profile.businessName }
    val totalViews = currentBrandDeals.sumOf { it.viewsCount }
    val totalClaims = currentBrandDeals.sumOf { it.claimsCount }
    val totalRedeemedScans = currentBrandDeals.sumOf { it.scansCount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("merchant_dashboard_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Merchant Welcome header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard for ${profile.businessName}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Category: ${profile.category} • Stripe Connected",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Logout profile",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Live metrics analytics block layout grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Real-time Metrics Tracking (Auto-refresh)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Views Metric
                    MetricItemCard(
                        title = "CONSUMER VIEWS",
                        value = totalViews.toString(),
                        tintColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    // Claims Metric
                    MetricItemCard(
                        title = "CLAIMS GATHERED",
                        value = totalClaims.toString(),
                        tintColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Scans/Redeemed Metric
                    MetricItemCard(
                        title = "QRCODES SCANNED",
                        value = totalRedeemedScans.toString(),
                        tintColor = Color.Green,
                        modifier = Modifier.weight(1f)
                    )

                    // Stripe Weekly earnings balance
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, Color(0xFF635BFF))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "STRIPE CONNECT WEEKLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF635BFF)
                            )
                            Text(
                                text = String.format("$%.2f", profile.weeklyPayoutEarned),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Green
                            )
                            Text(
                                "Next standard payout in 2 days",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3-Tap Deal Poster Wizard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3-Tap Quick-Post Flash Deal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Preset Templates (Click inserts)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        prefilledTemplates.forEachIndexed { i, template ->
                            val isSelected = postOfferMessage == template.first
                            val templateBtnColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                            val labelColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface

                            Card(
                                onClick = {
                                    postOfferMessage = template.first
                                    postSpotsCount = template.second.first
                                    postDurationHrs = template.second.second
                                },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = templateBtnColor),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "Template ${i + 1}\n" + template.first.substringBefore(" "),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = labelColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = postOfferMessage,
                        onValueChange = { postOfferMessage = it },
                        label = { Text("What is the slow-hour offer?") },
                        placeholder = { Text("e.g., 30% off any main service / entry") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = postSpotsCount,
                            onValueChange = { postSpotsCount = it },
                            label = { Text("Spot available") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = postDurationHrs,
                            onValueChange = { postDurationHrs = it },
                            label = { Text("Duration hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Post trigger
                    Button(
                        onClick = {
                            onPostDeal(postOfferMessage, postSpotsCount, postDurationHrs)
                            postOfferMessage = ""
                        },
                        enabled = postOfferMessage.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("post_deal_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Live Flash Deal", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // QR Redemption SCAN Verification Console debug
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Local QR Redemption Verification",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Input or select an active user claimed code below to run validation, process checkout redemption, and push funds directly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Helper list of active claims in database to let business instantly select and scan
                    val activeClaimsList = claimedDeals.filter { !it.scanned }
                    if (activeClaimsList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Live claims available nearby to simulated verify (Tap to insert):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeClaimsList) { claim ->
                                    Card(
                                        onClick = { redemptionManualCodeInput = claim.id },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(claim.id, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Text(claim.businessName, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "* No unredeemed consumer claims in the database yet. Go back to 'Consumer Mode', claim a deal, and it will list here for scanning verification!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = redemptionManualCodeInput,
                        onValueChange = { redemptionManualCodeInput = it },
                        label = { Text("Claim QR alphanumeric value") },
                        placeholder = { Text("e.g., CLAIM_1_XYZ") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                    )

                    Button(
                        onClick = {
                            isCheckingRedemption = true
                            onRedeemCode(redemptionManualCodeInput) { success ->
                                isCheckingRedemption = false
                                if (success) {
                                    redemptionManualCodeInput = ""
                                }
                            }
                        },
                        enabled = redemptionManualCodeInput.isNotBlank() && !isCheckingRedemption,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (isCheckingRedemption) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying token with server...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan & Validate Redemption Ticket", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Auto re-engagement automated template triggers
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Re-Engagement Prompt (7-days idle)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Your business last generated $240 during slow windows. Reposting the same template will forecast $184 of idle occupancy revenue today.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            onPostDeal("35% off any large pizza", "12", "2.0")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "One tap repost", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-Tap Repost Last Tuesday Offer", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// --- SUB LEVEL COMPONENT METRIC ITEM ---

@Composable
fun MetricItemCard(
    title: String,
    value: String,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = title,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
