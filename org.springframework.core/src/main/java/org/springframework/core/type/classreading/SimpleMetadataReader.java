/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.core.type.classreading;

import org.objectweb.asm.ClassReader;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * {@link MetadataReader} implementation based on an ASM
 * {@link org.objectweb.asm.ClassReader}.
 *
 * <p>Package-visible in order to allow for repackaging the ASM library
 * without effect on users of the <code>core.type</code> package.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
class SimpleMetadataReader implements MetadataReader {

	private final ClassReader classReader;

	private final ClassLoader classLoader;


	public SimpleMetadataReader(ClassReader classReader, ClassLoader classLoader) {
		this.classReader = classReader;
		this.classLoader = classLoader;
	}


	public ClassMetadata getClassMetadata() {
		ClassMetadataReadingVisitor visitor = new ClassMetadataReadingVisitor();
		this.classReader.accept(visitor, true);
		return visitor;
	}

	public AnnotationMetadata getAnnotationMetadata() {
		AnnotationMetadataReadingVisitor visitor = new AnnotationMetadataReadingVisitor(this.classLoader);
		this.classReader.accept(visitor, true);
		return visitor;
	}

}
