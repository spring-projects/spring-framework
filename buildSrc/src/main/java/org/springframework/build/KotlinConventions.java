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

package org.springframework.build;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

/**
 * @author Brian Clozel
 */
public class KotlinConventions {

	void apply(Project project) {
		project.getPlugins().withId("org.jetbrains.kotlin.jvm",
				(plugin) -> project.getTasks().withType(KotlinCompile.class, this::configure));
	}

	private void configure(KotlinCompile compile) {
		KotlinJvmOptions kotlinOptions = compile.getKotlinOptions();
		kotlinOptions.setApiVersion("1.7");
		kotlinOptions.setLanguageVersion("1.7");
		kotlinOptions.setJvmTarget("17");
		kotlinOptions.setJavaParameters(true);
		kotlinOptions.setAllWarningsAsErrors(true);
		List<String> freeCompilerArgs = new ArrayList<>(compile.getKotlinOptions().getFreeCompilerArgs());
		freeCompilerArgs.addAll(List.of("-Xsuppress-version-warnings", "-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn"));
		compile.getKotlinOptions().setFreeCompilerArgs(freeCompilerArgs);
	}

}
