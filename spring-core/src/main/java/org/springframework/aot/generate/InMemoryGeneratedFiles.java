/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aot.generate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * {@link GeneratedFiles} implementation that keeps generated files in-memory.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public class InMemoryGeneratedFiles implements GeneratedFiles {

	private final Map<Kind, Map<String, InputStreamSource>> files = new HashMap<>();


	@Override
	public void handleFile(Kind kind, String path, ThrowingConsumer<FileHandler> handler) {
		Map<String, InputStreamSource> paths = this.files.computeIfAbsent(kind,
				key -> new LinkedHashMap<>());
		handler.accept(new InMemoryFileHandler(paths, path));
	}

	/**
	 * Return a {@link Map} of the generated files of a specific {@link Kind}.
	 * @param kind the kind of generated file
	 * @return a {@link Map} of paths to {@link InputStreamSource} instances
	 */
	public Map<String, InputStreamSource> getGeneratedFiles(Kind kind) {
		Assert.notNull(kind, "'kind' must not be null");
		return Collections.unmodifiableMap(this.files.getOrDefault(kind, Collections.emptyMap()));
	}

	/**
	 * Return the content of the specified file.
	 * @param kind the kind of generated file
	 * @param path the path of the file
	 * @return the file content or {@code null} if no file could be found
	 * @throws IOException on read error
	 */
	@Nullable
	public String getGeneratedFileContent(Kind kind, String path) throws IOException {
		InputStreamSource source = getGeneratedFile(kind, path);
		if (source != null) {
			return new String(source.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		}
		return null;
	}

	/**
	 * Return the {@link InputStreamSource} of specified file.
	 * @param kind the kind of generated file
	 * @param path the path of the file
	 * @return the file source or {@code null} if no file could be found
	 */
	@Nullable
	public InputStreamSource getGeneratedFile(Kind kind, String path) {
		Assert.notNull(kind, "'kind' must not be null");
		Assert.hasLength(path, "'path' must not be empty");
		Map<String, InputStreamSource> paths = this.files.get(kind);
		return (paths != null ? paths.get(path) : null);
	}

	private static class InMemoryFileHandler extends FileHandler {

		private final Map<String, InputStreamSource> paths;

		private final String key;

		InMemoryFileHandler(Map<String, InputStreamSource> paths, String key) {
			super(paths.containsKey(key), () -> paths.get(key));
			this.paths = paths;
			this.key = key;
		}

		@Override
		protected void copy(InputStreamSource content, boolean override) {
			this.paths.put(this.key, content);
		}

		@Override
		public String toString() {
			return this.key;
		}
	}

}
