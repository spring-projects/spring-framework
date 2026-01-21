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

package org.springframework.build;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.dokka.gradle.DokkaExtension;
import org.jetbrains.dokka.gradle.DokkaPlugin;
import org.jetbrains.kotlin.gradle.dsl.JvmTarget;
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

/**
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class KotlinConventions {

	void apply(Project project) {
		project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> {
			project.getTasks().withType(KotlinCompile.class, this::configure);
			if (project.getLayout().getProjectDirectory().dir("src/main/kotlin").getAsFile().exists()) {
				project.getPlugins().apply(DokkaPlugin.class);
				project.getExtensions().configure(DokkaExtension.class, dokka -> configure(project, dokka));
				project.project(":framework-api").getDependencies().add("dokka", project);
			}
		});
	}

	private void configure(KotlinCompile compile) {
		compile.compilerOptions(options -> {
			options.getApiVersion().set(KotlinVersion.KOTLIN_2_2);
			options.getLanguageVersion().set(KotlinVersion.KOTLIN_2_2);
			options.getJvmTarget().set(JvmTarget.JVM_17);
			options.getJavaParameters().set(true);
			options.getAllWarningsAsErrors().set(true);
			options.getFreeCompilerArgs().addAll(
					"-Xsuppress-version-warnings",
					"-Xjsr305=strict", // For dependencies using JSR 305
					"-opt-in=kotlin.RequiresOptIn",
					"-Xjdk-release=17", // Needed due to https://youtrack.jetbrains.com/issue/KT-49746
					"-Xannotation-default-target=param-property" // Upcoming default, see https://youtrack.jetbrains.com/issue/KT-73255
			);
		});
	}

	private void configure(Project project, DokkaExtension dokka) {
		dokka.getDokkaSourceSets().forEach(sourceSet -> {
			sourceSet.getSourceRoots().setFrom(project.file("src/main/kotlin"));
			sourceSet.getClasspath()
					.from(project.getExtensions()
							.getByType(SourceSetContainer.class)
							.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
							.getOutput());
			var externalDocumentationLinks = sourceSet.getExternalDocumentationLinks();
			var springVersion = project.getVersion();
			externalDocumentationLinks.register("spring-framework", spec -> {
				spec.url("https://docs.spring.io/spring-framework/docs/" + springVersion + "/javadoc-api/");
				spec.packageListUrl("https://docs.spring.io/spring-framework/docs/" + springVersion + "/javadoc-api/element-list");
			});
			externalDocumentationLinks.register("reactor-core", spec ->
					spec.url("https://projectreactor.io/docs/core/release/api/"));
			externalDocumentationLinks.register("reactive-streams", spec ->
					spec.url("https://www.reactive-streams.org/reactive-streams-1.0.3-javadoc/"));
			externalDocumentationLinks.register("kotlinx-coroutines", spec ->
					spec.url("https://kotlinlang.org/api/kotlinx.coroutines/"));
			externalDocumentationLinks.register("hamcrest", spec ->
					spec.url("https://javadoc.io/doc/org.hamcrest/hamcrest/2.1/"));
			externalDocumentationLinks.register("jakarta-servlet", spec -> {
				spec.url("https://javadoc.io/doc/jakarta.servlet/jakarta.servlet-api/latest/");
				spec.packageListUrl("https://javadoc.io/doc/jakarta.servlet/jakarta.servlet-api/latest/element-list");
			});
			externalDocumentationLinks.register("rsocket-core", spec ->
					spec.url("https://javadoc.io/static/io.rsocket/rsocket-core/1.1.1/"));
		});
	}

}
