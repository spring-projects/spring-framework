/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.AssertProvider;

import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

/**
 * {@link DynamicFile} that holds resource file content and provides
 * {@link ResourceFileAssert} support.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class ResourceFile extends DynamicFile implements AssertProvider<ResourceFileAssert> {

	private ResourceFile(String path, String content) {
		super(path, content);
	}


	/**
	 * Factory method to create a new {@link ResourceFile} from the given
	 * {@link CharSequence}.
	 * @param path the relative path of the file or {@code null} to have the
	 * path deduced
	 * @param charSequence a char sequence containing the file contents
	 * @return a {@link ResourceFile} instance
	 */
	public static ResourceFile of(String path, CharSequence charSequence) {
		return new ResourceFile(path, charSequence.toString());
	}

	/**
	 * Factory method to create a new {@link ResourceFile} from the given
	 * {@code byte[]}.
	 * @param path the relative path of the file or {@code null} to have the
	 * path deduced
	 * @param bytes a byte array containing the file contents
	 * @return a {@link ResourceFile} instance
	 */
	public static ResourceFile of(String path, byte[] bytes) {
		return new ResourceFile(path, new String(bytes, StandardCharsets.UTF_8));
	}

	/**
	 * Factory method to create a new {@link ResourceFile} from the given
	 * {@link InputStreamSource}.
	 * @param path the relative path of the file
	 * @param inputStreamSource the source for the file
	 * @return a {@link ResourceFile} instance
	 */
	public static ResourceFile of(String path, InputStreamSource inputStreamSource) {
		return of(path, appendable -> appendable.append(FileCopyUtils.copyToString(
				new InputStreamReader(inputStreamSource.getInputStream(), StandardCharsets.UTF_8))));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link WritableContent}.
	 * @param path the relative path of the file
	 * @param writableContent the content to write to the file
	 * @return a {@link ResourceFile} instance
	 */
	public static ResourceFile of(String path, WritableContent writableContent) {
		return new ResourceFile(path, toString(writableContent));
	}

	/**
	 * Use {@code assertThat(sourceFile)} rather than calling this method
	 * directly.
	 */
	@Override
	public ResourceFileAssert assertThat() {
		return new ResourceFileAssert(this);
	}

}
