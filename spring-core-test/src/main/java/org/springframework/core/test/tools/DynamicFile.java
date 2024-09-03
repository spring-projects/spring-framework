/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.test.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class for dynamically generated files.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see SourceFile
 * @see ResourceFile
 */
public abstract sealed class DynamicFile permits SourceFile, ResourceFile {

	private final String path;

	private final String content;


	protected DynamicFile(String path, String content) {
		Assert.hasText(path, "'path' must not be empty");
		Assert.hasText(content, "'content' must not be empty");
		this.path = path;
		this.content = content;
	}


	protected static String toString(WritableContent writableContent) {
		try {
			StringBuilder stringBuilder = new StringBuilder();
			writableContent.writeTo(stringBuilder);
			return stringBuilder.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read content", ex);
		}
	}

	/**
	 * Return the contents of the file as a byte array.
	 * @return the file contents as a byte array
	 */
	public byte[] getBytes() {
		return this.content.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Return the contents of the file.
	 * @return the file contents
	 */
	public String getContent() {
		return this.content;
	}

	/**
	 * Return the relative path of the file.
	 * @return the file path
	 */
	public String getPath() {
		return this.path;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DynamicFile that &&
				this.path.equals(that.path) && this.content.equals(that.content)));
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@Override
	public String toString() {
		return this.path;
	}

}
