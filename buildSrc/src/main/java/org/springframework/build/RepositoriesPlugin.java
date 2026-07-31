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
		String releaseRepositoryUrl = getEnv("COMMERCIAL_RELEASE_REPO_URL");
		if (releaseRepositoryUrl != null) {
			project.getRepositories().maven((repository) -> {
				repository.setName("spring-commercial-release");
				repository.setUrl(releaseRepositoryUrl);
				repository.credentials((creds) -> {
					creds.setUsername(System.getenv("COMMERCIAL_REPO_USERNAME"));
					creds.setPassword(System.getenv("COMMERCIAL_REPO_PASSWORD"));
				});
			});
		}
		String snapshotRepositoryUrl = getEnv("COMMERCIAL_SNAPSHOT_REPO_URL");
		if (snapshotRepositoryUrl != null && project.getVersion().toString().endsWith("-SNAPSHOT")) {
			project.getRepositories().maven((repository) -> {
				repository.setName("spring-commercial-snapshot");
				repository.setUrl(snapshotRepositoryUrl);
				repository.credentials((creds) -> {
					creds.setUsername(System.getenv("COMMERCIAL_REPO_USERNAME"));
					creds.setPassword(System.getenv("COMMERCIAL_REPO_PASSWORD"));
				});
			});
		}
	}

	private void configureReleaseTrainRepository(Project project) {
		String releaseTrainRepositoryUrl = getEnv("RELEASE_TRAIN_MAVEN_REPOSITORY_URL");
		if (releaseTrainRepositoryUrl != null) {
			project.getRepositories().maven(repository -> {
				repository.setName("spring-release-train");
				repository.setUrl(releaseTrainRepositoryUrl);
				repository.credentials((creds) -> {
					creds.setUsername(System.getenv("RELEASE_TRAIN_MAVEN_REPOSITORY_USERNAME"));
					creds.setPassword(System.getenv("RELEASE_TRAIN_MAVEN_REPOSITORY_PASSWORD"));
				});
			});
		}
	}

	/**
	 * Returns the environment variable's value, or {@code null} if it is unset or blank.
	 */
	private static String getEnv(String name) {
		String value = System.getenv(name);
		return (value != null && !value.isBlank()) ? value : null;
	}
}
