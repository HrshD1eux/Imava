package com.hrshd1eux.imava.core.p2p

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.util.Size
import com.hrshd1eux.imava.core.util.FormatUtils
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ImavaDropState(
    val isRunning: Boolean = false,
    val ipAddress: String? = null,
    val port: Int = 8765,
    val url: String? = null,
    val sharedItems: List<MediaItem> = emptyList(),
    val totalBytes: Long = 0L,
    val logs: List<String> = emptyList(),
    val filesReceivedCount: Int = 0
)

object ImavaDropServer {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var threadPool = Executors.newCachedThreadPool()
    private var wifiLock: WifiManager.WifiLock? = null

    private val _state = MutableStateFlow(ImavaDropState())
    val state: StateFlow<ImavaDropState> = _state.asStateFlow()

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isRunning(): Boolean = _state.value.isRunning

    @Synchronized
    fun start(context: Context, items: List<MediaItem>, preferredPort: Int = 8765): Boolean {
        if (_state.value.isRunning) {
            updateItems(items)
            return true
        }

        appContext = context.applicationContext
        val ip = getLocalIpAddress()
        if (ip == null) {
            addLog("No active Wi-Fi or Local Area Network connection detected.")
        }

        val totalSize = items.sumOf { it.size }

        // Find available port starting from preferredPort
        var port = preferredPort
        var socket: ServerSocket? = null
        try {
            socket = ServerSocket(port)
        } catch (_: Exception) {
            try {
                socket = ServerSocket(0)
                port = socket.localPort
            } catch (e: Exception) {
                e.printStackTrace()
                addLog("Failed to bind server socket: ${e.message}")
                return false
            }
        }
        val finalSocket = socket ?: return false
        serverSocket = finalSocket

        // Acquire high-performance Wi-Fi lock to prevent throttling during large media transfers
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ImavaDrop:WifiLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {}

        if (threadPool.isShutdown) {
            threadPool = Executors.newCachedThreadPool()
        }

        val fullUrl = if (ip != null) "http://$ip:$port" else "http://localhost:$port"

        _state.value = ImavaDropState(
            isRunning = true,
            ipAddress = ip,
            port = port,
            url = fullUrl,
            sharedItems = items,
            totalBytes = totalSize,
            logs = listOf("Imava Drop started on $fullUrl", "Ready to transfer ${items.size} items (${FormatUtils.formatFileSize(totalSize)})")
        )

        serverJob = scope.launch {
            listenLoop(finalSocket)
        }

        return true
    }

    @Synchronized
    fun updateItems(items: List<MediaItem>) {
        val totalSize = items.sumOf { it.size }
        _state.value = _state.value.copy(
            sharedItems = items,
            totalBytes = totalSize
        )
        addLog("Updated share list: ${items.size} items (${FormatUtils.formatFileSize(totalSize)})")
    }

    @Synchronized
    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        serverJob?.cancel()
        serverJob = null

