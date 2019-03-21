/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * Adapter that implements JBoss Translator interface, delegating to a
 * standard JDK {@link ClassFileTransformer} underneath.
 *
 * <p>To avoid compile time checks again the vendor API, a dynamic proxy is
 * being used.
 *
 * @author Costin Leau
 * @since 3.1
 */
class JBossMCTranslatorAdapter implements InvocationHandler {

	private final ClassFileTransformer transformer;


	public JBossMCTranslatorAdapter(ClassFileTransformer transformer) {
		this.transformer = transformer;
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String name = method.getName();
		if ("equals".equals(name)) {
			return proxy == args[0];
		}
		else if ("hashCode".equals(name)) {
			return hashCode();
		}
		else if ("toString".equals(name)) {
			return toString();
		}
		else if ("transform".equals(name)) {
			return transform((ClassLoader) args[0], (String) args[1], (Class<?>) args[2],
					(ProtectionDomain) args[3], (byte[]) args[4]);
		}
		else if ("unregisterClassLoader".equals(name)) {
			unregisterClassLoader((ClassLoader) args[0]);
			return null;
		}
		else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws Exception {

		return this.transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}

	public void unregisterClassLoader(ClassLoader loader) {
	}


	@Override
	public String toString() {
		return getClass().getName() + " for transformer: " + this.transformer;
	}

}
