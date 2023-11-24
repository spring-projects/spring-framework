/*
 * Copyright 2002-2023 the original author or authors.
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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin;

/**
 * Plugin to apply conventions to projects that are part of Spring Framework's build.
 * Conventions are applied in response to various plugins being applied.
 *
 * <p>When the {@link JavaBasePlugin} is applied, the conventions in {@link CheckstyleConventions},
 * {@link TestConventions} and {@link JavaConventions} are applied.
 * When the {@link KotlinBasePlugin} is applied, the conventions in {@link KotlinConventions}
 * are applied.
 *
 * @author Brian Clozel
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		new CheckstyleConventions().apply(project);
		new JavaConventions().apply(project);
		new KotlinConventions().apply(project);
		new TestConventions().apply(project);
	}

}
