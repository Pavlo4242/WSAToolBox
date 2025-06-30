package kevin.page.pages

import kevin.LogUtils
import kevin.Main
import kevin.StringUtils.pathUp
import kevin.StringUtils.stringGet
import kevin.page.Page
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class FileManagerPage : Page("File Transfer", Main.width/5*2,0,Main.width/10,Main.height/40*3) {
    private val fileTable = JTable(arrayOf(),arrayOf("Name","Modification Time","Size","Number Files","Type"))
    private val fileTablePane by lazy {
        val scrollPane = JScrollPane(fileTable)
        scrollPane.setBounds(0,height/16*3 + height/40*3,width - width/50,height - height/16*5 - height/40*3)
        scrollPane.isVisible = false
        return@lazy scrollPane
    }
    private val upload = JButton("Upload")
    private val delete = JButton("Delete")
    private val download = JButton("Download")
    private val up = JButton("Up")
    private val path = JTextField("/storage/emulated/0")
    private val message = JLabel("Idle...")
    private val pathMessage = JLabel("Path:")
    private var nowPath = "/storage/emulated/0"
    private val files = arrayListOf<Pair<String,Int>>()
    init {
        fileTable.columnSelectionAllowed = false
        fileTable.enableInputMethods(false)
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        fileTable.rowHeight = (height - height/16*5)/10
        fileTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
        // "MS YaHei" is a font known for good Chinese character rendering. Kept for compatibility.
        fileTable.font = Font("MS YaHei", Font.PLAIN, 16)
        fileTable.columnModel.getColumn(0).preferredWidth = width/12*5
        fileTable.columnModel.getColumn(1).preferredWidth = width/24*5
        fileTable.columnModel.getColumn(2).preferredWidth = width/6
        fileTable.columnModel.getColumn(3).preferredWidth = width/15
        fileTable.columnModel.getColumn(4).preferredWidth = width/10
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
        fileTable.columnModel.getColumn(0).cellRenderer = cellRenderer
        fileTable.columnModel.getColumn(1).cellRenderer = cellRenderer
        fileTable.columnModel.getColumn(2).cellRenderer = cellRenderer
        fileTable.columnModel.getColumn(3).cellRenderer = cellRenderer
        fileTable.columnModel.getColumn(4).cellRenderer = cellRenderer
        fileTable.selectionModel.addListSelectionListener { updateButtons() }
        fileTable.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount==2){
                    if (files[fileTable.rowAtPoint(e.point)].second==1) return
                    val name = files[fileTable.rowAtPoint(e.point)].first
                    path.text = if (nowPath=="/")"/$name" else "$nowPath/$name"
                    nowPath = path.text
                    update()
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })

        upload.setBounds(width/50,height/10,width/20*3, height/40*3)
        upload.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    LogUtils.info("Select File to Upload")
                    val jFileChooser = JFileChooser()
                    jFileChooser.dialogTitle = "Select File"
                    jFileChooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
                    jFileChooser.isMultiSelectionEnabled = true
                    if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
                        message.text = "Uploading"
                        Main.window.update(Main.window.graphics)
                        jFileChooser.selectedFiles.forEach {
                            LogUtils.debug("Upload $it")
                            val command = if (it.isFile) {
                                "\"$nowPath\"/"
                            } else if (it.isDirectory) {
                                "\"$nowPath/${it.name}\""
                            } else {
                                LogUtils.error("Selection is neither a file nor a folder")
                                null
                            }
                            if (command != null) {
                                val process = Runtime.getRuntime().exec("${Main.aDBCommand} push \"$it\" $command")
                                val br = BufferedReader(InputStreamReader(process.inputStream))
                                var line: String?
                                while (br.readLine().also { l -> line = l } != null) {
                                    LogUtils.info("ADB: $line")
                                }
                                br.close()
                                LogUtils.debug("Upload of ${it} complete")
                            }
                        }
                        message.text = "Upload complete, refreshing..."
                        Main.window.update(Main.window.graphics)
                        LogUtils.info("Upload complete, refreshing...")
                        update()
                        message.text = "Upload Done, Refresh Finished"
                        LogUtils.info("Upload Done, Refresh Finished")
                    } else LogUtils.info("Cancelled")
                } catch (e:Exception){
                    LogUtils.error("Error during upload, $e")
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        upload.isVisible = false

        delete.setBounds(width/20*3+width/25,height/10,width/20*3, height/40*3)
        delete.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    if (!delete.isEnabled) return
                    LogUtils.info("Attempting to delete")
                    message.text = "Attempting to delete"
                    Main.window.update(Main.window.graphics)
                    val file = files[fileTable.selectedRow]
                    val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell rm${if (file.second>1) " -rf" else ""} \"${nowPath.replace(" ","\\ ")}/${file.first.replace(" ","\\ ")}\"")
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    var lastMessage = ""
                    while (br.readLine().also { l -> line = l } != null) {
                        LogUtils.info("ADB: $line")
                        lastMessage = line!!
                    }
                    br.close()
                    LogUtils.info("Delete complete")
                    message.text = "${if (lastMessage=="") "Delete Success" else try{lastMessage.split(": ")[2]}catch (e:Exception){"Delete Error"}}, Refreshing..."
                    Main.window.update(Main.window.graphics)
                    update()
                    message.text = "${if (lastMessage=="") "Delete Success" else try{lastMessage.split(": ")[2]}catch (e:Exception){"Delete Error"}}, Refresh Done"
                }catch (e:Exception){
                    LogUtils.error("Error on delete, $e")
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        delete.isVisible = false

        download.setBounds(width/10*3+width/50*3,height/10,width/20*3, height/40*3)
        download.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    if (!download.isEnabled) return
                    LogUtils.info("Download file")
                    val jFileChooser = JFileChooser()
                    jFileChooser.dialogTitle = "Select Save Location"
                    jFileChooser.dialogType = JFileChooser.SAVE_DIALOG
                    jFileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
                        val path = jFileChooser.selectedFile.toString().replace("\\","/")
                        LogUtils.debug("${Main.aDBCommand} pull \"$nowPath/${files[fileTable.selectedRow].first}\" \"${path}${if(!path.endsWith("/"))"/" else ""}${files[fileTable.selectedRow].first}\"")
                        message.text = "Downloading"
                        Main.window.update(Main.window.graphics)
                        val process = Runtime.getRuntime().exec("${Main.aDBCommand} pull \"$nowPath/${files[fileTable.selectedRow].first}\" \"${path}${if(!path.endsWith("/"))"/" else ""}${files[fileTable.selectedRow].first}\"")
                        val br = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?
                        var lastLine = ""
                        while (br.readLine().also { l -> line = l } != null) {
                            LogUtils.info("ADB: $line")
                            lastLine = line!!+","
                        }
                        br.close()
                        message.text = "${lastLine}Execution finished"
                        LogUtils.info("${lastLine}Execution finished")
                    } else LogUtils.info("Cancelled")
                }catch (e:Exception){
                    LogUtils.error("Error on download, $e")
                }
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        download.isVisible = false

        up.setBounds(width/20*9+width/50*4,height/10,width/20*3, height/40*3)
        up.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                if (!up.isEnabled) return
                nowPath = nowPath.pathUp()
                path.text = nowPath
                update()
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        up.isVisible = false

        path.setBounds(width/50*4,height/16*3,width - width/10, height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        path.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        path.addActionListener {
            while (path.text.endsWith("/")&&path.text!="/") path.text = path.text.removeSuffix("/")
            if (path.text=="") path.text = "/"
            nowPath = path.text
            update()
        }
        path.isVisible = false

        message.setBounds(width/10*6+width/50*5,height/10,width, height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        message.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        message.isVisible = false

        pathMessage.setBounds(width/50,height/16*3,width,height/40*3)
        // Original font was "宋体" (SimSun). Replaced with a more standard logical font.
        pathMessage.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        pathMessage.isVisible = false

        update()

        components += fileTablePane
        components += upload
        components += delete
        components += download
        components += up
        components += path
        components += message
        components += pathMessage
        addAll()
    }
    fun update(){
        val columns = fileTable.columnModel.columns.toList()
        val fileList = if (Main.adbState) getFiles() else {
            LogUtils.noConnectionInfo()
            null
        }
        files.clear()
        fileList?.forEach { files += it[0] to it[3].toInt() }
        fileTable.model = object : DefaultTableModel(fileList,arrayOf("Name","Modification Time","Size","Number of Files","Type")){
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
        columns.forEach {
            fileTable.columnModel.removeColumn(fileTable.columnModel.getColumn(0))
            fileTable.columnModel.addColumn(it)
        }
        updateButtons()
    }
    private fun updateButtons(){
        delete.isEnabled = Main.adbState && fileTable.selectedRow != -1
        download.isEnabled = Main.adbState && fileTable.selectedRow != -1
        up.isEnabled = Main.adbState && nowPath!= "/"
    }
    private fun getFiles(): Array<Array<String>>? {
        return try {
            LogUtils.info("Getting file list")
            val process = Runtime.getRuntime().exec("${Main.aDBCommand} shell ls $nowPath -all")
            val br = BufferedReader(InputStreamReader(process.inputStream,"UTF-8"))
            var line: String?
            val lines = arrayListOf<String>()
            while (br.readLine().also { l -> line = l } != null) {
                lines += line!!
            }
            br.close()
            val files = arrayListOf<Array<String>>()
            if (lines.first().startsWith("ls: //init: Permission denied")) {
                LogUtils.warn("ls: //init: Permission denied")
                lines.removeFirst()
            } else if (!lines.first().startsWith("total")) {
                LogUtils.error("Error, ${lines.first()}")
                return null
            }
            lines.removeFirst()
            lines.removeIf { it.stringGet(arrayOf(1,3),false, lastAll = false)[1] == "?" }
            for (l in lines){
                val array = l.stringGet(arrayOf(1,3,1,1,2),false, lastAll = true)
                val fileCount = array[0]
                val size = "${(array[1].toLong()/1024)}KB(${(array[1].toLong()/1024/1024)}MB)"
                val time = "${array[2]} ${array[3].split(".")[0]}"
                val name = array[4].replace("\\ "," ")
                if (name.contains(" ->")||name=="."||name=="..") continue
                files += arrayOf(name,time,size,fileCount,if(fileCount=="1") "File" else "Folder")
            }
            LogUtils.info("All files retrieved successfully!")
            if (files.isNotEmpty()) files.toTypedArray() else null
        }catch (e: Exception){
            LogUtils.error("Error getting files, $e")
            null
        }
    }
}
