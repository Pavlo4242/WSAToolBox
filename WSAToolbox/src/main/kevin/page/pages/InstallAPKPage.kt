package kevin.page.pages

import kevin.LogUtils
import kevin.Main
import kevin.page.Page
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.filechooser.FileFilter

class InstallAPKPage : Page("Install APK", Main.width/10,0,Main.width/10,Main.height/40*3) {
    private val pathText by lazy { JLabel() }
    private val apkPath by lazy { JTextField() }
    private val installButton by lazy { JButton() }
    private val pathButton by lazy { JButton() }
    private val infoText by lazy { JLabel() }
    init {
        pathText.setBounds(width/100*3,height/10,width/10*3,height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        pathText.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        pathText.text = "Path:"
        pathText.isVisible = false

        apkPath.setBounds(width/10,height/10,width - width/20*3,height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        apkPath.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        apkPath.addCaretListener { if(File(apkPath.text).isFile&& apkPath.text.endsWith(".apk",true)) apkPath.foreground = Color(100,200,100) else apkPath.foreground = Color(255,100,100) }
        apkPath.isVisible = false

        installButton.setBounds(width/100*3,height/10 + height/40*3,width - width/20*3 + width/10 - width/100*3,height/40*3)
        installButton.text = "Install"
        installButton.isVisible = false
        installButton.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                try {
                    LogUtils.debug("Attempting to install...")
                    if(File(apkPath.text).isFile&& apkPath.text.endsWith(".apk",true)) {
                        if (!Main.adbState) {
                            infoText.text = "Not connected to WSA, please connect and retry"
                            LogUtils.debug("Not connected to WSA, please connect and retry")
                            return
                        }
                        infoText.text = "Installing..."
                        Main.window.update(Main.window.graphics)
                        val process = Runtime.getRuntime().exec("${Main.aDBCommand} install \"${apkPath.text}\"")
                        val br = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?
                        val lines = arrayListOf<String>()
                        while (br.readLine().also { line = it } != null) {
                            LogUtils.info("ADB: $line")
                            lines += line!!
                        }
                        br.close()
                        infoText.text = "${lines.first()},${lines.last()}"
                        LogUtils.debug("Installation finished")
                    } else {
                        infoText.text = "Incorrect path or unsupported file type"
                        LogUtils.debug("Incorrect path or unsupported file type")
                    }
                }catch (e:Exception){
                    LogUtils.error("Installation error, $e")
                    infoText.text = "Installation error, $e"
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })

        pathButton.setBounds(width/100*3,height/10 + height/20*3,width - width/20*3 + width/10 - width/100*3,height/40*3)
        pathButton.text = "Select APK"
        pathButton.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    LogUtils.debug("Select APK")
                    val jFileChooser = JFileChooser()
                    jFileChooser.dialogTitle = "Please select an APK file"
                    jFileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    jFileChooser.isMultiSelectionEnabled = false
                    jFileChooser.isAcceptAllFileFilterUsed = false
                    jFileChooser.addChoosableFileFilter(object : FileFilter() {
                        override fun accept(pathname: File) = pathname.isDirectory || pathname.toString().endsWith(".apk")
                        override fun getDescription() = "Package files (*.apk)"
                    })
                    if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
                        apkPath.text = jFileChooser.selectedFile.toString()
                    } else LogUtils.info("Cancelled")
                    LogUtils.debug("Selection complete")
                } catch (e:Exception){
                    LogUtils.error("Selection error, $e")
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        pathButton.isVisible = false

        infoText.setBounds(width/100*3,height/10 + height/40*9,width - width/20*3 + width/10 - width/100*3,height/40*3)
        infoText.isVisible = false
        infoText.text = "Idle..."

        components.add(pathText)
        components.add(apkPath)
        components.add(installButton)
        components.add(pathButton)
        components.add(infoText)
        addAll()
    }
}
