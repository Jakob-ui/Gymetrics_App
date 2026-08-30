package com.laschober.gymetrics.data.local

import com.liftric.kvault.KVault

expect fun createKVault(): KVault

class TokenStore(private val vault: KVault = createKVault()) {

    fun saveTokens(accessToken: String, refreshToken: String) {
        vault.set(KEY_ACCESS, accessToken)
        vault.set(KEY_REFRESH, refreshToken)
    }

    fun accessToken(): String? = vault.string(KEY_ACCESS)
    fun refreshToken(): String? = vault.string(KEY_REFRESH)

    fun clear() {
        vault.deleteObject(KEY_ACCESS)
        vault.deleteObject(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}