/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.tests;

/**
 * Enumeration of known JDK versions.
 *
 * @author Phillip Webb
 * @see #runningVersion()
 */
public enum JavaVersion {

	/**
	 * Java 1.6
	 */
	JAVA_16("1.6", 16),

	/**
	 * Java 1.7
	 */
	JAVA_17("1.7", 17),

	/**
	 * Java 1.8
	 */
	JAVA_18("1.8", 18),

	/**
	 * Java 1.9
	 */
	JAVA_19("1.9", 19);


	private static final JavaVersion runningVersion = findRunningVersion();

	private static JavaVersion findRunningVersion() {
		String version = System.getProperty("java.version");
		for (JavaVersion candidate : values()) {
			if (version.startsWith(candidate.version)) {
				return candidate;
			}
		}
		return JavaVersion.JAVA_16;
	}


	private String version;

	private int value;


	private JavaVersion(String version, int value) {
		this.version = version;
		this.value = value;
	}


	@Override
	public String toString() {
		return version;
	}

	/**
	 * Determines if the specified version is the same as or greater than this version.
	 * @param version the version to check
	 * @return {@code true} if the specified version is at least this version
	 */
	public boolean isAtLeast(JavaVersion version) {
		return (this.value >= version.value);
	}

	/**
	 * Returns the current running JDK version. If the current version cannot be
	 * determined {@link #JAVA_16} will be returned.
	 * @return the JDK version
	 */
	public static JavaVersion runningVersion() {
		return runningVersion;
	}

}
