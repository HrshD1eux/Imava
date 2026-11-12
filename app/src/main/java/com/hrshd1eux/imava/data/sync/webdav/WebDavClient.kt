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

data class RemoteFileInfo(
    val exists: Boolean,
    val contentLength: Long = -1L,
    val etag: String? = null
)

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

    fun buildUrl(baseUrl: String, path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = path.trim().trim('/')
        if (cleanPath.isEmpty()) return cleanBase
        val encodedSegments = cleanPath.split("/").joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
        return "$cleanBase/$encodedSegments"
    }

    suspend fun testConnection(config: WebDavConfig, context: Context? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!config.isConfigured) {
                val msg = com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                    context,
                    com.hrshd1eux.imava.R.string.webdav_err_invalid_url,
                    "Server URL is invalid"
                )
                return@withContext Result.failure(IllegalArgumentException(msg))
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
                    val msg = com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                        context,
                        com.hrshd1eux.imava.R.string.webdav_test_connected_server_fmt,
                        "Connected successfully ($server, code $code)",
                        server,
                        code
                    )
                    return@withContext Result.success(msg)
                } else if (code == 401) {
                    val msg = com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                        context,
                        com.hrshd1eux.imava.R.string.webdav_test_auth_failed,
                        "Authentication failed (401). Verify username and password."
                    )
                    return@withContext Result.failure(Exception(msg))
                } else if (code == 405) {
                    // Method not allowed for PROPFIND, fallback to test with HEAD/GET
                    val headRequest = Request.Builder().url(url)
                    if (config.username.isNotBlank() || config.password.isNotBlank()) {
                        headRequest.header("Authorization", Credentials.basic(config.username, config.password))
                    }
                    val headResp = client.newCall(headRequest.head().build()).execute()
                    headResp.use { hResp ->
                        if (hResp.isSuccessful || hResp.code in 200..399) {
                            val msg = com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                                context,
                                com.hrshd1eux.imava.R.string.webdav_test_connected_host_fmt,
                                "Connected successfully to host (code ${hResp.code})",
                                hResp.code
                            )
                            return@withContext Result.success(msg)
                        } else {
                            val msg = com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                                context,
                                com.hrshd1eux.imava.R.string.webdav_test_http_err_fmt,
                                "Server returned HTTP ${hResp.code}: ${hResp.message}",
                                hResp.code,
                                hResp.message
                            )
                            return@withContext Result.failure(Exception(msg))
                        }
                    }
                } else {
                    val msg = com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                        context,
                        com.hrshd1eux.imava.R.string.webdav_test_http_err_fmt,
                        "Server returned HTTP $code: ${resp.message}",
                        code,
                        resp.message
                    )
                    return@withContext Result.failure(Exception(msg))
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
        getRemoteFileInfo(config, relativeFilePath).exists
    }

    suspend fun getRemoteFileInfo(config: WebDavConfig, relativeFilePath: String): RemoteFileInfo = withContext(Dispatchers.IO) {
        try {
            val client = createClient()
            val url = buildUrl(config.serverUrl, relativeFilePath)
            val requestBuilder = Request.Builder().url(url).head()
            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }
            val resp = client.newCall(requestBuilder.build()).execute()
            resp.use {
                if (it.isSuccessful) {
                    val len = it.header("Content-Length")?.toLongOrNull() ?: -1L
                    val etag = it.header("ETag")?.trim('"', ' ')
                    RemoteFileInfo(exists = true, contentLength = len, etag = etag)
                } else {
                    RemoteFileInfo(exists = false)
                }
            }
        } catch (_: Exception) {
            RemoteFileInfo(exists = false)
        }
    }

    suspend fun moveRemoteFile(
        config: WebDavConfig,
        sourceRelativePath: String,
        destRelativePath: String,
        overwrite: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = createClient()
            val sourceUrl = buildUrl(config.serverUrl, sourceRelativePath)
            val destUrl = buildUrl(config.serverUrl, destRelativePath)

            val requestBuilder = Request.Builder()
                .url(sourceUrl)
                .method("MOVE", null)
                .header("Destination", destUrl)
                .header("Overwrite", if (overwrite) "T" else "F")

            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }

            val response = client.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                if (resp.isSuccessful || resp.code in 200..299 || resp.code == 201 || resp.code == 204) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("MOVE failed: HTTP ${resp.code}: ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRemoteFile(
        config: WebDavConfig,
        relativePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = createClient()
            val url = buildUrl(config.serverUrl, relativePath)

            val requestBuilder = Request.Builder()
                .url(url)
                .delete()

            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }

            val response = client.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                if (resp.isSuccessful || resp.code in 200..299 || resp.code == 404) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("DELETE failed: HTTP ${resp.code}: ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        val cleanRemotePath = remoteRelativePath.trim().trimStart('/')
        val tempRelativePath = "$cleanRemotePath.imava_tmp"
        try {
            val client = createClient()

            // 1. Check if identical file already exists on remote NAS (avoid redundant re-upload)
            val existingInfo = getRemoteFileInfo(config, cleanRemotePath)
            if (existingInfo.exists && totalBytes > 0L && existingInfo.contentLength == totalBytes) {
                return@withContext Result.success(Unit)
            }

            // 2. Pre-ensure parent directory exists
            val lastSlash = cleanRemotePath.lastIndexOf('/')
            if (lastSlash > 0) {
                val dir = cleanRemotePath.substring(0, lastSlash)
                ensureRemoteDirectory(config, dir)
            }

            // 3. Atomic upload: write to temporary file first (.imava_tmp)
            val tempUrl = buildUrl(config.serverUrl, tempRelativePath)

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
                .url(tempUrl)
                .put(requestBody)

            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val uploadSuccess = response.use { resp ->
                resp.isSuccessful || resp.code in 200..299 || resp.code == 201 || resp.code == 204
            }

            if (!uploadSuccess) {
                deleteRemoteFile(config, tempRelativePath)
                return@withContext Result.failure(Exception("Upload failed to temp destination HTTP ${response.code}: ${response.message}"))
            }

            // 4. Atomic rename via WebDAV MOVE to finalize
            val moveResult = moveRemoteFile(config, tempRelativePath, cleanRemotePath, overwrite = true)
            if (moveResult.isFailure) {
                // If MOVE method is not supported (HTTP 405), fallback to direct PUT
                val err = moveResult.exceptionOrNull()
                if (err?.message?.contains("405") == true) {
                    deleteRemoteFile(config, tempRelativePath)
                    return@withContext directUpload(config, context, uri, cleanRemotePath, mimeType, totalBytes)
                }
                deleteRemoteFile(config, tempRelativePath)
                return@withContext moveResult
            }

            Result.success(Unit)
        } catch (e: Exception) {
            try {
                deleteRemoteFile(config, tempRelativePath)
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private suspend fun directUpload(
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
            val requestBuilder = Request.Builder().url(url).put(requestBody)
            if (config.username.isNotBlank() || config.password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            }
            val response = client.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                if (resp.isSuccessful || resp.code in 200..299 || resp.code == 201 || resp.code == 204) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Direct upload failed: HTTP ${resp.code}: ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
