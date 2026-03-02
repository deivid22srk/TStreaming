package com.rk.terminal.ui.screens.bot

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.rk.libcommons.*
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.ui.screens.terminal.Rootfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.UnknownHostException

@Composable
fun SetupScreen(
    mainActivity: MainActivity,
    navController: NavHostController
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("Iniciando...") }
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val abi = Build.SUPPORTED_ABIS.firstOrNull { it in abiMap } ?: throw RuntimeException("CPU não suportada")
            val urls = abiMap[abi]!!

            val steps = listOf<Pair<String, suspend () -> Unit>>(
                "Baixando componentes básicos" to {
                    logs.add("Baixando talloc...")
                    downloadFile(urls.talloc, Rootfs.reTerminal.child("libtalloc.so.2"))
                    logs.add("Baixando proot...")
                    downloadFile(urls.proot, Rootfs.reTerminal.child("proot"))
                },
                "Baixando Alpine Linux" to {
                    logs.add("Baixando alpine rootfs...")
                    downloadFile(urls.alpine, Rootfs.reTerminal.child("alpine.tar.gz"))
                },
                "Extraindo sistema" to {
                    logs.add("Extraindo alpine.tar.gz (isso pode demorar)...")
                    extractAlpine { logs.add(it) }
                },
                "Configurando ambiente" to {
                    logs.add("Configurando binários e scripts de inicialização...")
                    setupBasicEnv(mainActivity)
                    // DNS FIX
                    try {
                        val etcDir = alpineDir().child("etc")
                        etcDir.mkdirs()
                        etcDir.child("resolv.conf").writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
                        logs.add("DNS configurado.")
                    } catch (e: Exception) {
                        logs.add("Erro ao configurar DNS: ${e.message}")
                    }
                },
                "Atualizando repositórios" to {
                    runInAlpine(mainActivity, "apk update") { line ->
                        progressText = "apk: $line"
                        logs.add(line)
                    }
                },
                "Instalando dependências do sistema" to {
                    runInAlpine(mainActivity, "apk add bash gcompat glib nano python3 py3-pip git build-base python3-dev libffi-dev openssl-dev") { line ->
                        progressText = "apk: $line"
                        logs.add(line)
                    }
                },
                "Clonando FileStreamBot" to {
                    runInAlpine(mainActivity, "git clone https://github.com/TheCaduceus/FileStreamBot.git /root/FileStreamBot") { line ->
                        progressText = "git: $line"
                        logs.add(line)
                    }
                },
                "Instalando dependências do Bot" to {
                    runInAlpine(mainActivity, "cd /root/FileStreamBot && export PIP_BREAK_SYSTEM_PACKAGES=1 && pip3 install -r requirements.txt") { line ->
                        progressText = "pip: $line"
                        logs.add(line)
                    }
                }
            )

            for (index in steps.indices) {
                val (label, action) = steps[index]
                progressText = label
                val baseProgress = index.toFloat() / steps.size
                val nextProgress = (index + 1).toFloat() / steps.size

                progress = baseProgress
                action()
                progress = nextProgress
            }

            progress = 1f
            progressText = "Concluído!"
            Rootfs.isDownloaded.value = true

            navController.navigate(MainActivityRoutes.BotScreen.route) {
                popUpTo(MainActivityRoutes.MainScreen.route) { inclusive = true }
            }

        } catch (e: Exception) {
            val errorMsg = "Erro: ${e.message}"
            progressText = errorMsg
            logs.add(errorMsg)
            e.printStackTrace()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Configuração do Sistema", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(progressText, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            color = Color.Black,
            shape = MaterialTheme.shapes.small
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        val text = logs.joinToString("\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Logs", text)
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Text("Copiar Logs", color = Color.Gray, fontSize = 10.sp)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 2.dp),
                    state = listState
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        if (progressText.startsWith("Erro")) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                navController.navigate(MainActivityRoutes.SetupScreen.route) {
                    popUpTo(MainActivityRoutes.SetupScreen.route) { inclusive = true }
                }
            }) {
                Text("Tentar Novamente")
            }
        }
    }
}

private suspend fun downloadFile(url: String, outputFile: File) {
    if (outputFile.exists()) return
    outputFile.parentFile?.mkdirs()
    withContext(Dispatchers.IO) {
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Falha ao baixar: ${response.code}")
            outputFile.outputStream().use { output ->
                response.body?.byteStream()?.copyTo(output)
            }
        }
    }
    outputFile.setExecutable(true, false)
}

private suspend fun extractAlpine(onLine: (String) -> Unit) {
    val alpineTar = Rootfs.reTerminal.child("alpine.tar.gz")
    val destDir = alpineDir()
    destDir.mkdirs()

    val process = ProcessBuilder("tar", "-vxf", alpineTar.absolutePath, "-C", destDir.absolutePath)
        .redirectErrorStream(true)
        .start()

    withContext(Dispatchers.IO) {
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) {
                    onLine(line!!)
                }
            }
        }
    }

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw Exception("Falha na extração: código $exitCode")
    }
}

private fun setupBasicEnv(activity: MainActivity) {
    val binDir = localBinDir()
    val libDir = localLibDir()

    Rootfs.reTerminal.child("proot").copyTo(binDir.child("proot"), overwrite = true)
    binDir.child("proot").setExecutable(true, false)

    Rootfs.reTerminal.child("libtalloc.so.2").copyTo(libDir.child("libtalloc.so.2"), overwrite = true)

    binDir.child("init-host").apply {
        writeText(activity.assets.open("init-host.sh").bufferedReader().use { it.readText() })
        setExecutable(true, false)
    }

    binDir.child("init").apply {
        writeText(activity.assets.open("init.sh").bufferedReader().use { it.readText() })
        setExecutable(true, false)
    }
}

private suspend fun runInAlpine(activity: MainActivity, command: String, onLine: (String) -> Unit) {
    val binDir = localBinDir()
    val libDir = localLibDir()
    val linker = if (File("/system/bin/linker64").exists()) "/system/bin/linker64" else "/system/bin/linker"
    val prefix = activity.filesDir.parentFile!!.path

    val prootCmd = mutableListOf(
        linker,
        binDir.child("proot").absolutePath,
        "--kill-on-exit",
        "-r", alpineDir().absolutePath,
        "-b", "/dev",
        "-b", "/proc",
        "-b", "/sys",
        "-b", "/sdcard",
        "-b", "/system",
        "-b", "/vendor",
        "-b", "/data",
        "-b", prefix,
        "-0",
        "--link2symlink",
        "--sysvipc",
        "-L",
        "-w", "/root",
        "/bin/sh", "-c", "export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin && $command"
    )

    val pb = ProcessBuilder(prootCmd)
    val env = pb.environment()
    env["LD_LIBRARY_PATH"] = libDir.absolutePath
    env["PREFIX"] = prefix
    env["PROOT_TMP_DIR"] = activity.cacheDir.child("proot_tmp").also { it.mkdirs() }.absolutePath

    val process = pb.redirectErrorStream(true).start()

    withContext(Dispatchers.IO) {
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) {
                    onLine(line!!)
                }
            }
        }
    }

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw Exception("Código $exitCode")
    }
}

private val abiMap = mapOf(
    "x86_64" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.0-x86_64.tar.gz"
    ),
    "arm64-v8a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"
    ),
    "armeabi-v7a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/armhf/alpine-minirootfs-3.21.0-armhf.tar.gz"
    )
)

private data class AbiUrls(val talloc: String, val proot: String, val alpine: String)
