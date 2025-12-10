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

package org.springframework.build.multirelease;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.jvm.toolchain.JavaToolchainService;

import java.util.List;

import javax.inject.Inject;

@CacheableTask
public abstract class MultiReleaseJarValidateTask extends JavaExec {


	public MultiReleaseJarValidateTask() {
		getMainModule().set("jdk.jartool");
		getArgumentProviders().add(() -> List.of("--validate", "--file", getJar().get().getAsFile().getAbsolutePath()));
	}

	@Inject
	protected abstract JavaToolchainService getJavaToolchainService();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getJar();

}
