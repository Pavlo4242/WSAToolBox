package kevin.page.pages
import kevin.LogUtils
import kevin.Main
import kevin.page.Page
import java.awt.Component
import java.awt.Font
import java.awt.Image
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.text.Collator
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.JTable.AUTO_RESIZE_OFF
import javax.swing.ListSelectionModel.SINGLE_SELECTION
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import kotlin.collections.ArrayList


class AppManagerPage : Page("ManageAPP", Main.width/5,0,Main.width/10,Main.height/40*3) {
    // The original headers were in English, but the model update used Chinese.
    // The headers are: "Icon|Name", "APP Package Name", "Version", "Status"
    private val appTable = JTable(arrayOf(), arrayOf("Icon|Name","APPPkg Name","Ver","Status"))
    private val appTablePane by lazy {
        val scrollPane = JScrollPane(appTable)
        scrollPane.setBounds(0,height/16*3,width - width/50,height - height/16*5)
        scrollPane.isVisible = false
        return@lazy scrollPane
    }
    private val refresh = JButton("Refresh")
    private val remove = JButton("Delete")
    private val start = JButton("Start")
    private val stop = JButton("Stop")
    private val message = JLabel("Idle...")
    private val appIcons = arrayListOf<ImageIcon>()
    private val appStates = arrayListOf<Boolean>()
    private val packageNames = arrayListOf<String>()
    init {
        appTable.columnSelectionAllowed = false
        appTable.enableInputMethods(false)
        appTable.setSelectionMode(SINGLE_SELECTION)
        appTable.rowHeight = (height - height/16*5)/10
        appTable.autoResizeMode = AUTO_RESIZE_OFF
        appTable.selectionModel.addListSelectionListener { updateButtons() }
        appTable.columnModel.getColumn(0).cellRenderer =
            TableCellRenderer { table, _, isSelected, _, row, column ->
                val l = JLabel(table.getValueAt(row,column)?.toString(),appIcons[row],JLabel.LEFT)
                if (isSelected) {
                    l.isOpaque = true
                    l.background = table.selectionBackground
                }
                l
            }
        // "MS YaHei" is a font known for good Chinese character rendering. Kept for compatibility.
        appTable.font = Font("MS YaHei", Font.PLAIN, 16)
        appTable.columnModel.getColumn(0).preferredWidth = width/5
        appTable.columnModel.getColumn(1).preferredWidth = width/3
        appTable.columnModel.getColumn(2).preferredWidth = width/8
        appTable.columnModel.getColumn(3).preferredWidth = width/8

        val cellRenderer = object : DefaultTableCellRenderer(){
            override fun getTableCellRendererComponent(
                table: JTable?,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                if (value is String) {
                    this.horizontalAlignment = CENTER
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            }
        }
        appTable.columnModel.getColumn(1).cellRenderer = cellRenderer
        appTable.columnModel.getColumn(2).cellRenderer = cellRenderer
        appTable.columnModel.getColumn(3).cellRenderer = cellRenderer
        update()

        refresh.setBounds(width/50,height/10,width/20*3, height/40*3)
        refresh.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                message.text = "Refreshing..."
                Main.window.update(Main.window.graphics)
                update()
                message.text = if (Main.adbState) "Refresh Complete" else "Not Connected to WSA"
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        refresh.isVisible = false

        remove.setBounds(width/25 + width/20*3,height/10,width/20*3, height/40*3)
        remove.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                if (!remove.isEnabled) return
                message.text = "Deleting..."
                Main.window.update(Main.window.graphics)
                message.text = if (Main.adbState) {
                    val process = Runtime.getRuntime().exec("${Main.aDBCommand} uninstall ${packageNames[appTable.selectedRow]}")
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    val lines = arrayListOf<String>()
                    while (br.readLine().also { line = it } != null) {
                        LogUtils.info("ADB: $line")
                        lines += line!!
                    }
                    br.close()
                    message.text = "${lines.last()}, Refreshing..."
                    Main.window.update(Main.window.graphics)
                    update()
                    "Refresh Complete"
                } else "Not Connected to WSA"
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        remove.isVisible = false

        start.setBounds(width/50*3 + width/20*6,height/10,width/20*3, height/40*3)
        start.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                message.text = "Attempting to start..."
                Main.window.update(Main.window.graphics)
                message.text = if (Main.adbState) {
                    val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell monkey -p ${packageNames[appTable.selectedRow]} -c android.intent.category.LAUNCHER 1")
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        LogUtils.info("ADB: $line")
                    }
                    br.close()
                    message.text = "Started, Refreshing..."
                    Main.window.update(Main.window.graphics)
                    update()
                    "Refresh Complete"
                } else "Not Connected to WSA"
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        start.isVisible = false

        stop.setBounds(width/50*4 + width/20*9,height/10,width/20*3, height/40*3)
        stop.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                if (!stop.isEnabled) return
                message.text = "Attempting to stop..."
                Main.window.update(Main.window.graphics)
                message.text = if (Main.adbState) {
                    val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell am force-stop  ${packageNames[appTable.selectedRow]}")
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        LogUtils.info("ADB: $line")
                    }
                    br.close()
                    message.text = "Stop complete, refreshing..."
                    Main.window.update(Main.window.graphics)
                    update()
                    "Refresh complete"
                } else "Not connected to WSA"
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        stop.isVisible = false

