package io.piggydance.checkinn

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.piggydance.checkinn.nfc.NFCReader
import io.piggydance.checkinn.nfc.NFCReaderListener
import io.piggydance.checkinn.nfc.NFCResult
import io.piggydance.checkinn.nfc.NFCStatus
import io.piggydance.checkinn.nfc.NFCWriter
import io.piggydance.checkinn.nfc.WriteDataType
import io.piggydance.checkinn.ui.ReaderUI
import io.piggydance.checkinn.ui.WriterUI

class MainActivity : ComponentActivity(), NFCReaderListener {
    private val TAG = "MainActivity"
    private lateinit var nfcReader: NFCReader
    private var nfcStatus by mutableStateOf(NFCStatus.NOT_SUPPORTED)
    private var lastReadResult by mutableStateOf<NFCResult?>(null)
    private var readHistory by mutableStateOf<List<NFCResult>>(emptyList())

    private lateinit var nfcWriter: NFCWriter
    private var writeMode by mutableStateOf(false)
    private var writeDataType by mutableStateOf(WriteDataType.TEXT)
    private var writeContent by mutableStateOf(TextFieldValue(""))
    private var writeResult by mutableStateOf<String?>(null)


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 初始化NFCReader
        nfcReader = NFCReader.getInstance()
        nfcReader.initialize(this)
        nfcReader.addListener(this)

        // 初始化NFCWriter
        nfcWriter = NFCWriter()

        setContent {
            MainScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nfcReader.enableForegroundDispatch(this)
        }
        // 处理可能的NFC intent
        processIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nfcReader.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 检查是否处于写入模式
        if (writeMode && writeContent.text.isNotEmpty()) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                // 执行写入操作
                val success = when (writeDataType) {
                    WriteDataType.TEXT -> nfcWriter.writeText(tag, writeContent.text)
                    WriteDataType.URI -> nfcWriter.writeUri(tag, writeContent.text)
                    WriteDataType.APP_LAUNCH -> nfcWriter.writeAppLaunch(tag, writeContent.text, "EXERCISE")
                    WriteDataType.BLUETOOTH -> nfcWriter.writeBluetooth(tag, writeContent.text)
                    WriteDataType.CUSTOM -> false // 自定义NDEF消息需要特殊处理
                }

                writeResult = if (success) {
                    "写入成功!"
                } else {
                    "写入失败，请重试"
                }

                // 退出写入模式
                writeMode = false
            }
        } else {
            // 正常读取模式
            processIntent(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcReader.removeListener(this)
    }

    private fun processIntent(intent: Intent) {
        val action = intent.action
        Log.d(TAG, "NFC intent received, action: $action")
        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                if (rawMsgs != null) {
                    val msgs = rawMsgs.map { it as NdefMessage }
                    for (msg in msgs) {
                        for (record in msg.records) {
                            if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                                record.type.contentEquals(NdefRecord.RTD_URI)) {

                                // 解析 URI
                                val uri = record.toUri()
                                Log.d("NFC", "Scanned URI = $uri")

                                // 拿参数
                                uri?.getQueryParameter("userId")?.let { userId ->
                                    Log.d("NFC", "userId = $userId")
                                }
                            }
                        }
                    }
                }
            }

            val result = nfcReader.readTag(intent)
            // 这里会通过NFCReaderListener回调更新UI

        }
    }

    override fun onNFCStatusChanged(status: NFCStatus) {
        Log.d(TAG, "NFC status changed: $status")
        nfcStatus = status
    }

    override fun onTagDetected(result: NFCResult) {
        Log.d(TAG, "Tag detected: ${result.tagId}")
        lastReadResult = result
        // 添加到历史记录
        readHistory = listOf(result) + readHistory.take(4) // 只保留最近5条记录
    }

    override fun onError(error: Throwable) {
        Log.e(TAG, "NFC error: ${error.message}", error)
        // 可以在这里显示错误信息
    }

    private fun writeTagToNFC() {
        // 这个方法将在用户将标签靠近设备时被调用
        // 实际的写入操作将在onNewIntent中处理
        if (writeMode && writeContent.text.isNotEmpty()) {
            // 启用前台调度以检测NFC标签
            enableWriteForegroundDispatch()
        }
    }
    private fun enableWriteForegroundDispatch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(this, this.javaClass).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_MUTABLE
                )

                val filters = arrayOf(
                    IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                )

                val techList = arrayOf(
                    arrayOf("android.nfc.tech.Ndef"),
                    arrayOf("android.nfc.tech.NdefFormatable")
                )

                nfcReader.enableForegroundDispatch(this) // 临时禁用读取器的前台调度
                // 使用NFC适配器启用写入模式的前台调度
                NfcAdapter.getDefaultAdapter(this)?.enableForegroundDispatch(
                    this, pendingIntent, filters, techList
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable write foreground dispatch", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun MainScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "NFC Reader & Writer",
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("写入模式", fontSize = 18.sp)
                Switch(checked = writeMode, onCheckedChange = { writeMode = it })
            }

            if (writeMode) {
                WriterUI(
                    writeDataType = writeDataType,
                    writeContent = writeContent,
                    writeResult = writeResult,
                    onWriteDataTypeChange = { writeDataType = it },
                    onWriteContentChange = { writeContent = it },
                    onWriteTag = { payload ->
                        writeContent = writeContent.copy(text = payload) // 存到 writeContent 中供 onNewIntent 使用
                        writeTagToNFC()
                    },
                    onClearWriteResult = { writeResult = null }
                )
            } else {
                ReaderUI(
                    nfcStatus = nfcStatus,
                    lastReadResult = lastReadResult,
                    readHistory = readHistory,
                    onReadAgain = { processIntent(intent) }
                )
            }
        }
    }

}
