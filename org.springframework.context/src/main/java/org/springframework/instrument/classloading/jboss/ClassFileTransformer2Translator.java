/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.instrument.classloading.jboss;

import java.security.ProtectionDomain;
import java.lang.instrument.ClassFileTransformer;

import org.jboss.util.loading.Translator;

/**
 * ClassFileTransfomer to Translator bridge.
 *
 * @author Ales Justin
 */
public class ClassFileTransformer2Translator implements Translator {

	private ClassFileTransformer transformer;

	public ClassFileTransformer2Translator(ClassFileTransformer transformer) {
		if (transformer == null) {
			throw new IllegalArgumentException("Null transformer");
		}

		this.transformer = transformer;
	}

	public byte[] transform(ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws Exception {
		return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}

	public void unregisterClassLoader(ClassLoader loader) {
	}
}
