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

package org.springframework.orm.jpa.support;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Internal code generator that can inject a value into a field or single-arg
 * method.
 *
 * <p>Generates code in the form:
 * <pre class="code">{@code
 * instance.age = value;
 * }</pre> or <pre class="code">{@code
 * instance.setAge(value);
 * }</pre>
 *
 * <p>Will also generate reflection based injection and register hints if the
 * member is not visible.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class InjectionCodeGenerator {

	private final ClassName targetClassName;

	private final RuntimeHints hints;


	InjectionCodeGenerator(ClassName targetClassName, RuntimeHints hints) {
		Assert.notNull(targetClassName, "ClassName must not be null");
		Assert.notNull(hints, "RuntimeHints must not be null");
		this.targetClassName = targetClassName;
		this.hints = hints;
	}


	CodeBlock generateInjectionCode(Member member, String instanceVariable, CodeBlock resourceToInject) {
		if (member instanceof Field field) {
			return generateFieldInjectionCode(field, instanceVariable, resourceToInject);
		}
		if (member instanceof Method method) {
			return generateMethodInjectionCode(method, instanceVariable, resourceToInject);
		}
		throw new IllegalStateException("Unsupported member type " + member.getClass().getName());
	}

	private CodeBlock generateFieldInjectionCode(Field field, String instanceVariable,
			CodeBlock resourceToInject) {

		CodeBlock.Builder code = CodeBlock.builder();
		AccessControl accessControl = AccessControl.forMember(field);
		if (!accessControl.isAccessibleFrom(this.targetClassName)) {
			this.hints.reflection().registerField(field);
			code.addStatement("$T field = $T.findField($T.class, $S)", Field.class,
					ReflectionUtils.class, field.getDeclaringClass(), field.getName());
			code.addStatement("$T.makeAccessible($L)", ReflectionUtils.class, "field");
			code.addStatement("$T.setField($L, $L, $L)", ReflectionUtils.class,
					"field", instanceVariable, resourceToInject);
		}
		else {
			code.addStatement("$L.$L = $L", instanceVariable, field.getName(), resourceToInject);
		}
		return code.build();
	}

	private CodeBlock generateMethodInjectionCode(Method method, String instanceVariable,
			CodeBlock resourceToInject) {

		Assert.isTrue(method.getParameterCount() == 1,
				() -> "Method '" + method.getName() + "' must declare a single parameter");
		CodeBlock.Builder code = CodeBlock.builder();
		AccessControl accessControl = AccessControl.forMember(method);
		if (!accessControl.isAccessibleFrom(this.targetClassName)) {
			this.hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
			code.addStatement("$T method = $T.findMethod($T.class, $S, $T.class)",
					Method.class, ReflectionUtils.class, method.getDeclaringClass(),
					method.getName(), method.getParameterTypes()[0]);
			code.addStatement("$T.makeAccessible($L)", ReflectionUtils.class, "method");
			code.addStatement("$T.invokeMethod($L, $L, $L)", ReflectionUtils.class,
					"method", instanceVariable, resourceToInject);
		}
		else {
			code.addStatement("$L.$L($L)", instanceVariable, method.getName(), resourceToInject);
		}
		return code.build();
	}

}
