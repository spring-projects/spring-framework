/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.instrument.classloading.websphere;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.CodeSource;

import org.springframework.util.FileCopyUtils;

/**
 * Adapter that implements WebSphere 7.0 ClassPreProcessPlugin interface,
 * delegating to a standard JDK {@link ClassFileTransformer} underneath.
 *
 * <p>To avoid compile time checks again the vendor API, a dynamic proxy is
 * being used.
 *
 * @author Costin Leau
 * @since 3.1
 */
class WebSphereClassPreDefinePlugin implements InvocationHandler {

	private final ClassFileTransformer transformer;


	/**
	 * Create a new {@link WebSphereClassPreDefinePlugin}.
	 * @param transformer the {@link ClassFileTransformer} to be adapted
	 * (must not be {@code null})
	 */
	public WebSphereClassPreDefinePlugin(ClassFileTransformer transformer) {
		this.transformer = transformer;
		ClassLoader classLoader = transformer.getClass().getClassLoader();

		// first force the full class loading of the weaver by invoking transformation on a dummy class
		try {
			String dummyClass = Dummy.class.getName().replace('.', '/');
			byte[] bytes = FileCopyUtils.copyToByteArray(classLoader.getResourceAsStream(dummyClass + ".class"));
			transformer.transform(classLoader, dummyClass, null, null, bytes);
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot load transformer", ex);
		}
	}


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
		else if ("transformClass".equals(name)) {
			return transform((String) args[0], (byte[]) args[1], (CodeSource) args[2], (ClassLoader) args[3]);
		}
		else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	protected byte[] transform(String className, byte[] classfileBuffer, CodeSource codeSource, ClassLoader classLoader)
			throws Exception {

		// NB: WebSphere passes className as "." without class while the transformer expects a VM, "/" format
		byte[] result = transformer.transform(classLoader, className.replace('.', '/'), null, null, classfileBuffer);
		return (result != null ? result : classfileBuffer);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getName());
		builder.append(" for transformer: ");
		builder.append(this.transformer);
		return builder.toString();
	}


	private static class Dummy {
	}

}
