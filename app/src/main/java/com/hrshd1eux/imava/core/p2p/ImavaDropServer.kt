package com.hrshd1eux.imava.core.p2p

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.webkit.MimeTypeMap
import com.hrshd1eux.imava.core.util.FormatUtils
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
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

        try {
            ImavaDropService.start(context)
        } catch (_: Exception) {}

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
    fun stop(stopService: Boolean = true) {
        if (stopService) {
            appContext?.let {
                try { ImavaDropService.stop(it) } catch (_: Exception) {}
            }
        }

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
            client.soTimeout = 60_000
            val rawInput = client.getInputStream()
            val bis = BufferedInputStream(rawInput, 65536)
            val output = client.getOutputStream()

            // Read request line and headers byte-safely without consuming binary payload
            val requestLine = readAsciiLine(bis) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase(Locale.ROOT)
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")

            val headers = mutableMapOf<String, String>()
            var line: String? = readAsciiLine(bis)
            while (!line.isNullOrBlank()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val key = line.substring(0, colonIdx).trim().lowercase(Locale.ROOT)
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                line = readAsciiLine(bis)
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
                    handleUpload(headers, bis, output, clientIp)
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

    private fun readAsciiLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) {
                return if (sb.isEmpty()) null else sb.toString()
            }
            if (b == '\n'.code) {
                break
            }
            if (b != '\r'.code) {
                sb.append(b.toChar())
            }
        }
        return sb.toString()
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

        val itemsJson = items.joinToString(",", "[", "]") { item ->
            val filename = getFilename(item).replace("\\", "\\\\").replace("\"", "\\\"")
            val isVideo = item is MediaItem.Video
            """{"id":${item.id},"name":"$filename","size":"${FormatUtils.formatFileSize(item.size)}","isVideo":$isVideo}"""
        }

        val cardsHtml = StringBuilder()
        for ((idx, item) in items.withIndex()) {
            val filename = getFilename(item)
            val sizeStr = FormatUtils.formatFileSize(item.size)
            val isVideo = item is MediaItem.Video
            val typeBadge = if (isVideo) "VIDEO" else "PHOTO"
            val badgeColor = if (isVideo) "#7c4dff" else "#00b0ff"

            cardsHtml.append("""
                <div class="card" onclick="openLightbox($idx)">
                    <div class="thumb-container">
                        <img src="/thumb/${item.id}" alt="$filename" loading="lazy" onerror="this.onerror=null;this.src='/file/${item.id}';">
                        <span class="badge" style="background:${badgeColor};">$typeBadge</span>
                    </div>
                    <div class="card-info">
                        <div class="filename" title="$filename">$filename</div>
                        <div class="filesize">$sizeStr</div>
                        <a href="/file/${item.id}" class="btn-download" download="$filename" onclick="event.stopPropagation()">Download</a>
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
            --bg-color: #f8fafc;
            --surface-color: #ffffff;
            --card-color: #ffffff;
            --primary-color: #4f46e5;
            --primary-hover: #4338ca;
            --accent-color: #059669;
            --text-main: #0f172a;
            --text-sub: #64748b;
            --border-color: #e2e8f0;
            --card-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
            --card-shadow-hover: 0 12px 20px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.04);
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            padding: 28px 16px 64px;
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
            background: #eef2ff;
            border: 1px solid #c7d2fe;
            padding: 6px 16px;
            border-radius: 9999px;
            font-size: 13px;
            color: var(--primary-color);
            font-weight: 600;
            margin-bottom: 12px;
        }
        h1 { font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 6px; color: var(--text-main); }
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
            box-shadow: var(--card-shadow);
        }
        .stats-col h2 { font-size: 18px; font-weight: 700; color: var(--text-main); }
        .stats-col p { color: var(--text-sub); font-size: 13px; margin-top: 4px; }
        .btn-zip {
            background: linear-gradient(135deg, var(--primary-color), #6366f1);
            color: #fff;
            text-decoration: none;
            font-weight: 700;
            font-size: 14px;
            padding: 12px 22px;
            border-radius: 12px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: transform 0.15s ease, box-shadow 0.15s ease;
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
        }
        .btn-zip:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4); }
        .upload-section {
            background: var(--surface-color);
            border: 2px dashed #cbd5e1;
            border-radius: 18px;
            padding: 28px 20px;
            text-align: center;
            margin-bottom: 32px;
            transition: border-color 0.2s, background-color 0.2s;
            cursor: pointer;
            box-shadow: var(--card-shadow);
        }
        .upload-section.dragover { border-color: var(--primary-color); background: #eef2ff; }
        .upload-section h3 { font-size: 16px; font-weight: 600; margin-bottom: 6px; color: var(--text-main); }
        .upload-section p { font-size: 13px; color: var(--text-sub); margin-bottom: 14px; }
        .btn-select {
            background: #eef2ff;
            color: var(--primary-color);
            border: 1px solid #c7d2fe;
            padding: 8px 18px;
            font-size: 13px;
            font-weight: 600;
            border-radius: 8px;
            cursor: pointer;
            transition: background 0.15s;
        }
        .btn-select:hover { background: #e0e7ff; }
        .progress-bar {
            width: 100%;
            height: 6px;
            background: #e2e8f0;
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
            transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
            box-shadow: var(--card-shadow);
            cursor: pointer;
        }
        .card:hover { transform: translateY(-3px); box-shadow: var(--card-shadow-hover); border-color: #cbd5e1; }
        .thumb-container {
            position: relative;
            width: 100%;
            height: 160px;
            background: #f1f5f9;
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
            background: rgba(15, 23, 42, 0.75);
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
            color: var(--text-main);
        }
        .filesize { font-size: 12px; color: var(--text-sub); margin-bottom: 12px; }
        .btn-download {
            background: #f8fafc;
            color: var(--text-main);
            border: 1px solid var(--border-color);
            text-align: center;
            text-decoration: none;
            padding: 8px 12px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            transition: all 0.15s;
        }
        .btn-download:hover { background: var(--primary-color); color: #fff; border-color: var(--primary-color); }
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
        /* Lightbox modal styles */
        .lightbox {
            position: fixed;
            top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(15, 23, 42, 0.94);
            backdrop-filter: blur(10px);
            display: none;
            flex-direction: column;
            z-index: 9999;
            user-select: none;
        }
        .lightbox.active { display: flex; }
        .lb-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 14px 24px;
            background: rgba(0, 0, 0, 0.45);
            color: #fff;
            gap: 16px;
        }
        .lb-info { display: flex; align-items: center; gap: 14px; font-size: 14px; min-width: 0; }
        #lbIndex { font-weight: 600; color: #94a3b8; font-size: 13px; }
        .lb-title { font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #fff; }
        .lb-size { color: #94a3b8; font-size: 12px; white-space: nowrap; }
        .lb-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
        .btn-lb-download {
            background: var(--primary-color);
            color: #fff;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            padding: 8px 16px;
            border-radius: 8px;
            transition: background 0.15s;
        }
        .btn-lb-download:hover { background: var(--primary-hover); }
        .btn-lb-close {
            background: rgba(255, 255, 255, 0.15);
            border: none;
            color: #fff;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            font-size: 18px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background 0.15s;
        }
        .btn-lb-close:hover { background: rgba(255, 255, 255, 0.3); }
        .lb-content {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 20px 24px;
            position: relative;
            overflow: hidden;
        }
        .lb-media-box {
            flex: 1;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }
        .lb-nav {
            background: rgba(255, 255, 255, 0.12);
            border: none;
            color: #fff;
            font-size: 32px;
            width: 48px;
            height: 64px;
            border-radius: 12px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background 0.15s, transform 0.1s;
            z-index: 10;
        }
        .lb-nav:hover { background: rgba(255, 255, 255, 0.25); transform: scale(1.05); }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="logo-row">
                <span>⚡ Direct Wi-Fi P2P</span> · <span>Offline & Private</span>
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

    <div id="lightbox" class="lightbox" onclick="closeLightbox(event)">
        <div class="lb-header">
            <div class="lb-info">
                <span id="lbIndex">1 / 1</span>
                <span id="lbTitle" class="lb-title"></span>
                <span id="lbSize" class="lb-size"></span>
            </div>
            <div class="lb-actions">
                <a id="lbDownload" class="btn-lb-download" download>⬇ Download</a>
                <button class="btn-lb-close" onclick="closeLightboxDirect()">✕</button>
            </div>
        </div>
        <div class="lb-content" onclick="event.stopPropagation()">
            <button class="lb-nav lb-prev" onclick="navLightbox(-1)">‹</button>
            <div id="lbMediaContainer" class="lb-media-box"></div>
            <button class="lb-nav lb-next" onclick="navLightbox(1)">›</button>
        </div>
    </div>

    <script>
        const sharedFiles = $itemsJson;
        let currentIndex = -1;

        function openLightbox(index) {
            if (index < 0 || index >= sharedFiles.length) return;
            currentIndex = index;
            const item = sharedFiles[index];
            const lb = document.getElementById('lightbox');
            const container = document.getElementById('lbMediaContainer');
            const title = document.getElementById('lbTitle');
            const size = document.getElementById('lbSize');
            const idxText = document.getElementById('lbIndex');
            const download = document.getElementById('lbDownload');

            title.innerText = item.name;
            size.innerText = item.size;
            idxText.innerText = (index + 1) + ' / ' + sharedFiles.length;
            download.href = '/file/' + item.id;
            download.setAttribute('download', item.name);

            if (item.isVideo) {
                container.innerHTML = '<video src="/file/' + item.id + '" controls autoplay playsinline style="max-width:90vw;max-height:80vh;border-radius:8px;box-shadow:0 10px 30px rgba(0,0,0,0.5);"></video>';
            } else {
                container.innerHTML = '<img src="/file/' + item.id + '" alt="' + item.name + '" style="max-width:90vw;max-height:80vh;object-fit:contain;border-radius:8px;box-shadow:0 10px 30px rgba(0,0,0,0.5);">';
            }
            lb.classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        function closeLightboxDirect() {
            const lb = document.getElementById('lightbox');
            const container = document.getElementById('lbMediaContainer');
            lb.classList.remove('active');
            container.innerHTML = '';
            document.body.style.overflow = '';
        }

        function closeLightbox(e) {
            if (e.target.id === 'lightbox' || e.target.classList.contains('lb-content')) {
                closeLightboxDirect();
            }
        }

        function navLightbox(direction) {
            if (sharedFiles.length === 0) return;
            let next = currentIndex + direction;
            if (next < 0) next = sharedFiles.length - 1;
            if (next >= sharedFiles.length) next = 0;
            openLightbox(next);
        }

        document.addEventListener('keydown', (e) => {
            const lb = document.getElementById('lightbox');
            if (lb.classList.contains('active')) {
                if (e.key === 'Escape') closeLightboxDirect();
                else if (e.key === 'ArrowLeft') navLightbox(-1);
                else if (e.key === 'ArrowRight') navLightbox(1);
            }
        });

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
            setTimeout(() => {
                bar.style.display = 'none';
                window.location.reload();
            }, 1200);
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

    private fun openItemInputStream(context: Context, item: MediaItem): InputStream? {
        return try {
            context.contentResolver.openInputStream(item.uri)
        } catch (_: Exception) {
            null
        } ?: try {
            val file = File(item.path)
            if (file.exists() && file.canRead()) FileInputStream(file) else null
        } catch (_: Exception) {
            null
        } ?: try {
            if (item.id > 0) {
                val contentUri = if (item is MediaItem.Video) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
                }
                context.contentResolver.openInputStream(contentUri)
            } else null
        } catch (_: Exception) {
            null
        }
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
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
                // If thumbnail generation completely fails, stream the file as fallback so UI shows preview
                serveSingleFile(item, emptyMap(), output)
            }
        } catch (_: Exception) {
            send404(output)
        }
    }

    private fun decodeFallbackThumbnail(context: Context, item: MediaItem): Bitmap? {
        return try {
            if (item is MediaItem.Video) {
                val retriever = MediaMetadataRetriever()
                try {
                    try {
                        retriever.setDataSource(context, item.uri)
                    } catch (_: Exception) {
                        val file = File(item.path)
                        if (file.exists()) {
                            retriever.setDataSource(file.absolutePath)
                        }
                    }
                    val frame = retriever.getFrameAtTime(1_000_000)
                    retriever.release()
                    frame
                } catch (_: Exception) {
                    try { retriever.release() } catch (_: Exception) {}
                    null
                }
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openItemInputStream(context, item)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                var sample = 1
                while (options.outWidth / (sample * 2) >= 320 && options.outHeight / (sample * 2) >= 320) {
                    sample *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                val rawBitmap = openItemInputStream(context, item)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
                val orientation = try {
                    openItemInputStream(context, item)?.use {
                        androidx.exifinterface.media.ExifInterface(it).getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                } catch (_: Exception) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                }
                val rotation = when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (rotation != 0f && rawBitmap != null) {
                    val matrix = Matrix().apply { postRotate(rotation) }
                    val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    rawBitmap.recycle()
                    rotated
                } else {
                    rawBitmap
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

        val inStream: InputStream = openItemInputStream(ctx, item) ?: return send404(output)

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
                        "Content-Disposition: inline; filename=\"$filename\"\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(headerStr.toByteArray(Charsets.UTF_8))

                stream.skip(start)
                copyStreamWithLimit(stream, output, contentLength)
            } else {
                val headerStr = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: ${item.mimeType}\r\n" +
                        "Content-Length: $fileLength\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Disposition: inline; filename=\"$filename\"\r\n" +
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
        val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L

        val ext = cleanFilename.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val isVideo = ext in setOf("mp4", "mkv", "webm", "mov", "avi", "3gp")
        val mimeType = headers["content-type"]?.takeIf { it != "application/octet-stream" && it.isNotBlank() }
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: if (isVideo) "video/mp4" else "image/jpeg"

        var targetFile: File? = null
        var targetUri: Uri? = null

        try {
            // First attempt: Scoped Storage insert via ContentResolver (guaranteed MediaStore realtime visibility)
            val collection = if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ImavaDrop")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            targetUri = try {
                ctx.contentResolver.insert(collection, values)
            } catch (_: Exception) {
                null
            }

            if (targetUri != null) {
                ctx.contentResolver.openOutputStream(targetUri)?.use { os ->
                    if (contentLength > 0L) {
                        copyStreamWithLimit(input, os, contentLength)
                    } else {
                        input.copyTo(os, 65536)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(targetUri, values, null, null)
            } else {
                // Fallback: Direct filesystem write to Pictures/ImavaDrop
                val dropDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ImavaDrop")
                if (!dropDir.exists()) dropDir.mkdirs()

                var file = File(dropDir, cleanFilename)
                if (file.exists()) {
                    val base = cleanFilename.substringBeforeLast('.')
                    file = File(dropDir, "${base}_${System.currentTimeMillis()}${if (ext.isNotEmpty()) ".$ext" else ""}")
                }
                targetFile = file

                FileOutputStream(file).use { fos ->
                    if (contentLength > 0L) {
                        copyStreamWithLimit(input, fos, contentLength)
                    } else {
                        input.copyTo(fos, 65536)
                    }
                }
                MediaScannerConnection.scanFile(ctx, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            }

            addLog("Received from $clientIp: $cleanFilename")
            _state.value = _state.value.copy(filesReceivedCount = _state.value.filesReceivedCount + 1)

            val json = """{"status":"ok","filename":"$cleanFilename"}"""
            val res = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${json.length}\r\nConnection: close\r\n\r\n$json"
            output.write(res.toByteArray(Charsets.UTF_8))
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            targetUri?.let { try { ctx.contentResolver.delete(it, null, null) } catch (_: Exception) {} }
            targetFile?.let { try { if (it.exists()) it.delete() } catch (_: Exception) {} }
            addLog("Upload failed from $clientIp: ${e.message}")

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
