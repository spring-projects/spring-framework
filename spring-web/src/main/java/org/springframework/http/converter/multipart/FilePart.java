/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.multipart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Specialization of {@link Part} that represents an uploaded file received in
 * a multipart request.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 7.1
 */
public interface FilePart extends Part {

	/**
	 * Return the original filename in the client's filesystem.
	 * <p><strong>Note:</strong> Please keep in mind this filename is supplied
	 * by the client and should not be used blindly. In addition to not using
	 * the directory portion, the file name could also contain characters such
	 * as ".." and others that can be used maliciously. It is recommended to not
	 * use this filename directly. Preferably generate a unique one and save
	 * this one somewhere for reference, if necessary.
	 * @return the original filename, or the empty String if no file has been chosen
	 * in the multipart form, or {@code null} if not defined or not available
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
	 * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
	 */
	String filename();

	/**
	 * Convenience method to copy the content of the file in this part to the
	 * given destination file. If the destination file already exists, it will
	 * be truncated first.
	 * <p>The default implementation delegates to {@link #transferTo(Path)}.
	 * @param dest the target file
	 * @throws IllegalStateException if the part isn't a file
	 * @see #transferTo(Path)
	 */
	default void transferTo(File dest) throws IOException {
		transferTo(dest.toPath());
	}

	/**
	 * Convenience method to copy the content of the file in this part to the
	 * given destination file. If the destination file already exists, it will
	 * be truncated first.
	 * @param dest the target file
	 * @throws IllegalStateException if the part isn't a file
	 * @see #transferTo(File)
	 */
	void transferTo(Path dest) throws IOException;

}
