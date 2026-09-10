/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.os.SystemClock
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.base.IRootCommand
import org.slf4j.LoggerFactory

/** Calculates recursive directory usage for paths that require root access. */
object RootFolderSizeCommand : IRootCommand() {
    private const val CACHE_WINDOW_MS = 5_000L
    private val log = LoggerFactory.getLogger(RootFolderSizeCommand::class.java)

    private var cachedPath: String? = null
    private var cachedSize = -1L
    private var cachedAt = 0L

    /**
     * Returns the allocated size of [path] in bytes, or -1 when it cannot be measured.
     *
     * `du -k` is available through Android's toybox on supported devices and, unlike the file
     * metadata returned by `stat`, recursively measures directory contents. `-x` keeps a request
     * for `/` from walking into unrelated mounted filesystems. A short cache prevents the
     * properties dialog's size row and pie chart from launching the same expensive command twice.
     */
    @Synchronized
    fun calculate(path: String): Long {
        val now = SystemClock.elapsedRealtime()
        if (path == cachedPath && cachedSize >= 0L && now - cachedAt <= CACHE_WINDOW_MS) {
            return cachedSize
        }

        val sanitizedPath = RootHelper.getCommandLineString(path)
        // A trailing slash dereferences a directory symlink such as /sdcard without following
        // every symlink found below the directory.
        val commandPath = if (sanitizedPath == "/") "/" else sanitizedPath.trimEnd('/') + "/"
        val size =
            try {
                var result = runShellCommand("du -skx \"$commandPath\"")
                var kibibytes = parseKilobytes(result.out)
                if (kibibytes == null) {
                    result = runShellCommand("du -sk \"$commandPath\"")
                    kibibytes = parseKilobytes(result.out)
                }

                when {
                    kibibytes == null -> -1L
                    kibibytes > Long.MAX_VALUE / 1024L -> Long.MAX_VALUE
                    else -> kibibytes * 1024L
                }
            } catch (exception: ShellNotRunningException) {
                log.warn("Unable to calculate root folder size for {}", path, exception)
                -1L
            }

        if (size >= 0L) {
            cachedPath = path
            cachedSize = size
            cachedAt = SystemClock.elapsedRealtime()
        }
        return size
    }

    private fun parseKilobytes(lines: List<String>): Long? =
        lines
            .firstOrNull()
            ?.trim()
            ?.split(Regex("\\s+"), limit = 2)
            ?.firstOrNull()
            ?.toLongOrNull()
}
