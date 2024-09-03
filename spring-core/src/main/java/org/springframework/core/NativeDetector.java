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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * A common delegate for detecting a GraalVM native image environment.
 *
 * @author Sebastien Deleuze
 * @since 5.3.4
 */
public abstract class NativeDetector {

	// See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
	@Nullable
	private static final String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");

	private static final boolean inNativeImage = (imageCode != null);


	/**
	 * Returns {@code true} if running in a native image context (for example
	 * {@code buildtime}, {@code runtime}, or {@code agent}) expressed by setting the
	 * {@code org.graalvm.nativeimage.imagecode} system property to any value.
	 */
	public static boolean inNativeImage() {
		return inNativeImage;
	}

	/**
	 * Returns {@code true} if running in any of the specified native image context(s).
	 * @param contexts the native image context(s)
	 * @since 6.0.10
	 */
	public static boolean inNativeImage(Context... contexts) {
		for (Context context: contexts) {
			if (context.key.equals(imageCode)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Native image context as defined in GraalVM's
	 * <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">ImageInfo</a>.
	 * @since 6.0.10
	 */
	public enum Context {

		/**
		 * The code is executing in the context of image building.
		 */
		BUILD("buildtime"),

		/**
		 * The code is executing at image runtime.
		 */
		RUN("runtime");

		private final String key;

		Context(final String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return this.key;
		}
	}

}
