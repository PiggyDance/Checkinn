package io.piggydance.checkinn.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.piggydance.checkinn.nfc.NFCResult
import io.piggydance.checkinn.nfc.NFCStatus

class NFCViewModel : ViewModel() {
    // NFC 状态
    private val _nfcStatus = MutableLiveData(NFCStatus.NOT_SUPPORTED)
    val nfcStatus: LiveData<NFCStatus> = _nfcStatus

    // 最近读取结果
    private val _lastReadResult = MutableLiveData<NFCResult?>(null)
    val lastReadResult: LiveData<NFCResult?> = _lastReadResult

    // 读取历史
    private val _readHistory = MutableLiveData<List<NFCResult>>(emptyList())
    val readHistory: LiveData<List<NFCResult>> = _readHistory

    // 读取计数
    private val _readCount = MutableLiveData(0)
    val readCount: LiveData<Int> = _readCount

    // 更新 NFC 状态
    fun updateNFCStatus(status: NFCStatus) {
        _nfcStatus.value = status
    }

    // 处理标签检测
    fun onTagDetected(result: NFCResult) {
        _lastReadResult.value = result
        _readCount.value = (_readCount.value ?: 0) + 1

        // 更新历史记录，只保留最近5条
        val currentHistory = _readHistory.value ?: emptyList()
        _readHistory.value = listOf(result) + currentHistory.take(4)
    }

}