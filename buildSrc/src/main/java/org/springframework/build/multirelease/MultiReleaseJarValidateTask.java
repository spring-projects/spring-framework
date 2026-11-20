/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
