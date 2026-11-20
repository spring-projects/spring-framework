/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.nullness;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public interface JSpecifyProcessor {

	String process(String unspecified, @Nullable String nullable, @NonNull String nonNull);

	@Nullable String nullableProcess();

	@NonNull String nonNullProcess();

	@NullMarked
	String markedProcess(String unspecified, @Nullable String nullable, @NonNull String nonNull);

	@NullMarked
	@Nullable String nullableMarkedProcess();

	@NullMarked
	@NonNull String nonNullMarkedProcess();

	@NullMarked
	void voidProcess();

	@NullMarked
	void voidClassProcess();
}
