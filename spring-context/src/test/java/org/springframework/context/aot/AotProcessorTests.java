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

package org.springframework.context.aot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AotProcessor}.
 *
 * @author Stephane Nicoll
 */
class AotProcessorTests {

	@Test
	void processGeneratesAssets(@TempDir Path directory) {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(SampleApplication.class);
		AotProcessor processor = new TestAotProcessor(SampleApplication.class, directory);
		ClassName className = processor.process();
		assertThat(className).isEqualTo(ClassName.get(SampleApplication.class.getPackageName(),
				"AotProcessorTests_SampleApplication__ApplicationContextInitializer"));
		assertThat(directory).satisfies(hasGeneratedAssetsForSampleApplication());
		context.close();
	}

	@Test
	void processingDeletesExistingOutput(@TempDir Path directory) throws IOException {
		Path sourceOutput = directory.resolve("source");
		Path resourceOutput = directory.resolve("resource");
		Path classOutput = directory.resolve("class");
		Path existingSourceOutput = createExisting(sourceOutput);
		Path existingResourceOutput = createExisting(resourceOutput);
		Path existingClassOutput = createExisting(classOutput);
		AotProcessor processor = new TestAotProcessor(SampleApplication.class,
				sourceOutput, resourceOutput, classOutput);
		processor.process();
		assertThat(existingSourceOutput).doesNotExist();
		assertThat(existingResourceOutput).doesNotExist();
		assertThat(existingClassOutput).doesNotExist();
	}

	@Test
	void processWithEmptyNativeImageArgumentsDoesNotCreateNativeImageProperties(@TempDir Path directory) {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(SampleApplication.class);
		AotProcessor processor = new TestAotProcessor(SampleApplication.class, directory) {
			@Override
			protected List<String> getDefaultNativeImageArguments(String application) {
				return Collections.emptyList();
			}
		};
		processor.process();
		assertThat(directory.resolve("resource/META-INF/native-image/com.example/example/native-image.properties"))
				.doesNotExist();
		context.close();
	}

	private Path createExisting(Path directory) throws IOException {
		Path existing = directory.resolve("existing");
		Files.createDirectories(directory);
		Files.createFile(existing);
		return existing;
	}

	private Consumer<Path> hasGeneratedAssetsForSampleApplication() {
		return directory -> {
			assertThat(directory.resolve(
					"source/org/springframework/context/aot/AotProcessorTests_SampleApplication__ApplicationContextInitializer.java"))
					.exists().isRegularFile();
			assertThat(directory.resolve("source/org/springframework/context/aot/AotProcessorTests__BeanDefinitions.java"))
					.exists().isRegularFile();
			assertThat(directory.resolve(
					"source/org/springframework/context/aot/AotProcessorTests_SampleApplication__BeanFactoryRegistrations.java"))
					.exists().isRegularFile();
			assertThat(directory.resolve("resource/META-INF/native-image/com.example/example/reflect-config.json"))
					.exists().isRegularFile();
			Path nativeImagePropertiesFile = directory
					.resolve("resource/META-INF/native-image/com.example/example/native-image.properties");
			assertThat(nativeImagePropertiesFile).exists().isRegularFile().hasContent("""
					Args = -H:Class=org.springframework.context.aot.AotProcessorTests$SampleApplication \\
					--report-unsupported-elements-at-runtime \\
					--no-fallback \\
					--install-exit-handlers
					""");
		};
	}


	private static class TestAotProcessor extends AotProcessor {

		public TestAotProcessor(Class<?> application,
				Path sourceOutput, Path resourceOutput, Path classOutput) {
			super(application, sourceOutput, resourceOutput, classOutput, "com.example", "example");
		}

		public TestAotProcessor(Class<?> application, Path rootPath) {
			super(application, rootPath.resolve("source"), rootPath.resolve("resource"),
					rootPath.resolve("class"), "com.example", "example");
		}

		@Override
		protected GenericApplicationContext prepareApplicationContext(Class<?> application) {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.register(application);
			return context;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SampleApplication {

		@Bean
		public String testBean() {
			return "Hello";
		}

	}

}
