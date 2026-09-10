/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.root

import androidx.preference.PreferenceManager
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.exceptions.ShellCommandInvalidException
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.fileoperations.filesystem.root.NativeOperations
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.filesystem.root.base.IRootCommand
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InterruptedIOException

object ListFilesCommand : IRootCommand() {
    private val log: Logger = LoggerFactory.getLogger(ListFilesCommand::class.java)

    /**
     * list files in given directory and invoke callback
     */
    fun listFiles(
        path: String,
        root: Boolean,
        showHidden: Boolean,
        openModeCallback: (openMode: OpenMode) -> Unit,
        onFileFoundCallback: (file: HybridFileParcelable) -> Unit,
    ) {
        val mode: OpenMode
        if (root && FileUtils.isRunningAboveStorage(path)) {
            // we're rooted and we're trying to load file with superuser
            // we're at the root directories, superuser is required!
            // Keep the modern stat parser as the normal root-listing path. The old implementation
            // special-cased `/` as a relative `*` glob and tried to repair the shared root shell's
            // working directory in separate `pwd`/`cd` jobs. That left the actual stat job dependent
            // on mutable shell state. executeRootCommand() now makes the `/` stat command
            // self-contained (`cd / && stat ... *`) so it has the same path stability as the legacy
            // absolute ls command without replacing the modern parser. Legacy remains a preference
            // and a bounded fallback when stat genuinely fails or produces no parseable entries.
            var result = executeRootCommand(path, showHidden)
            var parsedFiles =
                if (result.cancelled) {
                    emptyList<HybridFileParcelable>()
                } else {
                    parseRootListing(result.lines, path, isStat = !result.usedLs)
                }
            if (!result.cancelled && parsedFiles.isEmpty() && !result.usedLs) {
                log.warn(
                    "stat output for {} yielded no valid files; falling back to legacy ls",
                    path,
                )
                result = executeRootCommand(path, showHidden, retryWithLs = true)
                if (!result.cancelled) {
                    parsedFiles = parseRootListing(result.lines, path, isStat = false)
                }
            }
            parsedFiles.forEach(onFileFoundCallback)
            mode = OpenMode.ROOT
        } else if (FileUtils.canListFiles(File(path))) {
            // we're taking a chance to load files using basic java filesystem
            getFilesList(path, showHidden, onFileFoundCallback)
            mode = OpenMode.FILE
        } else {
            // we couldn't load files using native java filesystem callbacks
            // maybe the access is not allowed due to android system restrictions, we'll see later
            mode = OpenMode.FILE
        }
        openModeCallback(mode)
    }

    /**
     * Get open mode for path if it's a root path or normal storage
     */
    fun getOpenMode(
        path: String,
        root: Boolean,
    ): OpenMode {
        val mode: OpenMode =
            if (root && FileUtils.isRunningAboveStorage(path)) {
                OpenMode.ROOT
            } else {
                OpenMode.FILE
            }
        return mode
    }

    /**
     * Executes the privileged directory-listing command. Modern stat is the primary parser unless
     * the Legacy Root Listing preference is enabled; usedLs reports which parser produced lines and
     * cancelled distinguishes a lifecycle cancellation from a valid empty result.
     */
    @Throws(ShellNotRunningException::class)
    fun executeRootCommand(
        path: String,
        showHidden: Boolean,
        retryWithLs: Boolean = false,
    ): ExecuteRootCommandResult {
        val enforceLegacyFileListing: Boolean =
            PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
                .getBoolean(
                    PreferencesConstants.PREFERENCE_ROOT_LEGACY_LISTING,
                    false,
                )
        try {
            val sanitizedPath = RootHelper.getCommandLineString(path)
            val appendedPath = if (path == "/") "" else sanitizedPath.plus("/")

            // The modern parser used to run `stat ... *` for `/` only after separate `pwd` and
            // `cd /` shell jobs. libsu reuses a root shell, so cancellation or another shell job
            // between those calls could leave `*` resolving somewhere else (or yield an empty
            // response). Keep the same relative-name stat output expected by the parser, but reset
            // the working directory inside the very same shell job. All non-root paths already use
            // absolute globs and therefore do not depend on the shell's current directory.
            val statCommand =
                if (path == "/") {
                    "cd / && stat -c '%A %h %G %U %B %Y %N' *" +
                        (if (showHidden) " .* " else "")
                } else {
                    "stat -c '%A %h %G %U %B %Y %N' " +
                        "$appendedPath*" + (if (showHidden) " $appendedPath.* " else "")
                }

            return if (!retryWithLs && !enforceLegacyFileListing) {
                log.info("Using stat for list parsing")
                val statLines =
                    runShellCommandToList(statCommand).map { line ->
                        if (path == "/") line else line.replace(appendedPath, "")
                    }
                if (statLines.isEmpty()) {
                    // A transient/partial root-shell response must not blank the browser. Keep the
                    // legacy parser as a bounded recovery path, not as the normal `/` implementation.
                    log.warn("stat returned no entries for {}; retrying with ls", path)
                    executeRootCommand(path, showHidden, true)
                } else {
                    ExecuteRootCommandResult(lines = statLines, usedLs = false)
                }
            } else {
                log.info("Using ls for list parsing")
                ExecuteRootCommandResult(
                    lines =
                        runShellCommandToList(
                            "ls -l " + (if (showHidden) "-a " else "") +
                                "\"$sanitizedPath\"",
                        ),
                    usedLs = true,
                )
            }
        } catch (cancelled: InterruptedIOException) {
            // A cancelled libsu job is neither an empty directory nor a reason to launch another
            // root command from the same cancelled refresh. Let the caller preserve its cached
            // listing and stop here.
            log.debug("Root listing cancelled for {}", path)
            return ExecuteRootCommandResult(
                lines = emptyList(),
                usedLs = retryWithLs || enforceLegacyFileListing,
                cancelled = true,
            )
        } catch (invalidCommand: ShellCommandInvalidException) {
            log.warn("Command not found - ${invalidCommand.message}")
            return if (retryWithLs || enforceLegacyFileListing) {
                ExecuteRootCommandResult(lines = emptyList(), usedLs = true)
            } else {
                executeRootCommand(path, showHidden, true)
            }
        } catch (exception: ShellNotRunningException) {
            log.warn("failed to execute root command", exception)
            return ExecuteRootCommandResult(
                lines = emptyList(),
                usedLs = retryWithLs || enforceLegacyFileListing,
            )
        }
    }

