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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.CollectionUtils;

/**
 * Filesystem-based ahead-of-time (AOT) processing base implementation.
 *
 * <p>Concrete implementations are typically used to kick off optimization of an
 * application in a build tool.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.0
 * @see org.springframework.test.context.aot.TestAotProcessor
 */
public abstract class ContextAotProcessor extends AbstractAotProcessor<ClassName> {

	private final Class<?> applicationClass;


	/**
	 * Create a new processor for the specified application entry point and
	 * common settings.
	 * @param applicationClass the application entry point (class with a {@code main()} method)
	 * @param settings the settings to apply
	 */
	protected ContextAotProcessor(Class<?> applicationClass, Settings settings) {
		super(settings);
		this.applicationClass = applicationClass;
	}


	/**
	 * Get the application entry point (typically a class with a {@code main()} method).
	 */
	protected Class<?> getApplicationClass() {
		return this.applicationClass;
	}


	/**
	 * Invoke the processing by clearing output directories first, followed by
	 * {@link #performAotProcessing(GenericApplicationContext)}.
	 * @return the {@code ClassName} of the {@code ApplicationContextInitializer}
	 * entry point
	 */
	@Override
	protected ClassName doProcess() {
		deleteExistingOutput();
		GenericApplicationContext applicationContext = prepareApplicationContext(getApplicationClass());
		return performAotProcessing(applicationContext);
	}

	/**
	 * Prepare the {@link GenericApplicationContext} for the specified
	 * application entry point to be used against an {@link ApplicationContextAotGenerator}.
	 * @return a non-refreshed {@link GenericApplicationContext}
	 */
	protected abstract GenericApplicationContext prepareApplicationContext(Class<?> applicationClass);

	/**
	 * Perform ahead-of-time processing of the specified context.
	 * <p>Code, resources, and generated classes are stored in the configured
	 * output directories. In addition, run-time hints are registered for the
	 * application and its entry point.
	 * @param applicationContext the context to process
	 */
	protected ClassName performAotProcessing(GenericApplicationContext applicationContext) {
		FileSystemGeneratedFiles generatedFiles = createFileSystemGeneratedFiles();
		DefaultGenerationContext generationContext = new DefaultGenerationContext(
				createClassNameGenerator(), generatedFiles);
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		ClassName generatedInitializerClassName = generator.processAheadOfTime(applicationContext, generationContext);
		registerEntryPointHint(generationContext, generatedInitializerClassName);
		generationContext.writeGeneratedContent();
		writeHints(generationContext.getRuntimeHints());
		writeNativeImageProperties(getDefaultNativeImageArguments(getApplicationClass().getName()));
		return generatedInitializerClassName;
	}

	/**
	 * Callback to customize the {@link ClassNameGenerator}.
	 * <p>By default, a standard {@link ClassNameGenerator} using the configured
	 * {@linkplain #getApplicationClass() application entry point} as the default
	 * target is used.
	 * @return the class name generator
	 */
	protected ClassNameGenerator createClassNameGenerator() {
		return new ClassNameGenerator(ClassName.get(getApplicationClass()));
	}

	/**
	 * Return the native image arguments to use.
	 * <p>By default, the main class to use, as well as standard application flags
	 * are added.
	 * <p>If the returned list is empty, no {@code native-image.properties} is
	 * contributed.
	 * @param applicationClassName the fully qualified class name of the application
	 * entry point
	 * @return the native image options to contribute
	 */
	protected List<String> getDefaultNativeImageArguments(String applicationClassName) {
		List<String> args = new ArrayList<>();
		args.add("-H:Class=" + applicationClassName);
		args.add("--report-unsupported-elements-at-runtime");
		args.add("--no-fallback");
		args.add("--install-exit-handlers");
		return args;
	}

	private void registerEntryPointHint(DefaultGenerationContext generationContext,
			ClassName generatedInitializerClassName) {

		TypeReference generatedType = TypeReference.of(generatedInitializerClassName.canonicalName());
		TypeReference applicationType = TypeReference.of(getApplicationClass());
		ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
		reflection.registerType(applicationType);
		reflection.registerType(generatedType, typeHint -> typeHint.onReachableType(applicationType)
				.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
	}

	private void writeNativeImageProperties(List<String> args) {
		if (CollectionUtils.isEmpty(args)) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Args = ");
		sb.append(String.join(String.format(" \\%n"), args));
		Path file = getSettings().getResourceOutput().resolve("META-INF/native-image/" +
				getSettings().getGroupId() + "/" + getSettings().getArtifactId() + "/native-image.properties");
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				Files.createFile(file);
			}
			Files.writeString(file, sb.toString());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write native-image.properties", ex);
		}
	}

}
