/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.service.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.javapoet.CodeBlock;
import org.springframework.web.service.registry.GroupsMetadata.Registration;

/**
 * {@link ValueCodeGenerator.Delegate} for {@link GroupsMetadata}.
 *
 * @author Stephane Nicoll
 * @since 7.0
 */
final class GroupsMetadataValueDelegate implements ValueCodeGenerator.Delegate {

	@Override
	public @Nullable CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
		if (value instanceof Registration registration) {
			return generateRegistrationCode(valueCodeGenerator, registration);
		}
		if (value instanceof GroupsMetadata groupsMetadata) {
			return generateGroupsMetadataCode(valueCodeGenerator, groupsMetadata);
		}
		return null;
	}

	public CodeBlock generateRegistrationCode(ValueCodeGenerator
			valueCodeGenerator, Registration value) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("new $T($S, $L, $L)", Registration.class, value.name(),
				valueCodeGenerator.generateCode(value.clientType()),
				!value.httpServiceTypeNames().isEmpty() ?
						valueCodeGenerator.generateCode(value.httpServiceTypeNames()) :
						CodeBlock.of("new $T()", LinkedHashSet.class));
		return code.build();
	}

	private CodeBlock generateGroupsMetadataCode(ValueCodeGenerator valueCodeGenerator, GroupsMetadata groupsMetadata) {
		Collection<Registration> registrations = groupsMetadata.registrations()
				.collect(Collectors.toCollection(ArrayList::new));
		if (valueCodeGenerator.getGeneratedMethods() != null) {
			return valueCodeGenerator.getGeneratedMethods().add("getGroupsMetadata", method -> method
					.addJavadoc("Create the {@link $T}.", GroupsMetadata.class)
					.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
					.returns(GroupsMetadata.class)
					.addCode(generateGroupsMetadataMethod(valueCodeGenerator, registrations))).toMethodReference().toInvokeCodeBlock(ArgumentCodeGenerator.none());
		}
		else {
			return CodeBlock.of("new $T($L)", GroupsMetadata.class, valueCodeGenerator.generateCode(registrations));
		}
	}

	private CodeBlock generateGroupsMetadataMethod(
			ValueCodeGenerator valueCodeGenerator, Collection<Registration> registrations) {

		CodeBlock.Builder code = CodeBlock.builder();
		String registrationsVariable = "registrations";
		code.addStatement("$T<$T> $L = new $T<>()", List.class, Registration.class,
				registrationsVariable, ArrayList.class);
		registrations.forEach(registration ->
				code.addStatement("$L.add($L)", registrationsVariable,
						valueCodeGenerator.generateCode(registration))
		);
		code.addStatement("return new $T($L)", GroupsMetadata.class, registrationsVariable);
		return code.build();
	}

}
