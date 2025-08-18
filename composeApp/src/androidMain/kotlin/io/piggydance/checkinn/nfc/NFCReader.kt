package io.piggydance.checkinn.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * NFC读取器接口
 */
interface INFCReader {
    fun initialize(context: Context)
    fun enableForegroundDispatch(activity: Activity)
    fun disableForegroundDispatch(activity: Activity)
    fun readTag(intent: Intent): NFCResult
    fun addListener(listener: NFCReaderListener)
    fun removeListener(listener: NFCReaderListener)
    fun isNfcAvailable(): Boolean
    fun getNfcStatus(): NFCStatus
}

/**
 * NFC状态枚举
 */
enum class NFCStatus {
    DISABLED, // NFC已禁用
    ENABLED,  // NFC已启用
    NOT_SUPPORTED // 设备不支持NFC
}

/**
 * NFC结果数据类
 */
data class NFCResult(
    val success: Boolean,
    val tagId: String? = null,
    val tagType: String? = null,
    val data: String? = null,
    val errorMessage: String? = null
)

/**
 * NFC读取器监听器接口
 */
interface NFCReaderListener {
    fun onNFCStatusChanged(status: NFCStatus)
    fun onTagDetected(result: NFCResult)
    fun onError(error: Throwable)
}

/**
 * NFC标签处理策略接口
 */
interface TagHandlerStrategy {
    fun handleTag(tag: android.nfc.Tag): NFCResult
    fun supportsTag(tag: android.nfc.Tag): Boolean
}

/**
 * ISO-DEP标签处理策略
 */
class IsoDepTagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val isoDep = IsoDep.get(tag)
            isoDep.connect()
            // 示例: 发送SELECT APDU命令
            val command = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0xD2.toByte(), 0x76, 0x00, 0x00,
                0x85.toByte(), 0x01, 0x01)
            val response = isoDep.transceive(command)
            isoDep.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "ISO-DEP", response.joinToString(" ") { "%02X".format(it) })
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read ISO-DEP tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(IsoDep::class.java.name)
    }
}

/**
 * Mifare Classic标签处理策略
 */
class MifareClassicTagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val mifare = MifareClassic.get(tag)
            mifare.connect()
            val sectorCount = mifare.sectorCount
            mifare.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "Mifare Classic", "Sectors: $sectorCount")
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read Mifare Classic tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(MifareClassic::class.java.name)
    }
}

/**
 * Mifare Ultralight标签处理策略
 */
class MifareUltralightTagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val mifare = MifareUltralight.get(tag)
            mifare.connect()
            val type = when (mifare.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                else -> "Unknown"
            }
            mifare.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "Mifare Ultralight ($type)", null)
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read Mifare Ultralight tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(MifareUltralight::class.java.name)
    }
}

/**
 * 通用NFC-A标签处理策略
 */
class NfcATagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val nfcA = NfcA.get(tag)
            nfcA.connect()
            val atqa = nfcA.atqa
            val sak = nfcA.sak
            nfcA.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "NFC-A", "ATQA: ${atqa.joinToString(" ") { "%02X".format(it) }}, SAK: ${sak}")
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read NFC-A tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(NfcA::class.java.name)
    }
}

/**
 * 通用NFC-B标签处理策略
 */
class NfcBTagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val nfcB = NfcB.get(tag)
            nfcB.connect()
            val applicationData = nfcB.applicationData
            val protocolInfo = nfcB.protocolInfo
            nfcB.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "NFC-B", "Application Data: ${applicationData.joinToString(" ") { "%02X".format(it) }}, Protocol Info: ${protocolInfo.joinToString(" ") { "%02X".format(it) }}")
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read NFC-B tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(NfcB::class.java.name)
    }
}

/**
 * 通用NFC-F标签处理策略
 */
class NfcFTagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val nfcF = NfcF.get(tag)
            nfcF.connect()
            val manufacturingData = nfcF.manufacturer
            val systemCode = nfcF.systemCode
            nfcF.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "NFC-F", "Manufacturing Data: ${manufacturingData?.joinToString(" ") { "%02X".format(it) }}, System Code: ${systemCode.joinToString(" ") { "%02X".format(it) }}")
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read NFC-F tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(NfcF::class.java.name)
    }
}

/**
 * 通用NFC-V标签处理策略
 */
class NfcVTagHandler : TagHandlerStrategy {
    override fun handleTag(tag: android.nfc.Tag): NFCResult {
        return try {
            val nfcV = NfcV.get(tag)
            nfcV.connect()
            val dsfId = nfcV.dsfId
            val responseFlags = nfcV.responseFlags
            nfcV.close()
            NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "NFC-V", "DSF ID: ${dsfId}, Response Flags: ${responseFlags}")
        } catch (e: IOException) {
            NFCResult(false, errorMessage = "Failed to read NFC-V tag: ${e.message}")
        }
    }

    override fun supportsTag(tag: android.nfc.Tag): Boolean {
        return tag.techList.contains(NfcV::class.java.name)
    }
}

