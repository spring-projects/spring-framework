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

package org.springframework.build.shadow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle task to add source from shadowed jars into our own source jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class ShadowSource extends DefaultTask {

	private final List<Relocation> relocations = new ArrayList<>();


	@InputFiles
	@PathSensitive(PathSensitivity.NONE)
	public abstract ConfigurableFileCollection getSourceJars();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@Inject
	protected abstract FileSystemOperations getFileSystemOperations();

	@Inject
	protected abstract ArchiveOperations getArchiveOperations();

	@Inject
	protected abstract ObjectFactory getObjectFactory();

	public void setConfigurations(List<Configuration> configurations) {
		for (Configuration configuration : configurations) {
			getSourceJars().from(resolveSourceArtifacts(configuration));
		}
	}

	private FileCollection resolveSourceArtifacts(Configuration configuration) {
		ObjectFactory objects = getObjectFactory();
		return configuration.getIncoming().artifactView(view -> {
			view.withVariantReselection();
			view.attributes(attributes -> {
				attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
				attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.DOCUMENTATION));
				attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
				attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.class, DocsType.SOURCES));
			});
		}).getFiles();
	}

	@Nested
	public List<Relocation> getRelocations() {
		return this.relocations;
	}

	public void relocate(String pattern, String destination) {
		this.relocations.add(new Relocation(pattern, destination));
	}

	@TaskAction
	void syncSourceJarFiles() {
		getFileSystemOperations().sync(spec -> {
			spec.into(getOutputDirectory());
			spec.eachFile(this::relocateFile);
			spec.filter(this::transformContent);
			spec.exclude("META-INF/**");
			spec.setIncludeEmptyDirs(false);
			getSourceJars().forEach(sourceJar -> spec.from(zipTree(sourceJar)));
		});
	}

	private void relocateFile(FileCopyDetails details) {
		String path = details.getPath();
		for (Relocation relocation : this.relocations) {
			path = relocation.relocatePath(path);
		}
		details.setPath(path);
	}

	private String transformContent(String content) {
		for (Relocation relocation : this.relocations) {
			content = relocation.transformContent(content);
		}
		return content;
	}

	private FileTree zipTree(File sourceJar) {
		return getArchiveOperations().zipTree(sourceJar);
	}


	/**
	 * A single relocation.
	 */
	static class Relocation {

		private final String pattern;

		private final String pathPattern;

		private final String destination;

		private final String pathDestination;


		Relocation(String pattern, String destination) {
			this.pattern = pattern;
			this.pathPattern = pattern.replace('.', '/');
			this.destination = destination;
			this.pathDestination = destination.replace('.', '/');
		}


		@Input
		public String getPattern() {
			return this.pattern;
		}

		@Input
		public String getDestination() {
			return this.destination;
		}

		String relocatePath(String path) {
			return path.replace(this.pathPattern, this.pathDestination);
		}

		public String transformContent(String content) {
			return content.replaceAll("\\b" + this.pattern, this.destination);
		}

	}

}
