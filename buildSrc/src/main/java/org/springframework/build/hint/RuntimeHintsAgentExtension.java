/*
 * Copyright 2002-2022 the original author or authors.
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

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;

/**
 * Entry point to the DSL extension for the {@link RuntimeHintsAgentPlugin} Gradle plugin.
 * @author Brian Clozel
 */
public class RuntimeHintsAgentExtension {

	private final SetProperty<String> includedPackages;

	private final SetProperty<String> excludedPackages;

	public RuntimeHintsAgentExtension(ObjectFactory objectFactory) {
		this.includedPackages = objectFactory.setProperty(String.class).convention(Collections.singleton("org.springframework"));
		this.excludedPackages = objectFactory.setProperty(String.class).convention(Collections.emptySet());
	}

	public SetProperty<String> getIncludedPackages() {
		return this.includedPackages;
	}

	public SetProperty<String> getExcludedPackages() {
		return this.excludedPackages;
	}

	String asJavaAgentArgument() {
		StringBuilder builder = new StringBuilder();
		this.includedPackages.get().forEach(packageName -> builder.append('+').append(packageName).append(','));
		this.excludedPackages.get().forEach(packageName -> builder.append('-').append(packageName).append(','));
		return builder.toString();
	}
}
