/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.io.File;
import java.io.IOException;

/**
 * Utility methods for working with the file system.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.5.3
 */
public abstract class FileSystemUtils {

	/**
	 * Delete the supplied {@link File} - for directories,
	 * recursively delete any nested directories or files as well.
	 * @param root the root {@code File} to delete
	 * @return {@code true} if the {@code File} was deleted,
	 * otherwise {@code false}
	 */
	public static boolean deleteRecursively(File root) {
		if (root != null && root.exists()) {
			if (root.isDirectory()) {
				File[] children = root.listFiles();
				if (children != null) {
					for (File child : children) {
						deleteRecursively(child);
					}
				}
			}
			return root.delete();
		}
		return false;
	}

	/**
	 * Recursively copy the contents of the {@code src} file/directory
	 * to the {@code dest} file/directory.
	 * @param src the source directory
	 * @param dest the destination directory
	 * @throws IOException in the case of I/O errors
	 */
	public static void copyRecursively(File src, File dest) throws IOException {
		Assert.isTrue(src != null && (src.isDirectory() || src.isFile()), "Source File must denote a directory or file");
		Assert.notNull(dest, "Destination File must not be null");
		doCopyRecursively(src, dest);
	}

	/**
	 * Actually copy the contents of the {@code src} file/directory
	 * to the {@code dest} file/directory.
	 * @param src the source directory
	 * @param dest the destination directory
	 * @throws IOException in the case of I/O errors
	 */
	private static void doCopyRecursively(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			dest.mkdir();
			File[] entries = src.listFiles();
			if (entries == null) {
				throw new IOException("Could not list files in directory: " + src);
			}
			for (File entry : entries) {
				doCopyRecursively(entry, new File(dest, entry.getName()));
			}
		}
		else if (src.isFile()) {
			try {
				dest.createNewFile();
			}
			catch (IOException ex) {
				IOException ioex = new IOException("Failed to create file: " + dest);
				ioex.initCause(ex);
				throw ioex;
			}
			FileCopyUtils.copy(src, dest);
		}
		else {
			// Special File handle: neither a file not a directory.
			// Simply skip it when contained in nested directory...
		}
	}

}
