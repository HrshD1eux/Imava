package com.hrshd1eux.imava.data.sync.webdav

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object WebDavClient {

    private fun createClient(trustAllCertificates: Boolean = true): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (trustAllCertificates) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (_: Exception) {}
        }

        return builder.build()
    }

    private fun buildUrl(baseUrl: String, path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = path.trim().trimStart('/')
        return if (cleanPath.isEmpty()) cleanBase else "$cleanBase/$cleanPath"
    }

    suspend fun testConnection(config: WebDavConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!config.isConfigured) {
                return@withContext Result.failure(IllegalArgumentException("Server URL is invalid"))
            }

            val client = createClient()
            val url = buildUrl(config.serverUrl, "")
            val requestBuilder = Request.Builder().url(url)

            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }

            // WebDAV PROPFIND with depth 0
            val propfindRequest = requestBuilder
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaTypeOrNull()))
                .header("Depth", "0")
                .build()

            val response = client.newCall(propfindRequest).execute()
            response.use { resp ->
                val code = resp.code
                if (code in 200..299 || code == 207) {
                    val server = resp.header("Server") ?: "WebDAV Server"
                    return@withContext Result.success("Connected successfully ($server, code $code)")
                } else if (code == 401) {
                    return@withContext Result.failure(Exception("Authentication failed (401). Verify username and password."))
                } else if (code == 405) {
                    // Method not allowed for PROPFIND, fallback to test with HEAD/GET
                    val headRequest = Request.Builder().url(url)
                    if (config.username.isNotBlank() || config.password.isNotBlank()) {
                        headRequest.header("Authorization", Credentials.basic(config.username, config.password))
                    }
                    val headResp = client.newCall(headRequest.head().build()).execute()
                    headResp.use { hResp ->
                        if (hResp.isSuccessful || hResp.code in 200..399) {
                            return@withContext Result.success("Connected successfully to host (code ${hResp.code})")
                        } else {
                            return@withContext Result.failure(Exception("Server returned HTTP ${hResp.code}: ${hResp.message}"))
                        }
                    }
                } else {
                    return@withContext Result.failure(Exception("Server returned HTTP $code: ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun ensureRemoteDirectory(config: WebDavConfig, relativeDirPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = createClient()
            val parts = relativeDirPath.split("/").map { it.trim() }.filter { it.isNotEmpty() }
            var currentPath = ""

            for (part in parts) {
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val url = buildUrl(config.serverUrl, currentPath)
                val requestBuilder = Request.Builder().url(url)

                if (config.username.isNotBlank() || config.password.isNotBlank()) {
                    requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
                }

                // Check if directory exists
                val headReq = requestBuilder.head().build()
                val headResp = client.newCall(headReq).execute()
                val exists = headResp.isSuccessful || headResp.code in 200..399
                headResp.close()

                if (!exists) {
                    // Send MKCOL to create folder
                    val mkcolReq = requestBuilder.method("MKCOL", null).build()
                    val mkcolResp = client.newCall(mkcolReq).execute()
                    mkcolResp.close()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkRemoteFileExists(config: WebDavConfig, relativeFilePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = createClient()
            val url = buildUrl(config.serverUrl, relativeFilePath)
            val requestBuilder = Request.Builder().url(url)
            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }
            val resp = client.newCall(requestBuilder.head().build()).execute()
            val exists = resp.isSuccessful
            resp.close()
            exists
        } catch (_: Exception) {
            false
        }
    }

    suspend fun uploadMedia(
        config: WebDavConfig,
        context: Context,
        uri: Uri,
        remoteRelativePath: String,
        mimeType: String,
        totalBytes: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = createClient()
            val url = buildUrl(config.serverUrl, remoteRelativePath)

            // Extract directory part to ensure it exists
            val lastSlash = remoteRelativePath.lastIndexOf('/')
            if (lastSlash > 0) {
                val dir = remoteRelativePath.substring(0, lastSlash)
                ensureRemoteDirectory(config, dir)
            }

            val requestBody = object : RequestBody() {
                override fun contentType() = (mimeType.ifBlank { "application/octet-stream" }).toMediaTypeOrNull()

                override fun contentLength(): Long = totalBytes

                override fun writeTo(sink: BufferedSink) {
                    val stream: InputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open input stream for $uri")
                    stream.use { input ->
                        input.source().use { source ->
                            sink.writeAll(source)
                        }
                    }
                }
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .put(requestBody)

            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }

            val response = client.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                if (resp.isSuccessful || resp.code in 200..299 || resp.code == 201 || resp.code == 204) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${resp.code}: ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
