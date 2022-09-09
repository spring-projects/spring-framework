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

package org.springframework.aot.test.generate.compile;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * {@link JavaFileManager} to create in-memory {@link DynamicClassFileObject
 * ClassFileObjects} when compiling.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {


	private final ClassLoader classLoader;

	private final Map<String, DynamicClassFileObject> classFiles = Collections.synchronizedMap(
			new LinkedHashMap<>());


	DynamicJavaFileManager(JavaFileManager fileManager, ClassLoader classLoader) {
		super(fileManager);
		this.classLoader = classLoader;
	}


	@Override
	public ClassLoader getClassLoader(Location location) {
		return this.classLoader;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className,
			JavaFileObject.Kind kind, FileObject sibling) throws IOException {
		if (kind == JavaFileObject.Kind.CLASS) {
			return this.classFiles.computeIfAbsent(className,
					DynamicClassFileObject::new);
		}
		return super.getJavaFileForOutput(location, className, kind, sibling);
	}

	Map<String, DynamicClassFileObject> getClassFiles() {
		return Collections.unmodifiableMap(this.classFiles);
	}

}
