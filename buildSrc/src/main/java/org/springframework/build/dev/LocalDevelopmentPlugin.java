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

package org.springframework.build.dev;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;

/**
 * {@link Plugin} that skips documentation tasks when the {@code "-PskipDocs"} property is defined.
 *
 * @author Brian Clozel
 */
public class LocalDevelopmentPlugin implements Plugin<Project> {

	private static final String SKIP_DOCS_PROPERTY = "skipDocs";

	@Override
	public void apply(Project target) {
		if (target.hasProperty(SKIP_DOCS_PROPERTY)) {
			skipDocumentationTasks(target);
			target.subprojects(this::skipDocumentationTasks);
		}
	}

	private void skipDocumentationTasks(Project project) {
		project.afterEvaluate(p -> {
			p.getTasks().matching(task -> {
						return JavaBasePlugin.DOCUMENTATION_GROUP.equals(task.getGroup())
								|| "distribution".equals(task.getGroup());
					})
					.forEach(task -> task.setEnabled(false));
		});
	}
}