    private fun parseRootListing(
        rawLines: List<String>,
        path: String,
        isStat: Boolean,
    ): List<HybridFileParcelable> {
        return rawLines
            .asSequence()
            .filterNot { it.contains("Permission denied") }
            .mapNotNull {
                parseStringForHybridFile(
                    rawFile = it,
                    path = path,
                    isStat = isStat,
                )
            }
            .toList()
    }

    private fun isDirectory(path: HybridFileParcelable): Boolean {
        return path.permission.startsWith("d") || File(path.path).isDirectory
    }

    /**
     * Loads files in a path using basic filesystem callbacks
     *
     * @param path the path
     */
    private fun getFilesList(
        path: String,
        showHidden: Boolean,
        listener: (HybridFileParcelable) -> Unit,
    ): ArrayList<HybridFileParcelable> {
        val pathFile = File(path)
        val files = ArrayList<HybridFileParcelable>()
        if (pathFile.exists() && pathFile.isDirectory) {
            val filesInPathFile = pathFile.listFiles()
            if (filesInPathFile != null) {
                filesInPathFile.forEach { currentFile ->
                    var size: Long = 0
                    if (!currentFile.isDirectory) size = currentFile.length()
                    HybridFileParcelable(
                        currentFile.path,
                        RootHelper.parseFilePermission(currentFile),
                        currentFile.lastModified(),
                        size,
                        currentFile.isDirectory,
                    ).let { baseFile ->
                        baseFile.name = currentFile.name
                        baseFile.mode = OpenMode.FILE
                        if (showHidden) {
                            files.add(baseFile)
                            listener(baseFile)
                        } else {
                            if (!currentFile.isHidden) {
                                files.add(baseFile)
                                listener(baseFile)
                            }
                        }
                    }
                }
            } else {
                log.error("Error listing files at [$path]. Access permission denied?")
                AppConfig.getInstance().run {
                    AppConfig.toast(this, this.getString(R.string.error_permission_denied))
                }
            }
        }
        return files
    }

    /**
     * Parses listing command result for HybridFile
     */
    private fun parseStringForHybridFile(
        rawFile: String,
        path: String,
        isStat: Boolean,
    ): HybridFileParcelable? {
        return FileUtils.parseName(
            if (isStat) {
                rawFile.replace(
                    "('|`)".toRegex(),
                    "",
                )
            } else {
                rawFile
            },
            isStat,
        )?.apply {
            this.mode = OpenMode.ROOT
            this.name = this.path
            if (path != "/") {
                this.path = path + "/" + this.path
            } else {
                // root of filesystem, don't concat another '/'
                this.path = path + this.path
            }
            if (this.link.trim { it <= ' ' }.isNotEmpty()) {
                if (isStat) {
                    isDirectory(this).let {
                        this.isDirectory = it
                        if (it) {
                            // stat command symlink includes time stamp at the end
                            // also, stat follows symlink by default if listing is invoked on it
                            // so we don't need link for stat
                            this.link = ""
                        }
                    }
                } else {
                    NativeOperations.isDirectory(this.link).let {
                        this.isDirectory = it
                    }
                }
            } else {
                this.isDirectory = isDirectory(this)
            }
        }
    }

    data class ExecuteRootCommandResult(
        val lines: List<String>,
        val usedLs: Boolean,
        val cancelled: Boolean = false,
    )
}
