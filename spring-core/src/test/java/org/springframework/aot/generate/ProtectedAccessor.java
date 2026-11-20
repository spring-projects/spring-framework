/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

@SuppressWarnings("unused")
public class ProtectedAccessor {

	ProtectedAccessor() {
	}

	public String methodWithProtectedParameter(PackagePrivateClass type) {
		return "test";
	}

	public PackagePrivateClass methodWithProtectedReturnType() {
		return new PackagePrivateClass();
	}
}