        try {
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        wifiLock = null

        _state.value = _state.value.copy(
            isRunning = false,
            logs = _state.value.logs + "Imava Drop stopped."
        )
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$time] $message"
        val currentLogs = _state.value.logs
        val updated = (currentLogs + formatted).takeLast(30)
        _state.value = _state.value.copy(logs = updated)
    }

    private fun listenLoop(socket: ServerSocket) {
        while (_state.value.isRunning && !socket.isClosed) {
            try {
                val client = socket.accept()
                threadPool.execute {
                    handleClient(client)
                }
            } catch (e: Exception) {
                if (socket.isClosed) break
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 30_000
            val input = client.getInputStream()
            val output = client.getOutputStream()

            // Read request line and headers
            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase(Locale.ROOT)
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")

            val headers = mutableMapOf<String, String>()
            var line: String? = reader.readLine()
            while (!line.isNullOrBlank()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val key = line.substring(0, colonIdx).trim().lowercase(Locale.ROOT)
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                line = reader.readLine()
            }

            val clientIp = client.inetAddress.hostAddress ?: "Unknown"

            when {
                method == "GET" && (path == "/" || path == "/index.html") -> {
                    serveHtmlClient(output)
                }
                method == "GET" && path == "/download-all" -> {
                    addLog("Client ($clientIp) requested Download All (ZIP)")
                    serveZipArchive(output)
                }
                method == "GET" && path.startsWith("/file/") -> {
                    val id = path.removePrefix("/file/").toLongOrNull()
                    val item = _state.value.sharedItems.find { it.id == id }
                    if (item != null) {
                        addLog("Client ($clientIp) downloading: ${getFilename(item)}")
                        serveSingleFile(item, headers, output)
                    } else {
                        send404(output)
                    }
                }
                method == "GET" && path.startsWith("/thumb/") -> {
                    val id = path.removePrefix("/thumb/").toLongOrNull()
                    val item = _state.value.sharedItems.find { it.id == id }
                    if (item != null) {
                        serveThumbnail(item, output)
                    } else {
                        send404(output)
                    }
                }
                method == "POST" && path == "/upload" -> {
                    handleUpload(headers, input, output, clientIp)
                }
                method == "GET" && path == "/api/status" -> {
                    serveJsonStatus(output)
                }
                else -> {
                    send404(output)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun getFilename(item: MediaItem): String {
        val nameFromPath = item.path.substringAfterLast('/')
        if (nameFromPath.isNotBlank()) return nameFromPath
        val ext = item.mimeType.substringAfterLast('/', if (item is MediaItem.Video) "mp4" else "jpg")
        return "media_${item.id}.$ext"
    }

    private fun serveHtmlClient(output: OutputStream) {
        val items = _state.value.sharedItems
        val totalSizeStr = FormatUtils.formatFileSize(_state.value.totalBytes)
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        val cardsHtml = StringBuilder()
        for (item in items) {
            val filename = getFilename(item)
            val sizeStr = FormatUtils.formatFileSize(item.size)
            val isVideo = item is MediaItem.Video
            val typeBadge = if (isVideo) "VIDEO" else "PHOTO"
            val badgeColor = if (isVideo) "#7c4dff" else "#00b0ff"

            cardsHtml.append("""
                <div class="card">
                    <div class="thumb-container">
                        <img src="/thumb/${item.id}" alt="$filename" loading="lazy">
                        <span class="badge" style="background:${badgeColor};">$typeBadge</span>
                    </div>
                    <div class="card-info">
                        <div class="filename" title="$filename">$filename</div>
                        <div class="filesize">$sizeStr</div>
                        <a href="/file/${item.id}" class="btn-download" download="$filename">Download</a>
                    </div>
                </div>
            """.trimIndent())
        }

        val emptyStateHtml = if (items.isEmpty()) {
            """<div class="empty-state">No media currently selected on phone.<br>You can still drop files below to send to the phone!</div>"""
        } else ""

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Imava Drop · Local Wi-Fi Transfer</title>
    <style>
        :root {
            --bg-color: #0f1015;
            --surface-color: #1a1b23;
            --card-color: #242632;
            --primary-color: #8b5cf6;
            --primary-hover: #7c3aed;
            --accent-color: #10b981;
            --text-main: #f3f4f6;
            --text-sub: #9ca3af;
            --border-color: #2e3244;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            padding: 24px 16px 60px;
            display: flex;
            flex-direction: column;
            align-items: center;
            min-height: 100vh;
        }
        .container { max-width: 960px; width: 100%; }
        header {
            text-align: center;
            margin-bottom: 28px;
        }
        .logo-row {
            display: inline-flex;
            align-items: center;
            gap: 10px;
            background: linear-gradient(135deg, rgba(139,92,246,0.2), rgba(16,185,129,0.1));
            border: 1px solid var(--border-color);
            padding: 6px 16px;
            border-radius: 9999px;
            font-size: 13px;
            color: var(--accent-color);
            font-weight: 600;
            margin-bottom: 12px;
        }
        h1 { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 6px; }
        .subtitle { color: var(--text-sub); font-size: 14px; }
        .hero-banner {
            background: var(--surface-color);
            border: 1px solid var(--border-color);
            border-radius: 18px;
            padding: 20px 24px;
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            margin-bottom: 28px;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.4);
        }
        .stats-col h2 { font-size: 18px; font-weight: 700; }
        .stats-col p { color: var(--text-sub); font-size: 13px; margin-top: 4px; }
        .btn-zip {
            background: linear-gradient(135deg, var(--primary-color), #6366f1);
            color: #fff;
            text-decoration: none;
            font-weight: 700;
            font-size: 15px;
            padding: 12px 24px;
            border-radius: 12px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: transform 0.15s ease, filter 0.15s ease;
            box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);
        }
        .btn-zip:hover { transform: translateY(-2px); filter: brightness(1.1); }
        .upload-section {
            background: var(--surface-color);
            border: 2px dashed var(--border-color);
            border-radius: 18px;
            padding: 28px 20px;
            text-align: center;
            margin-bottom: 32px;
            transition: border-color 0.2s;
            cursor: pointer;
        }
        .upload-section.dragover { border-color: var(--accent-color); background: rgba(16,185,129,0.05); }
        .upload-section h3 { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
        .upload-section p { font-size: 13px; color: var(--text-sub); margin-bottom: 14px; }
        .btn-select {
            background: #2e3244;
            color: #fff;
            padding: 8px 18px;
            font-size: 13px;
            font-weight: 600;
            border-radius: 8px;
            border: none;
            cursor: pointer;
        }
        .progress-bar {
            width: 100%;
            height: 6px;
            background: #2e3244;
            border-radius: 4px;
            overflow: hidden;
            margin-top: 14px;
            display: none;
        }
        .progress-fill { width: 0%; height: 100%; background: var(--accent-color); transition: width 0.2s; }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
            gap: 16px;
        }
        .card {
            background: var(--card-color);
            border: 1px solid var(--border-color);
            border-radius: 14px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0,0,0,0.5); }
        .thumb-container {
            position: relative;
            width: 100%;
            height: 160px;
            background: #12131a;
            overflow: hidden;
        }
        .thumb-container img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }
        .badge {
            position: absolute;
            top: 8px;
            right: 8px;
            font-size: 10px;
            font-weight: 700;
            padding: 3px 7px;
            border-radius: 6px;
            color: #fff;
            letter-spacing: 0.5px;
        }
        .card-info {
            padding: 12px;
            display: flex;
            flex-direction: column;
            flex: 1;
            justify-content: space-between;
        }
        .filename {
            font-size: 13px;
            font-weight: 600;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            margin-bottom: 4px;
        }
        .filesize { font-size: 12px; color: var(--text-sub); margin-bottom: 12px; }
        .btn-download {
            background: #373b4d;
            color: #fff;
            text-align: center;
            text-decoration: none;
            padding: 8px 12px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            transition: background 0.15s;
        }
        .btn-download:hover { background: var(--primary-color); }
        .empty-state {
            text-align: center;
            padding: 40px;
            color: var(--text-sub);
            font-size: 14px;
            line-height: 1.6;
        }
        footer {
            margin-top: 40px;
            text-align: center;
            font-size: 12px;
            color: var(--text-sub);
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="logo-row">
                <span>⚡ Direct Wi-Fi P2P</span> · <span>100% Offline & Private</span>
            </div>
            <h1>Imava Drop</h1>
            <p class="subtitle">Sharing originals directly from <strong>$deviceModel</strong></p>
        </header>

        <div class="hero-banner">
            <div class="stats-col">
                <h2>${items.size} Original Files Available</h2>
                <p>Total Size: <strong>$totalSizeStr</strong> · Lossless Full Resolution</p>
            </div>
            ${if (items.isNotEmpty()) """<a href="/download-all" class="btn-zip">📦 Download All as ZIP</a>""" else ""}
        </div>

        <div class="upload-section" id="dropZone" onclick="document.getElementById('fileInput').click()">
            <input type="file" id="fileInput" multiple style="display:none" onchange="uploadFiles(this.files)">
            <h3>📤 Send Photos or Videos to Phone</h3>
            <p>Drag & drop media files here, or tap to browse from this computer</p>
            <button type="button" class="btn-select">Select Files</button>
            <div class="progress-bar" id="progressBar">
                <div class="progress-fill" id="progressFill"></div>
            </div>
            <div id="uploadStatus" style="font-size:12px;color:var(--accent-color);margin-top:8px;"></div>
        </div>

        $emptyStateHtml

        <div class="grid">
            $cardsHtml
        </div>

        <footer>
            Imava Gallery · Zero Cloud · Local Peer-to-Peer Transfer
        </footer>
    </div>

    <script>
        const dropZone = document.getElementById('dropZone');
        ['dragenter', 'dragover'].forEach(name => {
            dropZone.addEventListener(name, (e) => { e.preventDefault(); dropZone.classList.add('dragover'); }, false);
        });
        ['dragleave', 'drop'].forEach(name => {
            dropZone.addEventListener(name, (e) => { e.preventDefault(); dropZone.classList.remove('dragover'); }, false);
        });
        dropZone.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            if (dt && dt.files && dt.files.length > 0) {
                uploadFiles(dt.files);
            }
        });

        async function uploadFiles(files) {
            if (!files || files.length === 0) return;
            const bar = document.getElementById('progressBar');
            const fill = document.getElementById('progressFill');
            const status = document.getElementById('uploadStatus');
            bar.style.display = 'block';

            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                status.innerText = "Uploading (" + (i + 1) + "/" + files.length + "): " + file.name + "...";
                fill.style.width = Math.round(((i) / files.length) * 100) + "%";

                try {
                    await fetch('/upload', {
                        method: 'POST',
                        headers: {
                            'X-Filename': encodeURIComponent(file.name),
                            'Content-Type': file.type || 'application/octet-stream'
                        },
                        body: file
                    });
                } catch (err) {
                    console.error('Upload error', err);
                    status.innerText = "Upload failed: " + file.name;
                    return;
                }
            }
            fill.style.width = '100%';
            status.innerText = "Successfully transferred " + files.length + " file(s) to phone!";
            setTimeout(() => { bar.style.display = 'none'; }, 2000);
        }
    </script>
</body>
</html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveThumbnail(item: MediaItem, output: OutputStream) {
        val ctx = appContext ?: return send404(output)
        try {
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ctx.contentResolver.loadThumbnail(item.uri, Size(480, 480), null)
                } catch (_: Exception) {
                    decodeFallbackThumbnail(ctx, item)
                }
            } else {
                decodeFallbackThumbnail(ctx, item)
            }

            if (bitmap != null) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bytes = baos.toByteArray()
                bitmap.recycle()

                val headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: image/jpeg\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Cache-Control: public, max-age=86400\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(headers.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.flush()
            } else {
                send404(output)
            }
        } catch (_: Exception) {
            send404(output)
        }
    }

    private fun decodeFallbackThumbnail(context: Context, item: MediaItem): Bitmap? {
        return try {
            if (item is MediaItem.Video) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, item.uri)
                val frame = retriever.getFrameAtTime(1_000_000)
                retriever.release()
                frame
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(item.uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                var sample = 1
                while (options.outWidth / (sample * 2) >= 320 && options.outHeight / (sample * 2) >= 320) {
                    sample *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(item.uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun serveSingleFile(item: MediaItem, headers: Map<String, String>, output: OutputStream) {
        val ctx = appContext ?: return send404(output)
        val fileLength = item.size
        val filename = getFilename(item)
        val rangeHeader = headers["range"]

        val inStream: InputStream = try {
            ctx.contentResolver.openInputStream(item.uri)
                ?: if (File(item.path).exists()) FileInputStream(File(item.path)) else return send404(output)
        } catch (_: Exception) {
            return send404(output)
        }

        inStream.use { stream ->
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Parse Range: bytes=start-end
                val rangeVal = rangeHeader.removePrefix("bytes=").trim()
                val parts = rangeVal.split("-")
                val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                val end = parts.getOrNull(1)?.toLongOrNull() ?: (fileLength - 1)
                val contentLength = end - start + 1

                val headerStr = "HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Type: ${item.mimeType}\r\n" +
                        "Content-Range: bytes $start-$end/$fileLength\r\n" +
                        "Content-Length: $contentLength\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Disposition: attachment; filename=\"$filename\"\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(headerStr.toByteArray(Charsets.UTF_8))

                stream.skip(start)
                copyStreamWithLimit(stream, output, contentLength)
            } else {
                val headerStr = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: ${item.mimeType}\r\n" +
                        "Content-Length: $fileLength\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Disposition: attachment; filename=\"$filename\"\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(headerStr.toByteArray(Charsets.UTF_8))
                stream.copyTo(output, 65536)
            }
            output.flush()
        }
    }

    private fun copyStreamWithLimit(input: InputStream, output: OutputStream, maxBytes: Long) {
        val buffer = ByteArray(65536)
        var remaining = maxBytes
        while (remaining > 0) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val read = input.read(buffer, 0, toRead)
            if (read == -1) break
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun serveZipArchive(output: OutputStream) {
        val ctx = appContext ?: return send404(output)
        val items = _state.value.sharedItems
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFilename = "Imava_Drop_$dateStamp.zip"

        val headerStr = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/zip\r\n" +
                "Content-Disposition: attachment; filename=\"$zipFilename\"\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headerStr.toByteArray(Charsets.UTF_8))

        val zipOut = ZipOutputStream(BufferedOutputStream(output, 65536))
        val seenNames = mutableSetOf<String>()

        for (item in items) {
            var name = getFilename(item)
            if (name in seenNames) {
                val base = name.substringBeforeLast('.')
                val ext = name.substringAfterLast('.', "")
                name = "${base}_${item.id}${if (ext.isNotEmpty()) ".$ext" else ""}"
            }
            seenNames.add(name)

            try {
                val inStream = ctx.contentResolver.openInputStream(item.uri)
                    ?: if (File(item.path).exists()) FileInputStream(File(item.path)) else null
                if (inStream != null) {
                    inStream.use { stream ->
                        val entry = ZipEntry(name)
                        zipOut.putNextEntry(entry)
                        stream.copyTo(zipOut, 65536)
                        zipOut.closeEntry()
                    }
                }
            } catch (_: Exception) {}
        }
        zipOut.finish()
        zipOut.flush()
        addLog("Zip transfer completed for ${items.size} items.")
    }

    private fun handleUpload(
        headers: Map<String, String>,
        input: InputStream,
        output: OutputStream,
        clientIp: String
    ) {
        val ctx = appContext ?: return send404(output)
        val rawFilename = headers["x-filename"]?.let {
            try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { null }
        } ?: "imava_drop_${System.currentTimeMillis()}.jpg"

        val cleanFilename = rawFilename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dropDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ImavaDrop")
        if (!dropDir.exists()) dropDir.mkdirs()

        var targetFile = File(dropDir, cleanFilename)
        if (targetFile.exists()) {
            val base = cleanFilename.substringBeforeLast('.')
            val ext = cleanFilename.substringAfterLast('.', "")
            targetFile = File(dropDir, "${base}_${System.currentTimeMillis()}${if (ext.isNotEmpty()) ".$ext" else ""}")
        }

        val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
        try {
            FileOutputStream(targetFile).use { fos ->
                if (contentLength > 0L) {
                    copyStreamWithLimit(input, fos, contentLength)
                } else {
                    input.copyTo(fos, 65536)
                }
            }

            // Immediately scan into Android MediaStore so gallery surfaces it
            MediaScannerConnection.scanFile(ctx, arrayOf(targetFile.absolutePath), null, null)

            val sizeStr = FormatUtils.formatFileSize(targetFile.length())
            addLog("Received from $clientIp: ${targetFile.name} ($sizeStr)")
            _state.value = _state.value.copy(filesReceivedCount = _state.value.filesReceivedCount + 1)

            val json = """{"status":"ok","filename":"${targetFile.name}"}"""
            val res = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${json.length}\r\nConnection: close\r\n\r\n$json"
            output.write(res.toByteArray(Charsets.UTF_8))
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            val errJson = """{"status":"error","message":"${e.message}"}"""
            val res = "HTTP/1.1 500 Internal Error\r\nContent-Type: application/json\r\nContent-Length: ${errJson.length}\r\nConnection: close\r\n\r\n$errJson"
            output.write(res.toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }

    private fun serveJsonStatus(output: OutputStream) {
        val items = _state.value.sharedItems
        val json = """{"running":true,"itemCount":${items.size},"totalBytes":${_state.value.totalBytes}}"""
        val res = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${json.length}\r\nConnection: close\r\n\r\n$json"
        output.write(res.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun send404(output: OutputStream) {
        val body = "404 Not Found"
        val res = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
        try {
            output.write(res.toByteArray(Charsets.UTF_8))
            output.flush()
        } catch (_: Exception) {}
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in Collections.list(interfaces)) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
            // Fallback: any non-loopback IPv4
            val fallbackInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in Collections.list(fallbackInterfaces)) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
