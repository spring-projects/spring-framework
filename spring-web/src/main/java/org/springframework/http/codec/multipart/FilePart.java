/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.multipart;

import java.io.File;
import java.nio.file.Path;

import reactor.core.publisher.Mono;

/**
 * Specialization of {@link Part} that represents an uploaded file received in
 * a multipart request.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public interface FilePart extends Part {

	/**
	 * Return the original filename in the client's filesystem.
	 * <p><strong>Note:</strong> Please keep in mind this filename is supplied
	 * by the client and should not be used blindly. In addition to not using
	 * the directory portion, the file name could also contain characters such
	 * as ".." and others that can be used maliciously. It is recommended to not
	 * use this filename directly. Preferably generate a unique one and save
	 * this one one somewhere for reference, if necessary.
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
	 * @return completion {@code Mono} with the result of the file transfer,
	 * possibly {@link IllegalStateException} if the part isn't a file
	 * @see #transferTo(Path)
	 */
	default Mono<Void> transferTo(File dest) {
		return transferTo(dest.toPath());
	}

	/**
	 * Convenience method to copy the content of the file in this part to the
	 * given destination file. If the destination file already exists, it will
	 * be truncated first.
	 * @param dest the target file
	 * @return completion {@code Mono} with the result of the file transfer,
	 * possibly {@link IllegalStateException} if the part isn't a file
	 * @since 5.1
	 * @see #transferTo(File)
	 */
	Mono<Void> transferTo(Path dest);

}
