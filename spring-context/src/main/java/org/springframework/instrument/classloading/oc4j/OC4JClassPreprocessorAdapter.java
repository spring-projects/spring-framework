/*
 * Copyright 2006-2009 the original author or authors.
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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * Adapter that implements OC4J ClassPreProcessor interface, delegating to a
 * standard JDK {@link ClassFileTransformer} underneath.
 *
 * <p>To avoid compile time checks again the vendor API, a dynamic proxy is
 * being used.
 *
 * @author Costin Leau
 */
class OC4JClassPreprocessorAdapter implements InvocationHandler {

	private final ClassFileTransformer transformer;

	/**
	 * Creates a new {@link OC4JClassPreprocessorAdapter}.
	 * @param transformer the {@link ClassFileTransformer} to be adapted (must
	 * not be {@code null})
	 */
	public OC4JClassPreprocessorAdapter(ClassFileTransformer transformer) {
		this.transformer = transformer;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String name = method.getName();

		if ("equals".equals(name)) {
			return (Boolean.valueOf(proxy == args[0]));
		} else if ("hashCode".equals(name)) {
			return hashCode();
		} else if ("toString".equals(name)) {
			return toString();
		} else if ("initialize".equals(name)) {
			initialize(proxy, (ClassLoader) args[0]);
			return null;
		} else if ("processClass".equals(name)) {
			return processClass((String) args[0], (byte[]) args[1], (Integer) args[2], (Integer) args[3],
					(ProtectionDomain) args[4], (ClassLoader) args[5]);
		} else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	// maps to oracle.classloader.util.ClassPreprocessor#initialize
	// the proxy is passed since it implements the Oracle interface which
	// is asked as a return type
	public Object initialize(Object proxy, ClassLoader loader) {
		return proxy;
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
		} catch (IllegalClassFormatException ex) {
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