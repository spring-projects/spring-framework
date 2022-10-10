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
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileSystemUtils;

/**
 * Filesystem-based ahead-of-time processing base implementation. Typically
 * used to kick off the optimizations of an application in a build tool.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 6.0
 */
public abstract class AotProcessor {

	private final Class<?> application;

	private final Path sourceOutput;

	private final Path resourceOutput;

	private final Path classOutput;

	private final String groupId;

	private final String artifactId;

	/**
	 * Create a new processor instance.
	 * @param application the application entry point
	 * @param sourceOutput the location of generated sources
	 * @param resourceOutput the location of generated resources
	 * @param classOutput the location of generated classes
	 * @param groupId the group ID of the application, used to locate
	 * {@code native-image.properties}
	 * @param artifactId the artifact ID of the application, used to locate
	 * {@code native-image.properties}
	 */
	protected AotProcessor(Class<?> application, Path sourceOutput, Path resourceOutput,
			Path classOutput, String groupId, String artifactId) {

		this.application = application;
		this.sourceOutput = sourceOutput;
		this.resourceOutput = resourceOutput;
		this.classOutput = classOutput;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	/**
	 * Prepare the {@link GenericApplicationContext} for the specified
	 * application to be used against an {@link ApplicationContextAotGenerator}.
	 * @return a non-refreshed {@link GenericApplicationContext}
	 */
	protected abstract GenericApplicationContext prepareApplicationContext(Class<?> application);


	/**
	 * Invoke the processing by clearing output directories first, followed by
	 * {@link #performAotProcessing(GenericApplicationContext)}.
	 * @return the {@code ClassName} of the {@code ApplicationContextInitializer}
	 * entry point
	 */
	public ClassName process() {
		deleteExistingOutput();
		GenericApplicationContext applicationContext = prepareApplicationContext(this.application);
		return performAotProcessing(applicationContext);
	}

	/**
	 * Delete the source, resource, and class output directories.
	 */
	protected void deleteExistingOutput() {
		deleteExistingOutput(this.sourceOutput, this.resourceOutput, this.classOutput);
	}

	/**
	 * Perform ahead-of-time processing of the specified context.
	 * <p>Code, resources, and generated classes are stored in the configured
	 * output directories. In addition, run-time hints are registered for the
	 * application and its entry point.
	 * @param applicationContext the context to process
	 */
	protected ClassName performAotProcessing(GenericApplicationContext applicationContext) {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this::getRoot);
		DefaultGenerationContext generationContext = new DefaultGenerationContext(
				createClassNameGenerator(), generatedFiles);
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		ClassName generatedInitializerClassName = generator.processAheadOfTime(applicationContext, generationContext);
		registerEntryPointHint(generationContext, generatedInitializerClassName);
		generationContext.writeGeneratedContent();
		writeHints(generationContext.getRuntimeHints());
		writeNativeImageProperties(getDefaultNativeImageArguments(this.application.getName()));
		return generatedInitializerClassName;
	}

	/**
	 * Callback to customize the {@link ClassNameGenerator}. By default, a
	 * standard {@link ClassNameGenerator} using the configured application
	 * as the default target is used.
	 * @return the class name generator
	 */
	protected ClassNameGenerator createClassNameGenerator() {
		return new ClassNameGenerator(ClassName.get(this.application));
	}

	/**
	 * Return the native image arguments to use. By default, the main
	 * class to use, as well as standard application flags are added.
	 * <p>If the returned list is empty, no {@code native-image.properties} is
	 * contributed.
	 * @param application the application entry point
	 * @return the native image options to contribute
	 */
	protected List<String> getDefaultNativeImageArguments(String application) {
		List<String> args = new ArrayList<>();
		args.add("-H:Class=" + application);
		args.add("--report-unsupported-elements-at-runtime");
		args.add("--no-fallback");
		args.add("--install-exit-handlers");
		return args;
	}

	private void registerEntryPointHint(DefaultGenerationContext generationContext,
			ClassName generatedInitializerClassName) {

		TypeReference generatedType = TypeReference.of(generatedInitializerClassName.canonicalName());
		TypeReference applicationType = TypeReference.of(this.application);
		ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
		reflection.registerType(applicationType);
		reflection.registerType(generatedType, typeHint -> typeHint.onReachableType(applicationType)
				.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
	}

	private void writeHints(RuntimeHints hints) {
		FileNativeConfigurationWriter writer =
				new FileNativeConfigurationWriter(this.resourceOutput, this.groupId, this.artifactId);
		writer.write(hints);
	}

	private void writeNativeImageProperties(List<String> args) {
		if (CollectionUtils.isEmpty(args)) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Args = ");
		sb.append(String.join(String.format(" \\%n"), args));
		Path file = this.resourceOutput
				.resolve("META-INF/native-image/" + this.groupId + "/" + this.artifactId + "/native-image.properties");
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				Files.createFile(file);
			}
			Files.writeString(file, sb.toString());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write native-image properties", ex);
		}
	}

	private void deleteExistingOutput(Path... paths) {
		for (Path path : paths) {
			try {
				FileSystemUtils.deleteRecursively(path);
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to delete existing output in '" + path + "'");
			}
		}
	}

	private Path getRoot(Kind kind) {
		return switch (kind) {
			case SOURCE -> this.sourceOutput;
			case RESOURCE -> this.resourceOutput;
			case CLASS -> this.classOutput;
		};
	}

}
