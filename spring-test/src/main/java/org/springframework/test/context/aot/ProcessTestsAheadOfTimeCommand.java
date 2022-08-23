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

package org.springframework.test.context.aot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.aot.nativex.NativeConfigurationWriter;

/**
 * Command-line application that scans the provided classpath roots for Spring
 * integration test classes and then generates AOT artifacts for those test
 * classes in the provided output directories.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see TestClassScanner
 * @see TestContextAotGenerator
 * @see FileNativeConfigurationWriter
 */
@Command(mixinStandardHelpOptions = true, description = "Process test classes ahead of time")
public class ProcessTestsAheadOfTimeCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1..*", description = "Classpath roots for compiled test classes.")
	private Path[] testClasspathRoots;

	@Option(names = {"--packages"}, required = false, description = "Test packages to scan. This is optional any only intended for testing purposes.")
	private String[] packagesToScan = new String[0];

	@Option(names = {"--sources-out"}, required = true, description = "Output path for the generated sources.")
	private Path sourcesOutputPath;

	@Option(names = {"--resources-out"}, required = true, description = "Output path for the generated resources.")
	private Path resourcesOutputPath;


	@Override
	public Integer call() throws Exception {
		TestClassScanner testClassScanner = new TestClassScanner(Set.of(this.testClasspathRoots));
		Stream<Class<?>> testClasses = testClassScanner.scan(this.packagesToScan);

		// TODO Determine if we need to support CLASS output path.
		Path tempDir = Files.createTempDirectory("classes");
		GeneratedFiles generatedFiles = new FileSystemGeneratedFiles(kind -> switch(kind) {
				case SOURCE -> this.sourcesOutputPath;
				case RESOURCE -> this.resourcesOutputPath;
				case CLASS -> tempDir;
		});
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		generator.processAheadOfTime(testClasses);

		NativeConfigurationWriter writer = new FileNativeConfigurationWriter(this.resourcesOutputPath);
		writer.write(generator.getRuntimeHints());

		return 0;
	}

	static int execute(String[] args) throws Exception {
		return new CommandLine(new ProcessTestsAheadOfTimeCommand()).execute(args);
	}

	public static void main(String[] args) throws Exception {
		System.exit(execute(args));
	}

}
