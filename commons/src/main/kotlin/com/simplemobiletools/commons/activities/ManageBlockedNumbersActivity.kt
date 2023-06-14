package com.simplemobiletools.commons.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.adapters.ManageBlockedNumbersAdapter
import com.simplemobiletools.commons.dialogs.AddBlockedNumberDialog
import com.simplemobiletools.commons.dialogs.ExportBlockedNumbersDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.helpers.BlockedNumbersExporter.ExportResult
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.models.BlockedNumber
import kotlinx.android.synthetic.main.activity_manage_blocked_numbers.*
import java.io.FileOutputStream
import java.io.OutputStream

class ManageBlockedNumbersActivity : BaseSimpleActivity(), RefreshRecyclerViewListener {
    private val PICK_IMPORT_SOURCE_INTENT = 11
    private val PICK_EXPORT_FILE_INTENT = 21

    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_blocked_numbers)
        updateBlockedNumbers()
        setupOptionsMenu()

        updateMaterialActivityViews(block_numbers_coordinator, manage_blocked_numbers_list, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(manage_blocked_numbers_list, block_numbers_toolbar)
        updateTextColors(manage_blocked_numbers_wrapper)
        updatePlaceholderTexts()

        val blockTitleRes = if (baseConfig.appId.startsWith("com.simplemobiletools.dialer")) R.string.block_unknown_calls else R.string.block_unknown_messages

        block_unknown.apply {
            setText(blockTitleRes)
            isChecked = baseConfig.blockUnknownNumbers
            if (isChecked) {
                maybeSetDefaultCallerIdApp()
            }
        }

        block_unknown_holder.setOnClickListener {
            block_unknown.toggle()
            baseConfig.blockUnknownNumbers = block_unknown.isChecked
            if (block_unknown.isChecked) {
                maybeSetDefaultCallerIdApp()
            }
        }

        manage_blocked_numbers_placeholder_2.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                if (isDefaultDialer()) {
                    addOrEditBlockedNumber()
                } else {
                    launchSetDefaultDialerIntent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(block_numbers_toolbar, NavigationIcon.Arrow)
    }

    private fun setupOptionsMenu() {
        block_numbers_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_blocked_number -> {
                    addOrEditBlockedNumber()
                    true
                }
                R.id.import_blocked_numbers -> {
                    tryImportBlockedNumbers()
                    true
                }
                R.id.export_blocked_numbers -> {
                    tryExportBlockedNumbers()
                    true
                }
                else -> false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER && isDefaultDialer()) {
            updatePlaceholderTexts()
            updateBlockedNumbers()
        } else if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportBlockedNumbersFromFile(resultData.data!!)
        } else if (requestCode == PICK_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportBlockedNumbersTo(outputStream)
        } else if (requestCode == REQUEST_CODE_SET_DEFAULT_CALLER_ID && resultCode != Activity.RESULT_OK) {
            toast(R.string.must_make_default_caller_id_app, length = Toast.LENGTH_LONG)
            baseConfig.blockUnknownNumbers = false
            block_unknown.isChecked = false
        }
    }

    override fun refreshItems() {
        updateBlockedNumbers()
    }

    private fun updatePlaceholderTexts() {
        manage_blocked_numbers_placeholder.text = getString(if (isDefaultDialer()) R.string.not_blocking_anyone else R.string.must_make_default_dialer)
        manage_blocked_numbers_placeholder_2.text = getString(if (isDefaultDialer()) R.string.add_a_blocked_number else R.string.set_as_default)
    }

    private fun updateBlockedNumbers() {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers()
            runOnUiThread {
                ManageBlockedNumbersAdapter(this, blockedNumbers, this, manage_blocked_numbers_list) {
                    addOrEditBlockedNumber(it as BlockedNumber)
                }.apply {
                    manage_blocked_numbers_list.adapter = this
                }

                manage_blocked_numbers_placeholder.beVisibleIf(blockedNumbers.isEmpty())
                manage_blocked_numbers_placeholder_2.beVisibleIf(blockedNumbers.isEmpty())

                if (blockedNumbers.any { it.number.isBlockedNumberPattern() }) {
                    maybeSetDefaultCallerIdApp()
                }
            }
        }
    }

    private fun addOrEditBlockedNumber(currentNumber: BlockedNumber? = null) {
        AddBlockedNumberDialog(this, currentNumber) {
            updateBlockedNumbers()
        }
    }

    private fun tryImportBlockedNumbers() {
        if (isQPlus()) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"

                try {
                    startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    pickFileToImportBlockedNumbers()
                }
            }
        }
    }

    private fun tryImportBlockedNumbersFromFile(uri: Uri) {
        when (uri.scheme) {
            "file" -> importBlockedNumbers(uri.path!!)
            "content" -> {
                val tempFile = getTempFile("blocked", "blocked_numbers.txt")
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    importBlockedNumbers(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun pickFileToImportBlockedNumbers() {
        FilePickerDialog(this) {
            importBlockedNumbers(it)
        }
    }

    private fun importBlockedNumbers(path: String) {
        ensureBackgroundThread {
            val result = BlockedNumbersImporter(this).importBlockedNumbers(path)
            toast(
                when (result) {
                    BlockedNumbersImporter.ImportResult.IMPORT_OK -> R.string.importing_successful
                    BlockedNumbersImporter.ImportResult.IMPORT_FAIL -> R.string.no_items_found
                }
            )
            updateBlockedNumbers()
        }
    }

    private fun tryExportBlockedNumbers() {
        if (isQPlus()) {
            ExportBlockedNumbersDialog(this, baseConfig.lastBlockedNumbersExportPath, true) { file ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    try {
                        startActivityForResult(this, PICK_EXPORT_FILE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    ExportBlockedNumbersDialog(this, baseConfig.lastBlockedNumbersExportPath, false) { file ->
                        getFileOutputStream(file.toFileDirItem(this), true) { out ->
                            exportBlockedNumbersTo(out)
                        }
                    }
                }
            }
        }
    }

    private fun exportBlockedNumbersTo(outputStream: OutputStream?) {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers()
            if (blockedNumbers.isEmpty()) {
                toast(R.string.no_entries_for_exporting)
            } else {
                BlockedNumbersExporter().exportBlockedNumbers(blockedNumbers, outputStream) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> R.string.exporting_successful
                            ExportResult.EXPORT_FAIL -> R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    private fun maybeSetDefaultCallerIdApp() {
        if (isQPlus() && baseConfig.appId.startsWith("com.simplemobiletools.dialer")) {
            setDefaultCallerIdApp()
        }
    }
}
