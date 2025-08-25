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

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent { App() }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            nfcReader.enableForegroundDispatch(this)
        }
        processIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            nfcReader.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
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
                                uri?.getQueryParameter("s")?.let { scene ->
                                    Log.d("NFC", "uri.host = ${uri.host}, path = ${uri.path}, scene = $scene")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