/**
 * NFC读取器实现类
 * 采用单例模式和观察者模式设计
 */
class NFCReader private constructor() : INFCReader {
    private var nfcAdapter: NfcAdapter? = null
    private var context: Context? = null
    private val listeners = CopyOnWriteArrayList<NFCReaderListener>()
    private val tagHandlers = listOf(
        IsoDepTagHandler(),
        MifareClassicTagHandler(),
        MifareUltralightTagHandler(),
        NfcATagHandler(),
        NfcBTagHandler(),
        NfcFTagHandler(),
        NfcVTagHandler()
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentStatus: NFCStatus = NFCStatus.NOT_SUPPORTED

    companion object {
        private const val TAG = "NFCReader"
        @Volatile
        private var instance: NFCReader? = null

        /**
         * 获取NFCReader单例实例
         */
        fun getInstance(): NFCReader {
            if (instance == null) {
                synchronized(NFCReader::class.java) {
                    if (instance == null) {
                        instance = NFCReader()
                    }
                }
            }
            return instance!!
        }
    }

    override fun initialize(context: Context) {
        this.context = context.applicationContext
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        updateNfcStatus()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun enableForegroundDispatch(activity: Activity) {
        if (!isNfcAvailable() || currentStatus != NFCStatus.ENABLED) {
            notifyError(IllegalStateException("NFC is not available or not enabled"))
            return
        }

        try {
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                activity, 0, intent, PendingIntent.FLAG_MUTABLE
            )

            val techList = arrayOf(arrayOf<String>())
            val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                filter.addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                notifyError(e)
            }

            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, arrayOf(filter), techList)
            Log.d(TAG, "Foreground dispatch enabled")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to enable foreground dispatch: ${e.message}")
            notifyError(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun disableForegroundDispatch(activity: Activity) {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            Log.d(TAG, "Foreground dispatch disabled")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to disable foreground dispatch: ${e.message}")
            notifyError(e)
        }
    }

    override fun readTag(intent: Intent): NFCResult {
        val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
            ?: return NFCResult(false, errorMessage = "No tag found in intent")

        Log.d(TAG, "Tag detected: ${tag.id.joinToString(" ") { "%02X".format(it) }}")
        Log.d(TAG, "Tag tech list: ${tag.techList.joinToString(", ")}")

        // 使用协程在后台线程处理标签读取
        CoroutineScope(Dispatchers.IO).launch {
            val result = processTag(tag)
            withContext(Dispatchers.Main) {
                notifyTagDetected(result)
            }
        }

        return NFCResult(true, tag.id.joinToString(" ") { "%02X".format(it) }, "Processing...", null)
    }

    private fun processTag(tag: android.nfc.Tag): NFCResult {
        // 查找支持该标签的处理器
        for (handler in tagHandlers) {
            if (handler.supportsTag(tag)) {
                return handler.handleTag(tag)
            }
        }
        return NFCResult(false, errorMessage = "Unsupported tag type: ${tag.techList.joinToString(", ")}")
    }

    override fun addListener(listener: NFCReaderListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            // 立即通知当前NFC状态
            mainHandler.post {
                listener.onNFCStatusChanged(currentStatus)
            }
        }
    }

    override fun removeListener(listener: NFCReaderListener) {
        listeners.remove(listener)
    }

    override fun isNfcAvailable(): Boolean {
        return nfcAdapter != null
    }

    override fun getNfcStatus(): NFCStatus {
        return currentStatus
    }

    private fun updateNfcStatus() {
        currentStatus = when {
            nfcAdapter == null -> NFCStatus.NOT_SUPPORTED
            nfcAdapter?.isEnabled == true -> NFCStatus.ENABLED
            else -> NFCStatus.DISABLED
        }

        Log.d(TAG, "NFC status updated to: $currentStatus")
        notifyStatusChanged(currentStatus)
    }

    private fun notifyStatusChanged(status: NFCStatus) {
        mainHandler.post {
            for (listener in listeners) {
                try {
                    listener.onNFCStatusChanged(status)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying listener: ${e.message}")
                }
            }
        }
    }

    private fun notifyTagDetected(result: NFCResult) {
        mainHandler.post {
            for (listener in listeners) {
                try {
                    listener.onTagDetected(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying listener: ${e.message}")
                }
            }
        }
    }

    private fun notifyError(error: Throwable) {
        mainHandler.post {
            for (listener in listeners) {
                try {
                    listener.onError(error)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying listener: ${e.message}")
                }
            }
        }
    }
}