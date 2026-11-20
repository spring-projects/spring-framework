/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.build.hint;

import java.util.Collections;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Argument provider for registering the runtime hints agent with a Java process.
 */
public interface RuntimeHintsAgentArgumentProvider extends CommandLineArgumentProvider {

	@Classpath
	ConfigurableFileCollection getAgentJar();

    @Input
    SetProperty<String> getIncludedPackages();

    @Input
    SetProperty<String> getExcludedPackages();

    @Override
    default Iterable<String> asArguments() {
        StringBuilder packages = new StringBuilder();
        getIncludedPackages().get().forEach(packageName -> packages.append('+').append(packageName).append(','));
        getExcludedPackages().get().forEach(packageName -> packages.append('-').append(packageName).append(','));
        return Collections.singleton("-javaagent:" + getAgentJar().getSingleFile() + "=" + packages);
    }
}
