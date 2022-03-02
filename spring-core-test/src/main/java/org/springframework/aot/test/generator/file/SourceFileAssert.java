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

package org.springframework.aot.test.generator.file;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertion methods for {@code SourceFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class SourceFileAssert extends DynamicFileAssert<SourceFileAssert, SourceFile> {


	SourceFileAssert(SourceFile actual) {
		super(actual, SourceFileAssert.class);
	}


	public SourceFileAssert implementsInterface(@Nullable Class<?> type) {
		return implementsInterface((type != null ? type.getName() : null));
	}

	public SourceFileAssert implementsInterface(@Nullable String name) {
		JavaClass javaClass = getJavaClass();
		assertThat(javaClass.getImplements()).as("implements").map(
				JavaType::getFullyQualifiedName).contains(name);
		return this;
	}

	public MethodAssert hasMethodNamed(String name) {
		JavaClass javaClass = getJavaClass();
		JavaMethod method = null;
		for (JavaMethod candidate : javaClass.getMethods()) {
			if (candidate.getName().equals(name)) {
				if (method != null) {
					throw Failures.instance().failure(this.info,
							new BasicErrorMessageFactory(String.format(
									"%nExpecting actual:%n  %s%nto contain unique method:%n  %s%n",
									this.actual.getContent(), name)));
				}
				method = candidate;
			}
		}
		if (method == null) {
			throw Failures.instance().failure(this.info,
					new BasicErrorMessageFactory(String.format(
							"%nExpecting actual:%n  %s%nto contain method:%n  %s%n",
							this.actual.getContent(), name)));
		}
		return new MethodAssert(method);
	}

	public MethodAssert hasMethod(String name, Class<?>... parameters) {
		JavaClass javaClass = getJavaClass();
		JavaMethod method = null;
		for (JavaMethod candidate : javaClass.getMethods()) {
			if (candidate.getName().equals(name)
					&& hasParameters(candidate, parameters)) {
				if (method != null) {
					throw Failures.instance().failure(this.info,
							new BasicErrorMessageFactory(String.format(
									"%nExpecting actual:%n  %s%nto contain unique method:%n  %s%n",
									this.actual.getContent(), name)));
				}
				method = candidate;
			}
		}
		if (method == null) {
			String methodDescription = getMethodDescription(name, parameters);
			throw Failures.instance().failure(this.info,
					new BasicErrorMessageFactory(String.format(
							"%nExpecting actual:%n  %s%nto contain method:%n  %s%n",
							this.actual.getContent(), methodDescription)));
		}
		return new MethodAssert(method);
	}

	private boolean hasParameters(JavaMethod method, Class<?>[] requiredParameters) {
		List<JavaParameter> parameters = method.getParameters();
		if (parameters.size() != requiredParameters.length) {
			return false;
		}
		for (int i = 0; i < requiredParameters.length; i++) {
			if (!requiredParameters[i].getName().equals(
					parameters.get(i).getFullyQualifiedName())) {
				return false;
			}
		}
		return true;
	}

	private String getMethodDescription(String name, Class<?>... parameters) {
		return name + "(" + Arrays.stream(parameters).map(Class::getName).collect(
				Collectors.joining(", ")) + ")";
	}

	private JavaClass getJavaClass() {
		return this.actual.getJavaSource().getClasses().get(0);
	}

}
