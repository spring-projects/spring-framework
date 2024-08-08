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
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * {@link GeneratedFiles} implementation that stores generated files using a
 * {@link FileSystem}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public class FileSystemGeneratedFiles implements GeneratedFiles {

	private final Function<Kind, Path> roots;


	/**
	 * Create a new {@link FileSystemGeneratedFiles} instance with all files
	 * stored under the specific {@code root}. The following subdirectories are
	 * created for the different file {@link Kind kinds}:
	 * <ul>
	 * <li>{@code sources}</li>
	 * <li>{@code resources}</li>
	 * <li>{@code classes}</li>
	 * </ul>
	 * @param root the root path
	 * @see #FileSystemGeneratedFiles(Function)
	 */
	public FileSystemGeneratedFiles(Path root) {
		this(conventionRoots(root));
	}

	/**
	 * Create a new {@link FileSystemGeneratedFiles} instance with all files
	 * stored under the root provided by the given {@link Function}.
	 * @param roots a function that returns the root to use for the given
	 * {@link Kind}
	 */
	public FileSystemGeneratedFiles(Function<Kind, Path> roots) {
		Assert.notNull(roots, "'roots' must not be null");
		Assert.isTrue(Arrays.stream(Kind.values()).map(roots).noneMatch(Objects::isNull),
				"'roots' must return a value for all file kinds");
		this.roots = roots;
	}


	private static Function<Kind, Path> conventionRoots(Path root) {
		Assert.notNull(root, "'root' must not be null");
		return kind -> switch (kind) {
			case SOURCE -> root.resolve("sources");
			case RESOURCE -> root.resolve("resources");
			case CLASS -> root.resolve("classes");
		};
	}

	@Override
	public void handleFile(Kind kind, String path, ThrowingConsumer<FileHandler> handler) {
		FileSystemFileHandler fileHandler = new FileSystemFileHandler(toPath(kind, path));
		handler.accept(fileHandler);
	}

	private Path toPath(Kind kind, String path) {
		Assert.notNull(kind, "'kind' must not be null");
		Assert.hasLength(path, "'path' must not be empty");
		Path root = this.roots.apply(kind).toAbsolutePath().normalize();
		Path relativePath = root.resolve(path).toAbsolutePath().normalize();
		Assert.isTrue(relativePath.startsWith(root), "'path' must be relative");
		return relativePath;
	}

	static final class FileSystemFileHandler extends FileHandler {

		private final Path path;

		FileSystemFileHandler(Path path) {
			super(Files.exists(path), () -> new FileSystemResource(path));
			this.path = path;
		}

		@Override
		protected void copy(InputStreamSource content, boolean override) {
			if (override) {
				copy(content, StandardCopyOption.REPLACE_EXISTING);
			}
			else {
				copy(content);
			}
		}

		private void copy(InputStreamSource content, CopyOption... copyOptions) {
			try {
				try (InputStream inputStream = content.getInputStream()) {
					Files.createDirectories(this.path.getParent());
					Files.copy(inputStream, this.path, copyOptions);
				}
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public String toString() {
			return this.path.toString();
		}
	}

}
