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

package org.springframework.test.context.aot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract base class for AOT tests.
 *
 * @author Sam Brannen
 * @since 6.0
 */
abstract class AbstractAotTests {

	Stream<Class<?>> scan() {
		return new TestClassScanner(classpathRoots()).scan();
	}

	Stream<Class<?>> scan(String... packageNames) {
		return new TestClassScanner(classpathRoots()).scan(packageNames);
	}

	Set<Path> classpathRoots() {
		try {
			return Set.of(classpathRoot());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	Path classpathRoot() {
		try {
			return Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	Path classpathRoot(Class<?> clazz) {
		try {
			return Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
