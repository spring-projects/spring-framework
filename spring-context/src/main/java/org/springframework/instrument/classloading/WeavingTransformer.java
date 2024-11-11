/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * ClassFileTransformer-based weaver, allowing for a list of transformers to be
 * applied on a class byte array. Normally used inside class loaders.
 *
 * <p>Note: This class is deliberately implemented for minimal external dependencies,
 * since it is included in weaver jars (to be deployed into application servers).
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 */
public class WeavingTransformer {

	@Nullable
	private final ClassLoader classLoader;

	private final List<ClassFileTransformer> transformers = new ArrayList<>();


	/**
	 * Create a new WeavingTransformer for the given class loader.
	 * @param classLoader the ClassLoader to build a transformer for
	 */
	public WeavingTransformer(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Add a class file transformer to be applied by this weaver.
	 * @param transformer the class file transformer to register
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		this.transformers.add(transformer);
	}


	/**
	 * Apply transformation on a given class byte definition.
	 * The method will always return a non-null byte array (if no transformation has taken place
	 * the array content will be identical to the original one).
	 * @param className the full qualified name of the class in dot format (i.e. some.package.SomeClass)
	 * @param bytes class byte definition
	 * @return (possibly transformed) class byte definition
	 */
	public byte[] transformIfNecessary(String className, byte[] bytes) {
		String internalName = StringUtils.replace(className, ".", "/");
		return transformIfNecessary(className, internalName, bytes, null);
	}

	/**
	 * Apply transformation on a given class byte definition.
	 * The method will always return a non-null byte array (if no transformation has taken place
	 * the array content will be identical to the original one).
	 * @param className the full qualified name of the class in dot format (i.e. some.package.SomeClass)
	 * @param internalName class name internal name in / format (i.e. some/package/SomeClass)
	 * @param bytes class byte definition
	 * @param pd protection domain to be used (can be {@code null})
	 * @return (possibly transformed) class byte definition
	 */
	public byte[] transformIfNecessary(String className, String internalName, byte[] bytes, @Nullable ProtectionDomain pd) {
		byte[] result = bytes;
		for (ClassFileTransformer cft : this.transformers) {
			try {
				byte[] transformed = cft.transform(this.classLoader, internalName, null, pd, result);
				if (transformed != null) {
					result = transformed;
				}
			}
			catch (IllegalClassFormatException ex) {
				throw new IllegalStateException("Class file transformation failed", ex);
			}
		}
		return result;
	}

}
