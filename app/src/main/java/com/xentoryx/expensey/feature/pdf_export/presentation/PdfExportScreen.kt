package com.xentoryx.expensey.feature.pdf_export.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportScreen(
    viewModel: PdfExportViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isDatePickerOpen by remember { mutableStateOf(false) }
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var renderError by remember { mutableStateOf<String?>(null) }

    // Render PDF to Bitmaps when pdfBytes changes
    LaunchedEffect(state.pdfBytes) {
        val bytes = state.pdfBytes
        if (bytes != null) {
            try {
                renderError = null
                withContext(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "temp_render_report.pdf").apply {
                        writeBytes(bytes)
                    }
                    val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(fileDescriptor)
                    val renderedBitmaps = mutableListOf<Bitmap>()
                    
                    for (i in 0 until pdfRenderer.pageCount) {
                        val page = pdfRenderer.openPage(i)
                        
                        // Render at 2x scale for premium crispness
                        val scale = 2f
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        renderedBitmaps.add(bitmap)
                        
                        page.close()
                    }
                    
                    pdfRenderer.close()
                    fileDescriptor.close()
                    
                    withContext(Dispatchers.Main) {
                        pdfPages = renderedBitmaps
                    }
                }
            } catch (e: Exception) {
                renderError = "Failed to render PDF preview: ${e.message}"
            }
        } else {
            pdfPages = emptyList()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Export Report",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (state.exportFormat == "PDF" && state.pdfBytes != null && pdfPages.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        sharePdf(context, state.pdfBytes!!, state.startDateMillis!!, state.endDateMillis!!)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CrushCanvasDecoration(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input panel: only visible if report is NOT loaded
                val isReportGenerated = (state.exportFormat == "PDF" && state.pdfBytes != null) ||
                        (state.exportFormat == "CSV" && state.csvBytes != null)

                if (!isReportGenerated && !state.isLoading) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Select Report Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "Choose the format and start/end dates to generate your beautiful transaction statement.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Select Format",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("PDF", "CSV").forEach { format ->
                            val selected = state.exportFormat == format
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectExportFormat(format) },
                                label = { Text(format, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    DateSelectionCard(
                        startDate = state.startDateMillis,
                        endDate = state.endDateMillis,
                        onClick = { isDatePickerOpen = true }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.generateReport() },
                        enabled = state.startDateMillis != null && state.endDateMillis != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = "Generate ${state.exportFormat} Report",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }

                // Loading State
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Crossfade(
                                targetState = state.loadingMessage,
                                animationSpec = tween(durationMillis = 500)
                            ) { message ->
                                Text(
                                    text = message,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Please wait, do not close the screen.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Rerender or loading error
                renderError?.let { err ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = err, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }

                // PDF Viewer
                if (state.exportFormat == "PDF" && state.pdfBytes != null && pdfPages.isNotEmpty() && !state.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generated Report Preview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Text(
                            text = "Change range",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.clearPdf()
                                isDatePickerOpen = true
                            }
                        )
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(pdfPages) { bitmap ->
                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "PDF Page",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                // CSV Success Screen
                if (state.exportFormat == "CSV" && state.csvBytes != null && !state.isLoading) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "CSV Ready",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Spreadsheet Generated Successfully!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your transactions are exported into a standard CSV format which you can open in Excel, Google Sheets, or share directly.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Button(
                        onClick = {
                            shareCsv(context, state.csvBytes!!, state.startDateMillis!!, state.endDateMillis!!)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share CSV File", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearPdf() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Change Range / Format")
                    }
                }
            }
        }
    }

    if (isDatePickerOpen) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = datePickerState.selectedStartDateMillis
                        val end = datePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.onDateRangeSelected(start, end)
                            isDatePickerOpen = false
                        } else {
                            Toast.makeText(context, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Select date range for report", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DateSelectionCard(
    startDate: Long?,
    endDate: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    val dateText = if (startDate != null && endDate != null) {
        val startStr = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
        val endStr = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
        "$startStr - $endStr"
    } else {
        "Tap to select date range"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = "Report Duration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = if (startDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (startDate != null) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun sharePdf(context: Context, bytes: ByteArray, startDate: Long, endDate: Long) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startStr = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    val endStr = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    
    try {
        val file = File(context.cacheDir, "Expensey_Report_${startStr}_to_${endStr}.pdf")
        file.writeBytes(bytes)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Expensey Financial Report ($startStr to $endStr)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share PDF Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving report file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareCsv(context: Context, bytes: ByteArray, startDate: Long, endDate: Long) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startStr = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    val endStr = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    
    try {
        val file = File(context.cacheDir, "Expensey_Report_${startStr}_to_${endStr}.csv")
        file.writeBytes(bytes)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Expensey Financial Report ($startStr to $endStr)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share CSV Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving report file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
