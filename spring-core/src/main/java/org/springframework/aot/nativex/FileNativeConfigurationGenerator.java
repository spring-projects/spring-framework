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

package org.springframework.aot.nativex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.aot.hint.JavaSerializationHints;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.lang.Nullable;

/**
 * Generate the GraalVM native configuration files from runtime hints.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
public class FileNativeConfigurationGenerator implements NativeConfigurationGenerator {

	private final Path basePath;

	private final String groupId;

	private final String artifactId;

	public FileNativeConfigurationGenerator(Path basePath) {
		this(basePath, null, null);
	}

	public FileNativeConfigurationGenerator(Path basePath, @Nullable String groupId, @Nullable String artifactId) {
		this.basePath = basePath;
		if ((groupId == null && artifactId != null) || (groupId != null && artifactId == null)) {
			throw new IllegalArgumentException("groupId and artifactId must be both null or both non-null");
		}
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	@Override
	public void generate(RuntimeHints hints) {
		try {
			if (hints.javaSerialization().types().findAny().isPresent()) {
				generateFile(hints.javaSerialization());
			}
			if (hints.proxies().jdkProxies().findAny().isPresent()) {
				generateFile(hints.proxies());
			}
			if (hints.reflection().typeHints().findAny().isPresent()) {
				generateFile(hints.reflection());
			}
			if (hints.resources().resourcePatterns().findAny().isPresent() ||
					hints.resources().resourceBundles().findAny().isPresent()) {
				generateFile(hints.resources());
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unexpected I/O error while writing the native configuration", ex);
		}
	}

	/**
	 * Generate the Java serialization native configuration file.
	 */
	private void generateFile(JavaSerializationHints hints) throws IOException {
		JavaSerializationHintsSerializer serializer = new JavaSerializationHintsSerializer();
		File file = createIfNecessary("serialization-config.json");
		FileWriter writer = new FileWriter(file);
		writer.write(serializer.serialize(hints));
		writer.close();
	}

	/**
	 * Generate the proxy native configuration file.
	 */
	private void generateFile(ProxyHints hints) throws IOException {
		ProxyHintsSerializer serializer = new ProxyHintsSerializer();
		File file = createIfNecessary("proxy-config.json");
		FileWriter writer = new FileWriter(file);
		writer.write(serializer.serialize(hints));
		writer.close();
	}

	/**
	 * Generate the reflection native configuration file.
	 */
	private void generateFile(ReflectionHints hints) throws IOException {
		ReflectionHintsSerializer serializer = new ReflectionHintsSerializer();
		File file = createIfNecessary("reflect-config.json");
		FileWriter writer = new FileWriter(file);
		writer.write(serializer.serialize(hints));
		writer.close();
	}

	/**
	 * Generate the resource native configuration file.
	 */
	private void generateFile(ResourceHints hints) throws IOException {
		ResourceHintsSerializer serializer = new ResourceHintsSerializer();
		File file = createIfNecessary("resource-config.json");
		FileWriter writer = new FileWriter(file);
		writer.write(serializer.serialize(hints));
		writer.close();
	}

	private File createIfNecessary(String filename) throws IOException {
		Path outputDirectory = this.basePath.resolve("META-INF").resolve("native-image");
		if (this.groupId != null && this.artifactId != null) {
			outputDirectory = outputDirectory.resolve(this.groupId).resolve(this.artifactId);
		}
		outputDirectory.toFile().mkdirs();
		File file = outputDirectory.resolve(filename).toFile();
		file.createNewFile();
		return file;
	}

}
