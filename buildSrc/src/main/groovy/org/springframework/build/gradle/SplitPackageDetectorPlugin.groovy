/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.build.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.maven.Conf2ScopeMapping
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.idea.IdeaPlugin

class SplitPackageDetectorPlugin implements Plugin<Project> {
	public void apply(Project project) {
		Task diagnoseSplitPackages = project.tasks.add('diagnoseSplitPackages', SplitPackageDetectorTask.class)
		diagnoseSplitPackages.setDescription('Detects packages which will be split across JARs')
	}
}

public class SplitPackageDetectorTask extends DefaultTask {
	@Input
	Set<Project> projectsToScan

	@TaskAction
	public final void diagnoseSplitPackages() {
		def Map<Project, Project> mergeMap = [:]
		def projects = projectsToScan.findAll { it.plugins.findPlugin(org.springframework.build.gradle.MergePlugin) }.findAll { it.merge.into }
		projects.each { p ->
			mergeMap.put(p, p.merge.into)
		}
		def splitFound = new org.springframework.build.gradle.SplitPackageDetector(projectsToScan, mergeMap, project.logger).diagnoseSplitPackages();
		assert !splitFound // see error log messages for details of split packages
	}
}

class SplitPackageDetector {

	private static final String HIDDEN_DIRECTORY_PREFIX = "."

	private static final String JAVA_FILE_SUFFIX = ".java"

	private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java"

	private static final String PACKAGE_SEPARATOR = "."

	private final Map<Project, Project> mergeMap

	private final Map<Project, Set<String>> pkgMap = [:]

	private final logger

	SplitPackageDetector(projectsToScan, mergeMap, logger) {
		this.mergeMap = mergeMap
		this.logger = logger
		projectsToScan.each { Project p ->
			def dir = p.projectDir
			def packages = getPackagesInDirectory(dir)
			if (!packages.isEmpty()) {
				pkgMap.put(p, packages)
			}
		}
	}

	private File[] dirList(String dir) {
		dirList(new File(dir))
	}

	private File[] dirList(File dir) {
		dir.listFiles({ file -> file.isDirectory() && !file.getName().startsWith(HIDDEN_DIRECTORY_PREFIX) } as FileFilter)
	}

	private Set<String> getPackagesInDirectory(File dir) {
		def pkgs = new HashSet<String>()
		addPackagesInDirectory(pkgs, new File(dir, SRC_MAIN_JAVA), "")
		return pkgs;
	}

	boolean diagnoseSplitPackages() {
		def splitFound = false;
		def projs = pkgMap.keySet().toArray()
		def numProjects = projs.length
		for (int i = 0; i < numProjects - 1; i++) {
			for (int j = i + 1; j < numProjects - 1; j++) {
				def pi = projs[i]
				def pkgi = new HashSet(pkgMap.get(pi))
				def pj = projs[j]
				def pkgj = pkgMap.get(pj)
				pkgi.retainAll(pkgj)
				if (!pkgi.isEmpty() && mergeMap.get(pi) != pj && mergeMap.get(pj) != pi) {
					pkgi.each { pkg ->
						def readablePkg = pkg.substring(1).replaceAll(File.separator, PACKAGE_SEPARATOR)
						logger.error("Package '$readablePkg' is split between $pi and $pj")
					}
					splitFound = true
				}
			}
		}
		return splitFound
	}

	private void addPackagesInDirectory(HashSet<String> packages, File dir, String pkg) {
		def scanDir = new File(dir, pkg)
		def File[] javaFiles = scanDir.listFiles({ file -> !file.isDirectory() && file.getName().endsWith(JAVA_FILE_SUFFIX) } as FileFilter)
		if (javaFiles != null && javaFiles.length != 0) {
			packages.add(pkg)
		}
		dirList(scanDir).each { File subDir ->
			addPackagesInDirectory(packages, dir, pkg + File.separator + subDir.getName())
		}
	}
}
