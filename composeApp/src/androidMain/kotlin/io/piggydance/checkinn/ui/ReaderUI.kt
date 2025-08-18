package io.piggydance.checkinn.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.piggydance.checkinn.nfc.NFCResult
import io.piggydance.checkinn.nfc.NFCStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReaderUI(
    nfcStatus: NFCStatus,
    lastReadResult: NFCResult?,
    readHistory: List<NFCResult>,
    onReadAgain: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val statusColor = when (nfcStatus) {
            NFCStatus.ENABLED -> Color.Green
            NFCStatus.DISABLED -> Color.Red
            NFCStatus.NOT_SUPPORTED -> Color.Gray
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("NFC Status", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(nfcStatus.name, color = statusColor, fontSize = 16.sp)
                Text(
                    when (nfcStatus) {
                        NFCStatus.ENABLED -> "NFC is ready to use"
                        NFCStatus.DISABLED -> "Please enable NFC in settings"
                        NFCStatus.NOT_SUPPORTED -> "This device does not support NFC"
                    },
                    fontSize = 14.sp
                )
            }
        }

        lastReadResult?.let {
            Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last Read Result", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Status: ${if (it.success) "Success" else "Failed"}")

                    // 解析卡片类型
                    val (cardType, payload) = it.data?.let { raw ->
                        val split = raw.split('|', limit = 2)
                        if (split.size == 2) {
                            try {
                                CardType.valueOf(split[0]) to split[1]
                            } catch (_: Exception) {
                                CardType.OTHER to raw
                            }
                        } else CardType.OTHER to raw
                    } ?: (CardType.OTHER to "")
                    Text("卡片类型: ${cardType.displayName}")

                    it.tagId?.let { id -> Text("Tag ID: $id") }
                    it.tagType?.let { type -> Text("Tag Type: $type") }
                    if (payload.isNotEmpty()) Text("Payload: $payload")
                    it.errorMessage?.let { err -> Text("Error: $err", color = Color.Red) }
                    Text(
                        "Time: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}",
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (readHistory.isNotEmpty()) {
            Text(
                "Reading History",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            readHistory.forEachIndexed { idx, res ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Record ${idx + 1}")
                        Text("Tag ID: ${res.tagId ?: "N/A"}")
                        Text("Type: ${res.tagType ?: "N/A"}")
                        Text("Status: ${if (res.success) "Success" else "Failed"}")
                    }
                }
            }
        }

        Button(onClick = onReadAgain) { Text("Read Again") }
    }
}
