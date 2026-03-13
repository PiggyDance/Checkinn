package io.piggydance.checkinn

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private lateinit var nfcHelper: NfcHelper
    private lateinit var viewModel: CheckinnViewModel
    private lateinit var storage: CheckinnStorage
    private lateinit var settingsStorage: AndroidCheckinnSettingsStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 初始化应用上下文用于字符串资源
        initializeContext(this)
        
        nfcHelper = NfcHelper(this)
        storage = CheckinnStorage(this)
        settingsStorage = AndroidCheckinnSettingsStorage(this)
        viewModel = ViewModelProvider(this)[CheckinnViewModel::class.java]
        viewModel.initialize(storage, settingsStorage)

        setContent {
            App(viewModel = viewModel)
        }

        // 处理 App 通过 NFC 标签启动的情况
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        Log.d(TAG, "handleIntent action=$action")

        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED) {
            return
        }

        val uiState = viewModel.uiState.value

        // 如果处于写入模式, 优先进行写入
        if (uiState.isWriteMode && uiState.writeScene != null) {
            if (nfcHelper.hasTag(intent)) {
                val success = nfcHelper.writeTag(intent, uiState.writeScene)
                viewModel.onWriteComplete(success)
                if (success) {
                    Toast.makeText(this, "NFC 贴纸写入成功！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "写入失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        // 正常读取模式: 解析NFC标签并触发打卡
        val scene = nfcHelper.parseScene(intent)
        if (scene != null) {
            Log.d(TAG, "NFC scene detected: ${scene.key}")
            viewModel.onNfcScanned(scene)
        } else {
            Log.d(TAG, "Unknown NFC tag, not a checkinn tag")
            Toast.makeText(this, "无法识别的NFC标签", Toast.LENGTH_SHORT).show()
        }

        // 清理 intent 避免 onResume 重复处理
        setIntent(Intent())
    }
}
