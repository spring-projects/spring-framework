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

package org.springframework.build;

import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import io.spring.nohttp.gradle.NoHttpExtension;
import io.spring.nohttp.gradle.NoHttpPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.CheckstyleExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * {@link Plugin} that applies conventions for checkstyle.
 *
 * @author Brian Clozel
 */
public class SpotlessConventions {

	/**
	 * Applies the Spring Java Format and Spotless plugins with the project conventions.
	 * @param project the current project
	 */
	public void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (_) -> {
			project.getPlugins().apply(com.diffplug.gradle.spotless.SpotlessPlugin.class);
			project.getTasks().withType(Spotless.class).forEach(spotless -> spotless.getMaxHeapSize().set("1g"));
		});
	}

}
