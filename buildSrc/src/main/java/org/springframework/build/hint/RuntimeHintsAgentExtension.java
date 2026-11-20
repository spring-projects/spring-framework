/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.build.hint;

import org.gradle.api.provider.SetProperty;

/**
 * Entry point to the DSL extension for the {@link RuntimeHintsAgentPlugin} Gradle plugin.
 * @author Brian Clozel
 */
public interface RuntimeHintsAgentExtension {

	SetProperty<String> getIncludedPackages();

	SetProperty<String> getExcludedPackages();
}
