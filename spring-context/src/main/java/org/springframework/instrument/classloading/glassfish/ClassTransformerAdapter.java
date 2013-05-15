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

package org.springframework.instrument.classloading.glassfish;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javax.persistence.spi.ClassTransformer;

/**
 * Adapter that implements the JPA ClassTransformer interface (as required by GlassFish V1 and V2)
 * based on a given JDK 1.5 ClassFileTransformer.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0.1
 */
class ClassTransformerAdapter implements ClassTransformer {

	private final ClassFileTransformer classFileTransformer;

	/**
	 * Build a new ClassTransformerAdapter for the given ClassFileTransformer.
	 * @param classFileTransformer the JDK 1.5 ClassFileTransformer to wrap
	 */
	public ClassTransformerAdapter(ClassFileTransformer classFileTransformer) {
		this.classFileTransformer = classFileTransformer;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		byte[] result = this.classFileTransformer.transform(loader, className, classBeingRedefined, protectionDomain,
				classfileBuffer);

		// If no transformation was done, return null.
		return (result == classfileBuffer ? null : result);
	}
}
