/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.instrument.classloading.weblogic;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Hashtable;

/**
 * Adapter that implements WebLogic ClassPreProcessor interface, delegating to a
 * standard JDK {@link ClassFileTransformer} underneath.
 *
 * <p>To avoid compile time checks again the vendor API, a dynamic proxy is
 * being used.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.5
 */
class WebLogicClassPreProcessorAdapter implements InvocationHandler {

	private final ClassFileTransformer transformer;

	private final ClassLoader loader;


	/**
	 * Creates a new {@link WebLogicClassPreProcessorAdapter}.
	 * @param transformer the {@link ClassFileTransformer} to be adapted
	 * (must not be {@code null})
	 */
	public WebLogicClassPreProcessorAdapter(ClassFileTransformer transformer, ClassLoader loader) {
		this.transformer = transformer;
		this.loader = loader;
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String name = method.getName();
		if ("equals".equals(name)) {
			return (proxy == args[0]);
		}
		else if ("hashCode".equals(name)) {
			return hashCode();
		}
		else if ("toString".equals(name)) {
			return toString();
		}
		else if ("initialize".equals(name)) {
			initialize((Hashtable<?, ?>) args[0]);
			return null;
		}
		else if ("preProcess".equals(name)) {
			return preProcess((String) args[0], (byte[]) args[1]);
		}
		else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	public void initialize(Hashtable<?, ?> params) {
	}

	public byte[] preProcess(String className, byte[] classBytes) {
		try {
			byte[] result = this.transformer.transform(this.loader, className, null, null, classBytes);
			return (result != null ? result : classBytes);
		}
		catch (IllegalClassFormatException ex) {
			throw new IllegalStateException("Cannot transform due to illegal class format", ex);
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + " for transformer: " + this.transformer;
	}

}
