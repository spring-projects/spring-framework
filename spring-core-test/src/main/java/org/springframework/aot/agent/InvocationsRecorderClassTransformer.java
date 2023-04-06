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

package org.springframework.aot.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.springframework.asm.ClassReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * ASM {@link ClassFileTransformer} that delegates bytecode transformations
 * to a {@link InvocationsRecorderClassVisitor class visitor} if and only
 * if the class is in the list of packages considered for instrumentation.
 *
 * @author Brian Clozel
 * @see InvocationsRecorderClassVisitor
 */
class InvocationsRecorderClassTransformer implements ClassFileTransformer {

	private static final String AGENT_PACKAGE = InvocationsRecorderClassTransformer.class.getPackageName().replace('.', '/');

	private static final String AOT_DYNAMIC_CLASSLOADER = "org/springframework/aot/test/generate/compile/DynamicClassLoader";

	private final String[] instrumentedPackages;

	private final String[] ignoredPackages;

	public InvocationsRecorderClassTransformer(String[] instrumentedPackages, String[] ignoredPackages) {
		Assert.notNull(instrumentedPackages, "instrumentedPackages must not be null");
		Assert.notNull(ignoredPackages, "ignoredPackages must not be null");
		this.instrumentedPackages = rewriteToAsmFormat(instrumentedPackages);
		this.ignoredPackages = rewriteToAsmFormat(ignoredPackages);
	}

	private String[] rewriteToAsmFormat(String[] packages) {
		return Arrays.stream(packages).map(pack -> pack.replace('.', '/'))
				.toArray(String[]::new);
	}

	@Override
	public byte[] transform(@Nullable ClassLoader classLoader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		if (isTransformationCandidate(classLoader, className)) {
			return attemptClassTransformation(classfileBuffer);
		}
		return classfileBuffer;
	}

	private boolean isTransformationCandidate(@Nullable ClassLoader classLoader, String className) {
		// Ignore system classes
		if (classLoader == null) {
			return false;
		}
		// Ignore agent classes and spring-core-test DynamicClassLoader
		else if (className.startsWith(AGENT_PACKAGE) || className.equals(AOT_DYNAMIC_CLASSLOADER)) {
			return false;
		}
		// Do not instrument CGlib classes
		else if (className.contains("$$")) {
			return false;
		}
		// Only some packages are instrumented
		else {
			for (String ignoredPackage : this.ignoredPackages) {
				if (className.startsWith(ignoredPackage)) {
					return false;
				}
			}
			for (String instrumentedPackage : this.instrumentedPackages) {
				if (className.startsWith(instrumentedPackage)) {
					return true;
				}
			}
		}
		return false;
	}

	private byte[] attemptClassTransformation(byte[] classfileBuffer) {
		ClassReader fileReader = new ClassReader(classfileBuffer);
		InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
		try {
			fileReader.accept(classVisitor, 0);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return classfileBuffer;
		}
		if (classVisitor.isTransformed()) {
			return classVisitor.getTransformedClassBuffer();
		}
		return classfileBuffer;
	}
}
