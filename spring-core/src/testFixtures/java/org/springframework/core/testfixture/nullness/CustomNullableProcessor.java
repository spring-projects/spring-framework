/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.nullness;

public interface CustomNullableProcessor {

	@org.springframework.core.testfixture.nullness.custom.Nullable
	String process(@org.springframework.core.testfixture.nullness.custom.Nullable String nullable);
}
