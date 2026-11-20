/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.nullness;

public class NullnessFields {

	public String unannotatedField = "";

	public @org.jspecify.annotations.Nullable String jspecifyNullableField;

	public @org.jspecify.annotations.NonNull String jspecifyNonNullField = "";

	@org.springframework.core.testfixture.nullness.custom.Nullable
	public String customNullableField;

	public int primitiveField = 0;
}
