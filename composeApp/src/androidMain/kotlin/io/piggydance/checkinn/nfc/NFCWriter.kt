package io.piggydance.checkinn.nfc

import android.content.Context
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import java.io.IOException

/**
 * NFC写入器接口
 */
interface INFCWriter {
    fun writeText(tag: Tag, text: String): Boolean
    fun writeUri(tag: Tag, uri: String): Boolean
    fun writeAppLaunch(tag: Tag, packageName: String, params: String): Boolean
    fun writeBluetooth(tag: Tag, macAddress: String): Boolean
    fun writeCustomNdef(tag: Tag, ndefMessage: NdefMessage): Boolean
}

/**
 * 写入数据类型枚举
 */
enum class WriteDataType {
    TEXT,
    URI,
    APP_LAUNCH,
    BLUETOOTH,
    CUSTOM
}

/**
 * NFC写入结果数据类
 */
data class NFCWriteResult(
    val success: Boolean,
    val message: String = "",
    val errorMessage: String? = null
)

/**
 * NFC写入器实现类
 */
class NFCWriter : INFCWriter {
    private val TAG = "NFCWriter"
    
    /**
     * 写入文本到NFC标签
     */
    override fun writeText(tag: Tag, text: String): Boolean {
        val record = NdefRecord.createTextRecord("en", text)
        val message = NdefMessage(arrayOf(record))
        return writeNdefMessage(tag, message)
    }
    
    /**
     * 写入URI到NFC标签
     */
    override fun writeUri(tag: Tag, uri: String): Boolean {
        val record = NdefRecord.createUri(uri)
        val message = NdefMessage(arrayOf(record))
        return writeNdefMessage(tag, message)
    }
    
    /**
     * 写入应用启动信息到NFC标签
     */
    override fun writeAppLaunch(
        tag: Tag,
        packageName: String,
        params: String
    ): Boolean = try {
        val ndef = Ndef.get(tag) ?: return false
        if (!ndef.isConnected) ndef.connect()

        val appRecord = NdefRecord.createApplicationRecord(packageName)
        val typeRecord  = NdefRecord.createMime("text/plain", params.toByteArray())

        val message = NdefMessage(arrayOf(appRecord, typeRecord))
        ndef.writeNdefMessage(message)
        ndef.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
    
    /**
     * 写入蓝牙配对信息到NFC标签
     */
    override fun writeBluetooth(tag: Tag, macAddress: String): Boolean {
        // 创建一个包含蓝牙MAC地址的文本记录
        val record = NdefRecord.createTextRecord("en", "BT:$macAddress")
        val message = NdefMessage(arrayOf(record))
        return writeNdefMessage(tag, message)
    }
    
    /**
     * 写入自定义NDEF消息到NFC标签
     */
    override fun writeCustomNdef(tag: Tag, ndefMessage: NdefMessage): Boolean {
        return writeNdefMessage(tag, ndefMessage)
    }
    
    /**
     * 写入NDEF消息到NFC标签的核心方法
     */
    private fun writeNdefMessage(tag: Tag, message: NdefMessage): Boolean {
        return try {
            val ndef = Ndef.get(tag)
            
            if (ndef != null) {
                ndef.connect()
                
                if (ndef.isWritable) {
                    // 检查标签是否有足够的空间
                    val size = message.toByteArray().size
                    if (ndef.maxSize >= size) {
                        ndef.writeNdefMessage(message)
                        Log.d(TAG, "Successfully wrote message to tag")
                        true
                    } else {
                        Log.e(TAG, "Tag capacity is insufficient. Message size: $size, Tag capacity: ${ndef.maxSize}")
                        false
                    }
                } else {
                    Log.e(TAG, "Tag is not writable")
                    false
                }
            } else {
                // 尝试格式化标签
                formatTag(tag, message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to tag", e)
            false
        }
    }
    
    /**
     * 格式化标签并写入NDEF消息
     */
    private fun formatTag(tag: Tag, message: NdefMessage): Boolean {
        return try {
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                ndefFormatable.connect()
                ndefFormatable.format(message)
                Log.d(TAG, "Successfully formatted tag and wrote message")
                true
            } else {
                Log.e(TAG, "Tag is not formatable")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to format tag", e)
            false
        } catch (e: FormatException) {
            Log.e(TAG, "Failed to format tag", e)
            false
        }
    }
    
    /**
     * 检查设备是否支持NFC写入
     */
    fun isNfcWriteSupported(context: Context): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        return nfcAdapter != null && nfcAdapter.isEnabled
    }
    
    /**
     * 检查标签是否可写入
     */
    fun isTagWritable(tag: Tag): Boolean {
        val ndef = Ndef.get(tag)
        return ndef?.isWritable ?: false
    }
    
    /**
     * 获取标签容量
     */
    fun getTagCapacity(tag: Tag): Int {
        val ndef = Ndef.get(tag)
        return ndef?.maxSize ?: 0
    }
}