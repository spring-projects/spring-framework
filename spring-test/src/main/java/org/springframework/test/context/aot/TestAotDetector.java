/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot;

import org.springframework.aot.AotDetector;
import org.springframework.core.SpringProperties;
import org.springframework.util.StringUtils;

/**
 * TestContext framework specific utility for determining if AOT-processed
 * optimizations must be used rather than the regular runtime.
 *
 * <p>Strictly for internal use within the framework.
 *
 * @author Sam Brannen
 * @since 6.0.9
 */
public abstract class TestAotDetector {

	/**
	 * Determine whether AOT optimizations must be considered at runtime.
	 * <p>This can be triggered using the {@value AotDetector#AOT_ENABLED}
	 * Spring property or via GraalVM's {@code "org.graalvm.nativeimage.imagecode"}
	 * JVM system property (if set to any non-empty value other than {@code agent}).
	 * @return {@code true} if AOT optimizations must be considered
	 * @see <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">GraalVM's ImageInfo.java</a>
	 * @see AotDetector#useGeneratedArtifacts()
	 */
	public static boolean useGeneratedArtifacts() {
		return (SpringProperties.getFlag(AotDetector.AOT_ENABLED) || inNativeImage());
	}

	/**
	 * Determine if we are currently running within a GraalVM native image from
	 * the perspective of the TestContext framework.
	 * @return {@code true} if the {@code org.graalvm.nativeimage.imagecode} JVM
	 * system property has been set to any value other than {@code agent}.
	 */
	private static boolean inNativeImage() {
		String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");
		return (StringUtils.hasText(imageCode) && !"agent".equalsIgnoreCase(imageCode.trim()));
	}

}
