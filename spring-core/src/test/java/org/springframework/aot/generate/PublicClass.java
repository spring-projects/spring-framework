/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

@SuppressWarnings("unused")
public class PublicClass {

	private String privateField;

	String protectedField;

	public PackagePrivateClass[] packagePrivateClasses;

	public PackagePrivateClass protectedClassField;

	String getProtectedMethod() {
		return this.protectedField;
	}

}
