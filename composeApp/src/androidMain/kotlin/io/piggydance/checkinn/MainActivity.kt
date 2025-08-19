package io.piggydance.checkinn

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import io.piggydance.checkinn.nfc.NFCReader
import io.piggydance.checkinn.nfc.NFCReaderListener
import io.piggydance.checkinn.nfc.NFCResult
import io.piggydance.checkinn.nfc.NFCStatus
import io.piggydance.checkinn.ui.ReaderUI
import io.piggydance.checkinn.viewmodel.NFCViewModel

class MainActivity : ComponentActivity(), NFCReaderListener {
    private val TAG = "MainActivity"
    private lateinit var nfcReader: NFCReader
    private lateinit var viewModel: NFCViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[NFCViewModel::class.java]

        // 初始化 NFCReader
        nfcReader = NFCReader.getInstance()
        nfcReader.initialize(this)
        nfcReader.addListener(this)

        setContent { MainScreen() }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nfcReader.enableForegroundDispatch(this)
        }
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

            nfcReader.readTag(intent)
            // 这里会通过 NFCReaderListener 回调更新 UI

        }
    }

    override fun onNFCStatusChanged(status: NFCStatus) {
        Log.d(TAG, "NFC status changed: $status")
        viewModel.updateNFCStatus(status)
    }

    override fun onTagDetected(result: NFCResult) {
        Log.d(TAG, "Tag detected: ${result.tagId}")
        viewModel.onTagDetected(result)
    }

    override fun onError(error: Throwable) {
        Log.e(TAG, "NFC error: ${error.message}", error)
        // 可以在这里显示错误信息
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    private fun MainScreen() {
        // 观察 ViewModel 中的数据
        val nfcStatus by viewModel.nfcStatus.observeAsState()
        val lastReadResult by viewModel.lastReadResult.observeAsState()
        val readHistory by viewModel.readHistory.observeAsState(emptyList())

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

            nfcStatus?.let {
                ReaderUI(
                    nfcStatus = it,
                    lastReadResult = lastReadResult,
                    readHistory = readHistory,
                    readCount = viewModel.readCount.value ?: 0,
                    onReadAgain = { processIntent(intent) }
                )
            }
        }
    }
}
