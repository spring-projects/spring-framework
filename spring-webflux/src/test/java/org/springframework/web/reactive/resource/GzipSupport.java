/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 5.2.2
 */
class GzipSupport implements ParameterResolver {

	private static final Namespace namespace = Namespace.create(GzipSupport.class);


	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.getParameter().getType().equals(GzippedFiles.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return getStore(extensionContext).getOrComputeIfAbsent(GzippedFiles.class);
	}

	private Store getStore(ExtensionContext extensionContext) {
		return extensionContext.getStore(namespace);
	}


	static class GzippedFiles implements AutoCloseable {

		private final Set<File> created = new HashSet<>();

		void create(String filePath) {
			try {
				Resource location = new ClassPathResource("test/", getClass());
				Resource resource = new FileSystemResource(location.createRelative(filePath).getFile());

				Path gzFilePath = Paths.get(resource.getFile().getAbsolutePath() + ".gz");
				Files.deleteIfExists(gzFilePath);

				File gzFile = Files.createFile(gzFilePath).toFile();
				GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzFile));
				FileCopyUtils.copy(resource.getInputStream(), out);
				this.created.add(gzFile);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		public void close() {
			for (File file: this.created) {
				file.delete();
			}
		}
	}

}
