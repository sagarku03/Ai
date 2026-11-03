package com.jarvisai.assistant.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Computes and verifies the runtime SHA-256 signing certificate digest of the APK
 * using Android's `PackageManager.GET_SIGNING_CERTIFICATES` API.
 */
object SignatureVerifier {

    fun getCurrentSigningCertSha256(context: Context): String? {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        pkgName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                val signingInfo = pkgInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners.firstOrNull()?.toByteArray()
                    } else {
                        signingInfo.signingCertificateHistory.firstOrNull()?.toByteArray()
                    }
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }

            if (certBytes == null) return null
            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun verifyExpectedSignature(context: Context, expectedSha256Hex: String): Boolean {
        val actual = getCurrentSigningCertSha256(context) ?: return false
        return actual.equals(expectedSha256Hex.trim(), ignoreCase = true)
    }
}
