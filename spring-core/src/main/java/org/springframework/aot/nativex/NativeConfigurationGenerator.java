/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.nativex;

import org.springframework.aot.hint.RuntimeHints;

/**
 * Generate GraalVM native configuration.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
public interface NativeConfigurationGenerator {

	/**
	 * Generate the GraalVM native configuration from the provided hints.
	 * @param hints the hints to serialize
	 */
	void generate(RuntimeHints hints);

}
