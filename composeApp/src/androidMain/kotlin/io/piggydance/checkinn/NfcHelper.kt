package io.piggydance.checkinn

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.util.Log

/**
 * NFC 读写辅助类.
 *
 * NFC 贴纸写入的 NDEF 格式:
 *   URI: piggydance://checkinn?s=clock_in  (上班卡)
 *   URI: piggydance://checkinn?s=clock_out (下班卡)
 */
class NfcHelper(private val activity: Activity) {

    companion object {
        private const val TAG = "NfcHelper"
        const val SCHEME = "piggydance"
        const val HOST = "checkinn"
        const val PARAM_SCENE = "s"
    }

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isNfcAvailable: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    /** 启用前台 NFC 调度, 让当前 Activity 优先接收 NFC 事件 */
    fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try { addDataScheme(SCHEME) } catch (e: Exception) { Log.e(TAG, "addDataScheme", e) }
        }
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        val filters = arrayOf(ndefFilter, techFilter, tagFilter)
        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
        )

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    /**
     * 从 NFC Intent 中解析场景.
     * @return NfcScene 或 null(如果不是有效的打卡标签)
     */
    fun parseScene(intent: Intent): NfcScene? {
        val action = intent.action ?: return null
        Log.d(TAG, "parseScene action=$action")

        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED) {
            return null
        }

        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMsgs != null) {
            for (raw in rawMsgs) {
                val msg = raw as? NdefMessage ?: continue
                for (record in msg.records) {
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                        record.type.contentEquals(NdefRecord.RTD_URI)) {
                        val uri = record.toUri() ?: continue
                        Log.d(TAG, "Parsed URI: $uri")
                        if (uri.scheme == SCHEME && uri.host == HOST) {
                            val sceneKey = uri.getQueryParameter(PARAM_SCENE) ?: continue
                            return NfcScene.fromKey(sceneKey)
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * 将场景标识写入 NFC 标签.
     * @return true 写入成功
     */
    fun writeTag(intent: Intent, scene: NfcScene): Boolean {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return false

        val uri = "$SCHEME://$HOST?$PARAM_SCENE=${scene.key}"
        val record = NdefRecord.createUri(uri)
        val message = NdefMessage(arrayOf(record))

        return try {
            // 先尝试用 Ndef
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Log.e(TAG, "Tag is read-only")
                    ndef.close()
                    return false
                }
                if (ndef.maxSize < message.toByteArray().size) {
                    Log.e(TAG, "Tag capacity insufficient")
                    ndef.close()
                    return false
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                Log.d(TAG, "Write success via Ndef: $uri")
                true
            } else {
                // 尝试格式化后写入
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                    Log.d(TAG, "Write success via NdefFormatable: $uri")
                    true
                } else {
                    Log.e(TAG, "Tag doesn't support NDEF")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Write failed", e)
            false
        }
    }

    /** 检测 Intent 中是否包含 NFC Tag (用于写入模式) */
    fun hasTag(intent: Intent): Boolean {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        return tag != null
    }
}
