package com.splitsignal.core.network

import com.splitsignal.core.domain.FileTransferTransport
import com.splitsignal.core.domain.PlatformFileHandle

/**
 * Browser-safe no-op [FileTransferTransport] for WasmJs targets.
 */
class WasmFileTransferTransport : FileTransferTransport {
    override suspend fun sendOfferAndStream(
        peerIp: String,
        peerPort: Int,
        transferId: String,
        file: PlatformFileHandle,
        onProgress: (Long, Long) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        onError("Offline file transfer is not supported on WasmJs")
    }

    override fun registerOutgoingFile(transferId: String, file: PlatformFileHandle) = Unit

    override fun unregisterOutgoingFile(transferId: String) = Unit

    override suspend fun downloadOfferedFile(
        peerIp: String,
        peerPort: Int,
        transferId: String,
        fileName: String,
        totalBytes: Long,
        onProgress: (Long, Long) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        onError("Offline file transfer is not supported on WasmJs")
    }

    override fun cancelTransfer(transferId: String) = Unit
}