        message.setBounds(width/8 + width/20*12,height/10,width/4, height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        message.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        message.isVisible = false

        components += appTablePane
        components += refresh
        components += remove
        components += start
        components += stop
        components += message
        addAll()
    }
    private fun updateButtons(){
        val selected = appTable.selectedRow
        if (selected==-1){
            remove.isEnabled = false
            start.isEnabled = false
            stop.isEnabled = false
        } else {
            remove.isEnabled = Main.adbState
            stop.isEnabled = appStates[selected] && Main.adbState
            start.isEnabled = Main.adbState
        }
    }
    fun update(){
        val columns = appTable.columnModel.columns.toList()
        val appList = arrayListOf<Array<String?>>()
        appIcons.clear()
        appStates.clear()
        packageNames.clear()
        if (Main.adbState) getAllInstalledApps()?.forEach {
            appList += arrayOf(it[0],it[1],it[2],it[3])
            appIcons += getAppIcon(it[1]!!) ?: ImageIcon()
            // Original was: it[3] == "正在运行"
            appStates += it[3] == "Running"
            packageNames += it[1]!!
        } else LogUtils.noConnectionInfo()
        appTable.model = object : DefaultTableModel(appList.toTypedArray(),arrayOf("Icon|APP Name","APP Package Name","Version","Status")){
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
        columns.forEach {
            appTable.columnModel.removeColumn(appTable.columnModel.getColumn(0))
            appTable.columnModel.addColumn(it)
        }
        updateButtons()
    }
    private fun getAppIcon(packageName: String): ImageIcon? {
        return try {
            val icon = File("${System.getenv("USERPROFILE")}\\AppData\\Local\\Packages\\")
                .listFiles()
                ?.firstOrNull { it.name.contains("WindowsSubsystemForAndroid")&&it.isDirectory }
                ?.listFiles()
                ?.firstOrNull { it.name=="LocalState"&&it.isDirectory }
                ?.listFiles()
                ?.firstOrNull { it.name=="$packageName.png"&&it.isFile }
            if (icon==null) {
                LogUtils.debug("Icon for $packageName not found")
                null
            } else ImageIcon(ImageIO.read(icon).getScaledInstance((height - height/16*5)/10,(height - height/16*5)/10, Image.SCALE_DEFAULT))
        }catch (e: Exception){
            LogUtils.error("Error getting icon for $packageName, $e")
            null
        }
    }
    private fun getAllInstalledApps(): List<List<String?>>? {
        return try {
            val appList = CopyOnWriteArrayList<List<String?>>()
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell pm list packages -3")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            val appLines = lines.filter { it.startsWith("package:") }
                .map { it.removePrefix("package:") }
            val latch = CountDownLatch(appLines.size)
            appLines.forEach {
                Thread({
                    appList += listOf(getName(it), it, getVersion(it), getState(it))
                    latch.countDown()
                },"$it-GetMainThread") .start()
            }
            latch.await()
            appList.sortedBy { it[0] }.toList()
        } catch (e: Exception) {
            LogUtils.error("Error getting installed apps, $e")
            null
        }
    }
    private fun getName(packageName: String): String?{
        return try {
            LogUtils.info("Getting name for $packageName")
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell ls /data/local/tmp")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            val aapt = lines.find { it.contains("aapt-arm-pie") }
            if (aapt==null) {
                LogUtils.info("aapt not found, attempting to install")
                val aaptInputStream = this::class.java.getResourceAsStream("/aapt/aapt-arm-pie")
                val tmp = System.getProperty("java.io.tmpdir")
                val tmpDir = Files.createTempDirectory(Paths.get(tmp),"KevinWSAToolBox-AAPT")
                val tmpDirFile = tmpDir.toFile()
                LogUtils.info("aapt cache path: $tmpDirFile, cache will be deleted on exit")
                val fos = FileOutputStream(File("$tmpDirFile\\aapt-arm-pie"))
                fos.write(aaptInputStream!!.readAllBytes())
                aaptInputStream.close()
                fos.close()
                LogUtils.info("File released successfully!")
                Runtime.getRuntime().exec("${Main.aDBCommand} push \"$tmpDirFile\\aapt-arm-pie\" /data/local/tmp/aapt-arm-pie")
                LogUtils.info("File copied successfully!")
                Runtime.getRuntime().exec("${Main.aDBCommand} shell chmod 0755 /data/local/tmp/aapt-arm-pie")
                LogUtils.info("Permissions set successfully!")
                Thread.sleep(250)
                LogUtils.info("aapt installed successfully!")
            }
            val process3 = Runtime.getRuntime().exec("${Main.aDBCommand} shell pm path $packageName")
            lines.clear()
            val br3 = BufferedReader(InputStreamReader(process3.inputStream))
            while (br3.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br3.close()
            val path = lines.first().replace("package:","")
            val process4 = Runtime.getRuntime().exec("${Main.aDBCommand} shell /data/local/tmp/aapt-arm-pie d badging $path")
            lines.clear()
            val br4 = BufferedReader(InputStreamReader(process4.inputStream))
            while (br4.readLine().also { line = it } != null) {
                lines += line!!
            }
            br4.close()
            getName(lines)
        } catch (e: Exception) {
            LogUtils.error("Error getting name for $packageName, $e")
            null
        }
    }
    private fun getName(lines: ArrayList<String>): String? {
        // This logic correctly prioritizes Chinese locales then falls back to the default.
        // It does not need translation as it is parsing standard Android tool output.
        return lines.find { it.startsWith("application-label-zh-CN:'") }
            ?.removePrefix("application-label-zh-CN:'")
            ?.removeSuffix("'") ?: lines.find { it.startsWith("application-label-zh:'") }
            ?.removePrefix("application-label-zh:'")
            ?.removeSuffix("'") ?: lines.find { it.startsWith("application-label:'") }
            ?.removePrefix("application-label:'")
            ?.removeSuffix("'")
    }
    private fun getVersion(packageName: String): String?{
        return try {
            LogUtils.info("Getting version for $packageName")
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell pm dump $packageName")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                lines += line!!
            }
            br.close()
            lines.first{it.contains("versionName=")}
                .replace("versionName=","")
                .removeSpace()
        } catch (e: Exception) {
            LogUtils.error("Error getting version for $packageName, $e")
            null
        }
    }
    private fun getState(packageName: String): String?{
        return try {
            LogUtils.info("Getting status for $packageName")
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell pidof $packageName")
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { line = it } != null) {
                LogUtils.info("ADB: $line")
                lines += line!!
            }
            br.close()
            // Original was: if(...) "正在运行" else "未运行"
            if(lines.firstOrNull{it.isNotEmpty()}!=null) "Running" else "Not Running"
        } catch (e: Exception) {
            LogUtils.error("Error getting status for $packageName, $e")
            null
        }
    }
    private fun String.removeSpace(): String {
        var string = this
        while (string.startsWith(" ")) string = string.removePrefix(" ")
        return string
    }
}
