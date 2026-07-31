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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Plugin that configures the OSS, commercial and release train repositories in the build.
 *
 * @author Brian Clozel
 */
public class RepositoriesPlugin implements Plugin <Project> {

	@Override
	public void apply(Project project) {
		configureOssRepositories(project);
		configureCommercialRepositories(project);
		configureReleaseTrainRepository(project);
	}

	private void configureOssRepositories(Project project) {
		project.getRepositories().mavenCentral();
		if (project.getVersion().toString().contains("-")) {
			project.getRepositories().maven(repository -> {
				repository.setName("spring-oss-milestone");
				repository.setUrl("https://repo.spring.io/milestone/");
			});
		}
		if (project.getVersion().toString().endsWith("-SNAPSHOT")) {
			project.getRepositories().maven(repository -> {
				repository.setName("spring-oss-snapshot");
				repository.setUrl("https://repo.spring.io/snapshot/");
			});
		}
	}

	private void configureCommercialRepositories(Project project) {
		if (System.getenv().containsKey("COMMERCIAL_RELEASE_REPO_URL")) {
			project.getRepositories().maven((repository) -> {
				repository.setName("spring-commercial-release");
				repository.setUrl(System.getenv("COMMERCIAL_RELEASE_REPO_URL"));
				repository.credentials((creds) -> {
					creds.setUsername(System.getenv("COMMERCIAL_REPO_USERNAME"));
					creds.setPassword(System.getenv("COMMERCIAL_REPO_PASSWORD"));
				});
			});
		}
		if (System.getenv().containsKey("COMMERCIAL_SNAPSHOT_REPO_URL") && project.getVersion().toString().endsWith("-SNAPSHOT")) {
			project.getRepositories().maven((repository) -> {
				repository.setName("spring-commercial-snapshot");
				repository.setUrl(System.getenv("COMMERCIAL_SNAPSHOT_REPO_URL"));
				repository.credentials((creds) -> {
					creds.setUsername(System.getenv("COMMERCIAL_REPO_USERNAME"));
					creds.setPassword(System.getenv("COMMERCIAL_REPO_PASSWORD"));
				});
			});
		}
	}

	private void configureReleaseTrainRepository(Project project) {
		if (System.getenv().containsKey("RELEASE_TRAIN_MAVEN_REPOSITORY_URL")) {
			project.getRepositories().maven(repository -> {
				repository.setName("spring-release-train");
				repository.setUrl(System.getenv("RELEASE_TRAIN_MAVEN_REPOSITORY_URL"));
				repository.credentials((creds) -> {
					creds.setUsername(System.getenv("RELEASE_TRAIN_MAVEN_REPOSITORY_USERNAME"));
					creds.setPassword(System.getenv("RELEASE_TRAIN_MAVEN_REPOSITORY_PASSWORD"));
				});
			});
		}
	}
}
