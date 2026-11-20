/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.nullness;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ClassMarkedJSpecifyProcessor {

	String process(String unspecified, @Nullable String nullable, @NonNull String nonNull);

	@Nullable String nullableProcess();

	@NonNull String nonNullProcess();

	@NullUnmarked
	String unmarkedProcess(String unspecified, @Nullable String nullable, @NonNull String nonNull);

}
