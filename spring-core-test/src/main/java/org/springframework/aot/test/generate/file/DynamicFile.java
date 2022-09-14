/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.test.generate.file;

import java.io.IOException;
import java.util.Objects;

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
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DynamicFile other = (DynamicFile) obj;
		return Objects.equals(this.path, other.path)
				&& Objects.equals(this.content, other.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.path, this.content);
	}

	@Override
	public String toString() {
		return this.path;
	}

}
