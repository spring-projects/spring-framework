/*
 * Copyright 2002-2013 the original author or authors.
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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Gradle plugin that detects identically named, non-empty packages split across multiple
 * subprojects, e.g. "org.springframework.context.annotation" existing in both spring-core
 * and spring-aspects. Adds a 'detectSplitPackages' task to the current project's task
 * collection. If the project already contains a 'check' task (i.e. is a typical Gradle
 * project with the "java" plugin applied), the 'check' task will be updated to depend on
 * the execution of 'detectSplitPackages'.
 *
 * By default, all subprojects will be scanned. Use the 'projectsToScan' task property to
 * modify this value. Example usage:
 *
 *     apply plugin: 'detect-split-packages // typically applied to root project
 *
 *     detectSplitPackages {
 *         packagesToScan -= project(":spring-xyz") // scan every project but spring-xyz
 *     }
 *
 * @author Rob Winch
 * @author Glyn Normington
 * @author Chris Beams
 */
public class DetectSplitPackagesPlugin implements Plugin<Project> {
	public void apply(Project project) {
		def tasks = project.tasks
		Task detectSplitPackages = tasks.add('detectSplitPackages', DetectSplitPackagesTask.class)
		if (tasks.asMap.containsKey('check')) {
			tasks.getByName('check').dependsOn detectSplitPackages
		}
	}
}

public class DetectSplitPackagesTask extends DefaultTask {

	private static final String JAVA_FILE_SUFFIX = ".java"
	private static final String PACKAGE_SEPARATOR = "."
	private static final String HIDDEN_DIRECTORY_PREFIX = "."

	@Input
	Set<Project> projectsToScan = project.subprojects

	public DetectSplitPackagesTask() {
		this.group = 'Verification'
		this.description = 'Detects packages split across two or more subprojects.'
	}

	@TaskAction
	public void detectSplitPackages() {
		def splitPackages = doDetectSplitPackages()
		if (!splitPackages.isEmpty()) {
			def message = "The following split package(s) have been detected:\n"
			splitPackages.each { pkg, mod ->
				message += " - ${pkg} (split across ${mod[0].name} and ${mod[1].name})\n"
			}
			throw new GradleException(message)
		}
	}

	private Map<String, List<Project>> doDetectSplitPackages() {
		def splitPackages = [:]
		def mergedProjects = findMergedProjects()
		def packagesByProject = mapPackagesByProject()

		def projects = packagesByProject.keySet().toArray()
		def nProjects = projects.length

		for (int i = 0; i < nProjects - 1; i++) {
			for (int j = i + 1; j < nProjects - 1; j++) {
				def prj_i = projects[i]
				def prj_j = projects[j]

				def pkgs_i = new HashSet(packagesByProject.get(prj_i))
				def pkgs_j = packagesByProject.get(prj_j)
				pkgs_i.retainAll(pkgs_j)

				if (!pkgs_i.isEmpty()
						&& mergedProjects.get(prj_i) != prj_j
						&& mergedProjects.get(prj_j) != prj_i) {
					pkgs_i.each { pkg ->
						def readablePkg = pkg.substring(1).replaceAll(File.separator, PACKAGE_SEPARATOR)
						splitPackages[readablePkg] = [prj_i, prj_j]
					}
				}
			}
		}
		return splitPackages;
	}

	private Map<Project, Set<String>> mapPackagesByProject() {
		def packagesByProject = [:]
		this.projectsToScan.each { Project p ->
			def packages = new HashSet<String>()
			p.sourceSets.main.java.srcDirs.each { File dir ->
				findPackages(packages, dir, "")
			}
			if (!packages.isEmpty()) {
				packagesByProject.put(p, packages)
			}
		}
		return packagesByProject;
	}

	private Map<Project, Project> findMergedProjects() {
		def mergedProjects = [:]
		this.projectsToScan.findAll { p ->
			p.plugins.findPlugin(MergePlugin)
		}.findAll { p ->
			p.merge.into
		}.each { p ->
			mergedProjects.put(p, p.merge.into)
		}
		return mergedProjects
	}

	private static void findPackages(Set<String> packages, File dir, String packagePath) {
		def scanDir = new File(dir, packagePath)
		def File[] javaFiles = scanDir.listFiles({ file ->
			!file.isDirectory() && file.name.endsWith(JAVA_FILE_SUFFIX)
		} as FileFilter)

		if (javaFiles != null && javaFiles.length != 0) {
			packages.add(packagePath)
		}

		scanDir.listFiles({ File file ->
			file.isDirectory() && !file.name.startsWith(HIDDEN_DIRECTORY_PREFIX)
		} as FileFilter).each { File subDir ->
			findPackages(packages, dir, packagePath + File.separator + subDir.name)
		}
	}
}

