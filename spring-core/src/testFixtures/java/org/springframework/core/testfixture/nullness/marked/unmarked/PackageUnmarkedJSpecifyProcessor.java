/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.nullness.marked.unmarked;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PackageUnmarkedJSpecifyProcessor {

	String process(String unspecified, @Nullable String nullable, @NonNull String nonNull);

	@Nullable String nullableProcess();

	@NonNull String nonNullProcess();
}
