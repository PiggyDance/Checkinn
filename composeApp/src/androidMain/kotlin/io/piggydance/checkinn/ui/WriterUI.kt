package io.piggydance.checkinn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.piggydance.checkinn.nfc.WriteDataType

enum class CardType(val displayName: String) {
    EXERCISE("运动打卡"),
    STUDY("学习打卡"),
    OTHER("其它")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriterUI(
    writeDataType: WriteDataType,
    writeContent: TextFieldValue,
    writeResult: String?,
    onWriteDataTypeChange: (WriteDataType) -> Unit,
    onWriteContentChange: (TextFieldValue) -> Unit,
    onWriteTag: (String) -> Unit,               // 改成把完整内容传出去
    onClearWriteResult: () -> Unit
) {
    var selectedCardType by remember { mutableStateOf(CardType.EXERCISE) }
    var expandedType by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("选择卡片类型", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = selectedCardType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedType) }
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    CardType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedCardType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 2. 原来数据类型/内容输入保持不变 ...
            // 3. 写入按钮：把卡片类型也拼到内容里
            Button(
                onClick = {
                    val payload = "${selectedCardType.name}|${writeContent.text}"
                    onWriteTag(payload)
                },
                enabled = writeContent.text.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("写入到NFC标签")
            }
            Text("选择写入数据类型", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = when (writeDataType) {
                        WriteDataType.TEXT -> "文本"
                        WriteDataType.URI -> "网址"
                        WriteDataType.APP_LAUNCH -> "应用启动"
                        WriteDataType.BLUETOOTH -> "蓝牙配对"
                        WriteDataType.CUSTOM -> "自定义"
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),   // 3. 关键：添加 menuAnchor()
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    WriteDataType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (type) {
                                        WriteDataType.TEXT -> "文本"
                                        WriteDataType.URI -> "网址"
                                        WriteDataType.APP_LAUNCH -> "应用启动"
                                        WriteDataType.BLUETOOTH -> "蓝牙配对"
                                        WriteDataType.CUSTOM -> "自定义"
                                    }
                                )
                            },
                            onClick = {
                                onWriteDataTypeChange(type)
                                expanded = false
                            }
                        )
                    }
                }
            }


            OutlinedTextField(
                value = writeContent,
                onValueChange = onWriteContentChange,
                label = {
                    Text(
                        when (writeDataType) {
                            WriteDataType.TEXT -> "输入文本内容"
                            WriteDataType.URI -> "输入网址 (如: https://www.example.com)"
                            WriteDataType.APP_LAUNCH -> "输入应用包名 (如: com.example.app)"
                            WriteDataType.BLUETOOTH -> "输入蓝牙MAC地址 (如: 00:11:22:33:44:55)"
                            WriteDataType.CUSTOM -> "输入自定义内容"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                maxLines = 3
            )

            Button(
                onClick = { onWriteTag("${selectedCardType.name}|${writeContent.text}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                enabled = writeContent.text.isNotEmpty()
            ) {
                Text("写入到NFC标签")
            }

            writeResult?.let { res ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.contains("成功")) Color.Green else Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(res, color = Color.White)
                        IconButton(onClick = onClearWriteResult) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                        }
                    }
                }
            }

            Text(
                "请将NFC标签靠近设备以写入数据",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
