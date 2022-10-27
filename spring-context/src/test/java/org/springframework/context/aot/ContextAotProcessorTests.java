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
 * Tests for {@link ContextAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class ContextAotProcessorTests {

	@Test
	void processGeneratesAssets(@TempDir Path directory) {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(SampleApplication.class);
		ContextAotProcessor processor = new DemoContextAotProcessor(SampleApplication.class, directory);
		ClassName className = processor.process();
		assertThat(className).isEqualTo(ClassName.get(SampleApplication.class.getPackageName(),
				"ContextAotProcessorTests_SampleApplication__ApplicationContextInitializer"));
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
		ContextAotProcessor processor = new DemoContextAotProcessor(SampleApplication.class,
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
		ContextAotProcessor processor = new DemoContextAotProcessor(SampleApplication.class, directory) {
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
					"source/org/springframework/context/aot/ContextAotProcessorTests_SampleApplication__ApplicationContextInitializer.java"))
					.exists().isRegularFile();
			assertThat(directory.resolve("source/org/springframework/context/aot/ContextAotProcessorTests__BeanDefinitions.java"))
					.exists().isRegularFile();
			assertThat(directory.resolve(
					"source/org/springframework/context/aot/ContextAotProcessorTests_SampleApplication__BeanFactoryRegistrations.java"))
					.exists().isRegularFile();
			assertThat(directory.resolve("resource/META-INF/native-image/com.example/example/reflect-config.json"))
					.exists().isRegularFile();
			Path nativeImagePropertiesFile = directory
					.resolve("resource/META-INF/native-image/com.example/example/native-image.properties");
			assertThat(nativeImagePropertiesFile).exists().isRegularFile().hasContent("""
					Args = -H:Class=org.springframework.context.aot.ContextAotProcessorTests$SampleApplication \\
					--report-unsupported-elements-at-runtime \\
					--no-fallback \\
					--install-exit-handlers
					""");
		};
	}


	private static class DemoContextAotProcessor extends ContextAotProcessor {

		DemoContextAotProcessor(Class<?> application, Path rootPath) {
			this(application, rootPath.resolve("source"), rootPath.resolve("resource"), rootPath.resolve("class"));
		}

		DemoContextAotProcessor(Class<?> application, Path sourceOutput, Path resourceOutput, Path classOutput) {
			super(application, createSettings(sourceOutput, resourceOutput, classOutput, "com.example", "example"));
		}

		private static Settings createSettings(Path sourceOutput, Path resourceOutput,
				Path classOutput, String groupId, String artifactId) {
			return Settings.builder()
					.sourceOutput(sourceOutput)
					.resourceOutput(resourceOutput)
					.classOutput(classOutput)
					.artifactId(artifactId)
					.groupId(groupId)
					.build();
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
