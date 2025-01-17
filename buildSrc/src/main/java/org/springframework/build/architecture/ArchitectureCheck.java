/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.build.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import static org.springframework.build.architecture.ArchitectureRules.*;

/**
 * {@link Task} that checks for architecture problems.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public abstract class ArchitectureCheck extends DefaultTask {

	private FileCollection classes;

	public ArchitectureCheck() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		getProhibitObjectsRequireNonNull().convention(true);
		getRules().addAll(packageInfoShouldBeNullMarked(),
				classesShouldNotImportForbiddenTypes(),
				javaClassesShouldNotImportKotlinAnnotations(),
				allPackagesShouldBeFreeOfTangles(),
				noClassesShouldCallStringToLowerCaseWithoutLocale(),
				noClassesShouldCallStringToUpperCaseWithoutLocale());
		getRuleDescriptions().set(getRules().map((rules) -> rules.stream().map(ArchRule::getDescription).toList()));
	}

	@TaskAction
	void checkArchitecture() throws IOException {
		JavaClasses javaClasses = new ClassFileImporter()
				.importPaths(this.classes.getFiles().stream().map(File::toPath).toList());
		List<EvaluationResult> violations = getRules().get()
				.stream()
				.map((rule) -> rule.evaluate(javaClasses))
				.filter(EvaluationResult::hasViolation)
				.toList();
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		outputFile.getParentFile().mkdirs();
		if (!violations.isEmpty()) {
			StringBuilder report = new StringBuilder();
			for (EvaluationResult violation : violations) {
				report.append(violation.getFailureReport());
				report.append(String.format("%n"));
			}
			Files.writeString(outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			throw new GradleException("Architecture check failed. See '" + outputFile + "' for details.");
		}
		else {
			outputFile.createNewFile();
		}
	}

	public void setClasses(FileCollection classes) {
		this.classes = classes;
	}

	@Internal
	public FileCollection getClasses() {
		return this.classes;
	}

	@InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	@PathSensitive(PathSensitivity.RELATIVE)
	final FileTree getInputClasses() {
		return this.classes.getAsFileTree();
	}

	@Optional
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getResourcesDirectory();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@Internal
	public abstract ListProperty<ArchRule> getRules();

	@Internal
	public abstract Property<Boolean> getProhibitObjectsRequireNonNull();

	@Input
	// The rules themselves can't be an input as they aren't serializable so we use
	// their descriptions instead
	abstract ListProperty<String> getRuleDescriptions();
}
