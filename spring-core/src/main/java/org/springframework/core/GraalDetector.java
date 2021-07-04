/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

/**
 * A common delegate for detecting a GraalVM native image environment.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.1
 */
abstract class GraalDetector {

	// See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
	private static final boolean imageCode = (System.getProperty("org.graalvm.nativeimage.imagecode") != null);


	/**
	 * Return whether this runtime environment lives within a native image.
	 */
	public static boolean inImageCode() {
		return imageCode;
	}

}
