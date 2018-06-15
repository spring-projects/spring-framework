/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.multipart.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link MultipartFile} implementation for Apache Commons FileUpload.
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see CommonsMultipartResolver
 */
@SuppressWarnings("serial")
public class CommonsMultipartFile implements MultipartFile, Serializable {

	protected static final Log logger = LogFactory.getLog(CommonsMultipartFile.class);

	private final FileItem fileItem;

	private final long size;

	private boolean preserveFilename = false;


	/**
	 * Create an instance wrapping the given FileItem.
	 * @param fileItem the FileItem to wrap
	 */
	public CommonsMultipartFile(FileItem fileItem) {
		this.fileItem = fileItem;
		this.size = this.fileItem.getSize();
	}


	/**
	 * Return the underlying {@code org.apache.commons.fileupload.FileItem}
	 * instance. There is hardly any need to access this.
	 */
	public final FileItem getFileItem() {
		return this.fileItem;
	}

	/**
	 * Set whether to preserve the filename as sent by the client, not stripping off
	 * path information in {@link CommonsMultipartFile#getOriginalFilename()}.
	 * <p>Default is "false", stripping off path information that may prefix the
	 * actual filename e.g. from Opera. Switch this to "true" for preserving the
	 * client-specified filename as-is, including potential path separators.
	 * @since 4.3.5
	 * @see #getOriginalFilename()
	 * @see CommonsMultipartResolver#setPreserveFilename(boolean)
	 */
	public void setPreserveFilename(boolean preserveFilename) {
		this.preserveFilename = preserveFilename;
	}


	@Override
	public String getName() {
		return this.fileItem.getFieldName();
	}

	@Override
	public String getOriginalFilename() {
		String filename = this.fileItem.getName();
		if (filename == null) {
			// Should never happen.
			return "";
		}
		if (this.preserveFilename) {
			// Do not try to strip off a path...
			return filename;
		}

		// Check for Unix-style path
		int unixSep = filename.lastIndexOf('/');
		// Check for Windows-style path
		int winSep = filename.lastIndexOf('\\');
		// Cut off at latest possible point
		int pos = (winSep > unixSep ? winSep : unixSep);
		if (pos != -1)  {
			// Any sort of path separator found...
			return filename.substring(pos + 1);
		}
		else {
			// A plain name
			return filename;
		}
	}

	@Override
	public String getContentType() {
		return this.fileItem.getContentType();
	}

	@Override
	public boolean isEmpty() {
		return (this.size == 0);
	}

	@Override
	public long getSize() {
		return this.size;
	}

	@Override
	public byte[] getBytes() {
		if (!isAvailable()) {
			throw new IllegalStateException("File has been moved - cannot be read again");
		}
		byte[] bytes = this.fileItem.get();
		return (bytes != null ? bytes : new byte[0]);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!isAvailable()) {
			throw new IllegalStateException("File has been moved - cannot be read again");
		}
		InputStream inputStream = this.fileItem.getInputStream();
		return (inputStream != null ? inputStream : StreamUtils.emptyInput());
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if (!isAvailable()) {
			throw new IllegalStateException("File has already been moved - cannot be transferred again");
		}

		if (dest.exists() && !dest.delete()) {
			throw new IOException(
					"Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
		}

		try {
			this.fileItem.write(dest);
			if (logger.isDebugEnabled()) {
				String action = "transferred";
				if (!this.fileItem.isInMemory()) {
					action = (isAvailable() ? "copied" : "moved");
				}
				logger.debug("Multipart file '" + getName() + "' with original filename [" +
						getOriginalFilename() + "], stored " + getStorageDescription() + ": " +
						action + " to [" + dest.getAbsolutePath() + "]");
			}
		}
		catch (FileUploadException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
		catch (IllegalStateException | IOException ex) {
			// Pass through IllegalStateException when coming from FileItem directly,
			// or propagate an exception from I/O operations within FileItem.write
			throw ex;
		}
		catch (Exception ex) {
			throw new IOException("File transfer failed", ex);
		}
	}

	/**
	 * Determine whether the multipart content is still available.
	 * If a temporary file has been moved, the content is no longer available.
	 */
	protected boolean isAvailable() {
		// If in memory, it's available.
		if (this.fileItem.isInMemory()) {
			return true;
		}
		// Check actual existence of temporary file.
		if (this.fileItem instanceof DiskFileItem) {
			return ((DiskFileItem) this.fileItem).getStoreLocation().exists();
		}
		// Check whether current file size is different than original one.
		return (this.fileItem.getSize() == this.size);
	}

	/**
	 * Return a description for the storage location of the multipart content.
	 * Tries to be as specific as possible: mentions the file location in case
	 * of a temporary file.
	 */
	public String getStorageDescription() {
		if (this.fileItem.isInMemory()) {
			return "in memory";
		}
		else if (this.fileItem instanceof DiskFileItem) {
			return "at [" + ((DiskFileItem) this.fileItem).getStoreLocation().getAbsolutePath() + "]";
		}
		else {
			return "on disk";
		}
	}

}
