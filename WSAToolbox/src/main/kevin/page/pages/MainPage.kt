package kevin.page.pages

import kevin.LogUtils
import kevin.Main
import kevin.page.Page
import java.awt.Font
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.swing.JButton
import javax.swing.JLabel

class MainPage : Page("Home",0,0,Main.width/10,Main.height/40*3) {
    private val restartServer = JButton("Restart ADB Service")
    private val startSettings = JButton("Open Android Settings")
    private val adbVersion = JLabel()
    private val messageConnected = JLabel()
    private val wsaVersion = JLabel()
    private val wsaKernel = JLabel()
    private val wsaMemory = JLabel()
    private val wsaCPU = JLabel()
    init {
        //RestartServer
        restartServer.setBounds(width/100*3,height/10,width/10*3,height/40*3)
        restartServer.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                try {
                    LogUtils.debug("Executing Restart ADB server...")
                    val process = Runtime.getRuntime().exec("${Main.aDBCommand} kill-server")
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        LogUtils.info("ADB: $line")
                    }
                    br.close()
                    LogUtils.info("Kill ADB Server successful!")
                    messageConnected.text = "Connection lost..."
                    wsaVersion.text = "Reloading..."
                    wsaKernel.text = "Reloading..."
                    wsaMemory.text = "Reloading..."
                    wsaCPU.text = "Reloading..."
                    Main.window.update(Main.window.graphics)
                    val processStart = Runtime.getRuntime().exec("${Main.aDBCommand} connect 127.0.0.1:58526")
                    val brStart = BufferedReader(InputStreamReader(processStart.inputStream))
                    val lines = arrayListOf<String>()
                    while (brStart.readLine().also { line = it } != null) {
                        LogUtils.info("ADB: $line")
                        lines += line!!
                    }
                    brStart.close()
                    Main.adbState = if (!lines.last().contains("connected to")&&!lines.last().contains("already connected to")){
                        LogUtils.error("ADB connection error, please start WSA and enable developer mode, then try to reconnect!")
                        false
                    } else {
                        LogUtils.info("Start ADB Server successful!")
                        true
                    }
                    messageConnected.text = if (Main.adbState) "Connected to WSA!" else "Not Connected"
                    wsaVersion.text = if (Main.adbState) getWSAVersion() else "No connection"
                    wsaKernel.text = if (Main.adbState) getWSAKernel() else "No connection"
                    wsaMemory.text = if (Main.adbState) getWSAMemory() else "No connection"
                    wsaCPU.text = if (Main.adbState) getWSACPU() else "No connection"
                    Main.window.update(Main.window.graphics)
                    Main.appManager.update()
                    Main.taskManager.update()
                    Main.fileManager.update()
                    LogUtils.debug("Restart ADB server execution complete")
                }catch (e: IOException){
                    LogUtils.error("Restart ADB server error: $e")
                }
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
        })
        restartServer.isVisible = false
        //Start settings
        startSettings.setBounds(width/50*3 + width/10*3,height/10,width/10*3,height/40*3)
        startSettings.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    LogUtils.info("Attempting to open settings...")
                    if (!Main.adbState) {
                        LogUtils.info("Not connected to WSA, cannot open settings")
                        return
                    }
                    val process = Runtime.getRuntime().exec("${Main.aDBCommand}  shell monkey -p com.android.settings -c android.intent.category.LAUNCHER 1")
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        LogUtils.info("ADB: $line")
                    }
                    br.close()
                    LogUtils.info("Settings started successfully!")
                }catch (e: Exception){
                    LogUtils.error("Exception while starting settings, $e")
                }
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
        })
        startSettings.isVisible = false
        //ADBVersion
        adbVersion.setBounds(width/100*3,height/5+height/40*3,width/4*3,height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        adbVersion.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        adbVersion.text = getAdbVersion()
        adbVersion.isVisible = false
        //MessageConnected
        messageConnected.setBounds(width/100*3,height/5,width/4*3,height/40*3)
        messageConnected.font = Font(Font.SANS_SERIF, Font.BOLD, 25)
        messageConnected.text = if (Main.adbState) "Connected to WSA!" else "Not Connected"
        messageConnected.isVisible = false
        //WSAVersion
        wsaVersion.setBounds(width/50*3,height/5+height/20*3,width/4*3,height/40*3)
        wsaVersion.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        wsaVersion.text = if (Main.adbState) getWSAVersion() else "No connection"
        wsaVersion.isVisible = false
        //WSAKernel
        wsaKernel.setBounds(width/50*3,height/5+height/40*9,width,height/40*3)
        wsaKernel.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        wsaKernel.text = if (Main.adbState) getWSAKernel() else "No connection"
        wsaKernel.isVisible = false
        //WSAMemory
        wsaMemory.setBounds(width/50*3,height/5+height/10*3,width,height/40*3)
        wsaMemory.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        wsaMemory.text = if (Main.adbState) getWSAMemory() else "No connection"
        wsaMemory.isVisible = false
        //WSACPU
        wsaCPU.setBounds(width/50*3,height/5+height/8*3,width,height/40*3)
        wsaCPU.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        wsaCPU.text = if (Main.adbState) getWSACPU() else "No connection"
        wsaCPU.isVisible = false

        components.add(restartServer)
        components += startSettings
        components.add(adbVersion)
        components.add(messageConnected)
        components.add(wsaVersion)
        components.add(wsaKernel)
        components.add(wsaMemory)
        components.add(wsaCPU)
        addAll()
    }
    private fun getWSACPU(): String{
        return try {
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell cat /proc/cpuinfo")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            val core = lines.first { it.startsWith("cpu cores") }.replace(" ","").replace("\t","").replace("cpucores:","").toInt()
            val thread = lines.filter { it.startsWith("processor") }.size
            val cpuName = lines.first { it.startsWith("model name") }.replace("model name\t: ","")
            val frequency = lines.first { it.startsWith("cpu MHz") }.replace("cpu MHz\t\t: ","")
            "Processor: $cpuName (${core} cores ${thread} threads, Base Frequency: ${frequency}MHz)"
        } catch (e: Exception){
            LogUtils.error("Error getting WSA processor, $e")
            "Processor: Error"
        }
    }
    private fun getWSAMemory(): String{
        return try {
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell cat /proc/meminfo")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            val mem = lines.first().replace(" ","").replace("MemTotal:","").replace("kB","",true).toInt()
            "Memory: ${mem/1024}MB (${mem}KB)"
        } catch (e: Exception){
            LogUtils.error("Error getting WSA memory, $e")
            "Memory: Error"
        }
    }
    private fun getWSAKernel(): String{
        return try {
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell cat /proc/version")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            val message = lines.first().split(" ")
            "Android Kernel: ${message[0]} ${message[1]} ${message[2]}"
        } catch (e: Exception){
            LogUtils.error("Error getting WSA kernel, $e")
            "Android Kernel: Error"
        }
    }
    private fun getWSAVersion(): String{
        return try {
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell getprop ro.build.version.release")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            "Android Version: ${lines.first()}"
        } catch (e: Exception){
            LogUtils.error("Error getting WSA version, $e")
            "Android Version: Error"
        }
    }
    private fun getAdbVersion(): String{
        return try {
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} version")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            // Original logic checked if the command was "adb", and labeled it "系统" (System) or "内置" (Built-in).
            "${lines.first()} (${if(Main.aDBCommand == "adb")"System" else "Built-in"})"
        } catch (e: Exception){
            LogUtils.error("Error getting ADB version, $e")
            "Adb Version: Error"
        }
    }
}
