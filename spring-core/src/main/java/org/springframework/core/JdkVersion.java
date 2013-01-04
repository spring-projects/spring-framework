/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

/**
 * Internal helper class used to find the Java/JVM version
 * that Spring is operating on, to allow for automatically
 * adapting to the present platform's capabilities.
 *
 * <p>Note that Spring requires JVM 1.5 or higher, as of Spring 3.0.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public abstract class JdkVersion {

	/**
	 * Constant identifying the 1.3.x JVM (JDK 1.3).
	 */
	public static final int JAVA_13 = 0;

	/**
	 * Constant identifying the 1.4.x JVM (J2SE 1.4).
	 */
	public static final int JAVA_14 = 1;

	/**
	 * Constant identifying the 1.5 JVM (Java 5).
	 */
	public static final int JAVA_15 = 2;

	/**
	 * Constant identifying the 1.6 JVM (Java 6).
	 */
	public static final int JAVA_16 = 3;

	/**
	 * Constant identifying the 1.7 JVM (Java 7).
	 */
	public static final int JAVA_17 = 4;


	private static final String javaVersion;

	private static final int majorJavaVersion;

	static {
		javaVersion = System.getProperty("java.version");
		// version String should look like "1.4.2_10"
		if (javaVersion.contains("1.7.")) {
			majorJavaVersion = JAVA_17;
		}
		else if (javaVersion.contains("1.6.")) {
			majorJavaVersion = JAVA_16;
		}
		else {
			// else leave 1.5 as default (it's either 1.5 or unknown)
			majorJavaVersion = JAVA_15;
		}
	}


	/**
	 * Return the full Java version string, as returned by
	 * {@code System.getProperty("java.version")}.
	 * @return the full Java version string
	 * @see System#getProperty(String)
	 */
	public static String getJavaVersion() {
		return javaVersion;
	}

	/**
	 * Get the major version code. This means we can do things like
	 * {@code if (getMajorJavaVersion() < JAVA_14)}.
	 * @return a code comparable to the JAVA_XX codes in this class
	 * @see #JAVA_13
	 * @see #JAVA_14
	 * @see #JAVA_15
	 * @see #JAVA_16
	 * @see #JAVA_17
	 */
	public static int getMajorJavaVersion() {
		return majorJavaVersion;
	}


	/**
	 * Convenience method to determine if the current JVM is at least Java 1.4.
	 * @return {@code true} if the current JVM is at least Java 1.4
	 * @deprecated as of Spring 3.0 which requires Java 1.5+
	 * @see #getMajorJavaVersion()
	 * @see #JAVA_14
	 * @see #JAVA_15
	 * @see #JAVA_16
	 * @see #JAVA_17
	 */
	@Deprecated
	public static boolean isAtLeastJava14() {
		return true;
	}

	/**
	 * Convenience method to determine if the current JVM is at least
	 * Java 1.5 (Java 5).
	 * @return {@code true} if the current JVM is at least Java 1.5
	 * @deprecated as of Spring 3.0 which requires Java 1.5+
	 * @see #getMajorJavaVersion()
	 * @see #JAVA_15
	 * @see #JAVA_16
	 * @see #JAVA_17
	 */
	@Deprecated
	public static boolean isAtLeastJava15() {
		return true;
	}

	/**
	 * Convenience method to determine if the current JVM is at least
	 * Java 1.6 (Java 6).
	 * @return {@code true} if the current JVM is at least Java 1.6
	 * @deprecated as of Spring 3.0, in favor of reflective checks for
	 * the specific Java 1.6 classes of interest
	 * @see #getMajorJavaVersion()
	 * @see #JAVA_16
	 * @see #JAVA_17
	 */
	@Deprecated
	public static boolean isAtLeastJava16() {
		return (majorJavaVersion >= JAVA_16);
	}

}
