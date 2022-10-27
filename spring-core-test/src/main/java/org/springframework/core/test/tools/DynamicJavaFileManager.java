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

package org.springframework.core.test.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.springframework.util.ClassUtils;

/**
 * {@link JavaFileManager} to create in-memory {@link DynamicClassFileObject
 * ClassFileObjects} when compiling.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 6.0
 */
class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

	private final ClassLoader classLoader;

	private final ClassFiles classFiles;

	private final ResourceFiles resourceFiles;

	private final Map<String, DynamicClassFileObject> dynamicClassFiles = Collections.synchronizedMap(new LinkedHashMap<>());

	private final Map<String, DynamicResourceFileObject> dynamicResourceFiles= Collections.synchronizedMap(new LinkedHashMap<>());


	DynamicJavaFileManager(JavaFileManager fileManager, ClassLoader classLoader,
			ClassFiles classFiles, ResourceFiles resourceFiles) {

		super(fileManager);
		this.classFiles = classFiles;
		this.resourceFiles = resourceFiles;
		this.classLoader = classLoader;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return this.classLoader;
	}

	@Override
	public FileObject getFileForOutput(Location location, String packageName,
			String relativeName, FileObject sibling) {
		ResourceFile resourceFile = this.resourceFiles.get(relativeName);
		if (resourceFile != null) {
			return new DynamicResourceFileObject(relativeName, resourceFile.getContent());
		}
		return this.dynamicResourceFiles.computeIfAbsent(relativeName, DynamicResourceFileObject::new);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className,
			JavaFileObject.Kind kind, FileObject sibling) throws IOException {
		if (kind == JavaFileObject.Kind.CLASS) {
			ClassFile classFile = this.classFiles.get(className);
			if (classFile != null) {
				return new DynamicClassFileObject(className, classFile.getContent());
			}
			return this.dynamicClassFiles.computeIfAbsent(className, DynamicClassFileObject::new);
		}
		return super.getJavaFileForOutput(location, className, kind, sibling);
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName,
			Set<Kind> kinds, boolean recurse) throws IOException {
		List<JavaFileObject> result = new ArrayList<>();
		if (kinds.contains(Kind.CLASS)) {
			for (ClassFile candidate : this.classFiles) {
				String existingPackageName = ClassUtils.getPackageName(candidate.getName());
				if (existingPackageName.equals(packageName) || (recurse && existingPackageName.startsWith(packageName + "."))) {
					result.add(new DynamicClassFileObject(candidate.getName(), candidate.getContent()));
				}
			}
			for (DynamicClassFileObject candidate : this.dynamicClassFiles.values()) {
				String existingPackageName = ClassUtils.getPackageName(candidate.getClassName());
				if (existingPackageName.equals(packageName) || (recurse && existingPackageName.startsWith(packageName + "."))) {
					result.add(candidate);
				}
			}
		}
		super.list(location, packageName, kinds, recurse).forEach(result::add);
		return result;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof DynamicClassFileObject dynamicClassFileObject) {
			return dynamicClassFileObject.getClassName();
		}
		return super.inferBinaryName(location, file);
	}

	Map<String, DynamicClassFileObject> getDynamicClassFiles() {
		return this.dynamicClassFiles;
	}

	Map<String, DynamicResourceFileObject> getDynamicResourceFiles() {
		return Collections.unmodifiableMap(this.dynamicResourceFiles);
	}

}
