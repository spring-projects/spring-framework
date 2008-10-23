/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.instrument.classloading.oc4j;

import oracle.classloader.util.ClassPreprocessor;
import org.springframework.util.Assert;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * {@link ClassPreprocessor} adapter for OC4J, delegating to a standard
 * JDK {@link ClassFileTransformer} underneath.
 *
 * <p>Many thanks to <a href="mailto:mike.keith@oracle.com">Mike Keith</a>
 * for his assistance.
 *
 * @author Costin Leau
 * @since 2.0
 */
class OC4JClassPreprocessorAdapter implements ClassPreprocessor {

	private final ClassFileTransformer transformer;


	/**
	 * Creates a new instance of the {@link OC4JClassPreprocessorAdapter} class.
	 * @param transformer the {@link ClassFileTransformer} to be adapted (must not be <code>null</code>)
	 * @throws IllegalArgumentException if the supplied <code>transformer</code> is <code>null</code>
	 */
	public OC4JClassPreprocessorAdapter(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		this.transformer = transformer;
	}


	public ClassPreprocessor initialize(ClassLoader loader) {
		return this;
	}

	public byte[] processClass(String className, byte origClassBytes[], int offset, int length, ProtectionDomain pd,
							   ClassLoader loader) {
		try {
			byte[] tempArray = new byte[length];
			System.arraycopy(origClassBytes, offset, tempArray, 0, length);

			// NB: OC4J passes className as "." without class while the
			// transformer expects a VM, "/" format
			byte[] result = this.transformer.transform(loader, className.replace('.', '/'), null, pd, tempArray);
			return (result != null ? result : origClassBytes);
		}
		catch (IllegalClassFormatException ex) {
			throw new IllegalStateException("Cannot transform because of illegal class format", ex);
		}
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getName());
		builder.append(" for transformer: ");
		builder.append(this.transformer);
		return builder.toString();
	}

}
