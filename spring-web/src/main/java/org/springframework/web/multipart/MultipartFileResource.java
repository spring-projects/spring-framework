/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.AbstractResource;
import org.springframework.util.Assert;

/**
 * Adapt {@link MultipartFile} to {@link org.springframework.core.io.Resource},
 * exposing the content as {@code InputStream} and also overriding
 * {@link #contentLength()} as well as {@link #getFilename()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see MultipartFile#getResource()
 */
class MultipartFileResource extends AbstractResource {

	private final MultipartFile multipartFile;


	public MultipartFileResource(MultipartFile multipartFile) {
		Assert.notNull(multipartFile, "MultipartFile must not be null");
		this.multipartFile = multipartFile;
	}


	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public long contentLength() {
		return this.multipartFile.getSize();
	}

	@Override
	public @Nullable String getFilename() {
		return this.multipartFile.getOriginalFilename();
	}

	/**
	 * This implementation throws IllegalStateException if attempting to
	 * read the underlying stream multiple times.
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		return this.multipartFile.getInputStream();
	}

	/**
	 * This implementation returns a description that has the Multipart name.
	 */
	@Override
	public String getDescription() {
		return "MultipartFile resource [" + this.multipartFile.getName() + "]";
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MultipartFileResource that &&
				this.multipartFile.equals(that.multipartFile)));
	}

	@Override
	public int hashCode() {
		return this.multipartFile.hashCode();
	}

}
