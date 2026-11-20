/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
