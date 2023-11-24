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
