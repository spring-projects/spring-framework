/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
