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

package org.springframework.beans;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.FieldSource;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Named.named;

/**
 * Unit tests for property descriptor resolution via
 * {@link PropertyDescriptorUtils#determineBasicProperties(Class)}.
 *
 * <p>Results are compared to the behavior of the standard {@link Introspector}.
 *
 * @author Sam Brannen
 * @since 6.2.16
 */
@ParameterizedClass(name = "{0}")
@FieldSource("resolvers")
class PropertyDescriptorUtilsPropertyResolutionTests {

	static final List<Named<PropertiesResolver>> resolvers = List.of(
			named("Basic Properties", new BasicPropertiesResolver()),
			named("Standard Properties", new StandardPropertiesResolver()));


	@Parameter
	PropertiesResolver resolver;


	@Test
	void determineBasicPropertiesWithUnresolvedGenericsInInterface() {
		var pdMap = resolver.resolve(GenericService.class);

		assertThat(pdMap).containsOnlyKeys("id");
		assertReadAndWriteMethodsForId(pdMap.get("id"), Object.class, Object.class);
	}

	@Test
	void determineBasicPropertiesWithUnresolvedGenericsInSubInterface() {
		// FYI: java.beans.Introspector does not resolve properties for sub-interfaces.
		assumeThat(resolver).isNotInstanceOf(StandardPropertiesResolver.class);

		var pdMap = resolver.resolve(SubGenericService.class);

		assertThat(pdMap).containsOnlyKeys("id");
		assertReadAndWriteMethodsForId(pdMap.get("id"), Object.class, Object.class);
	}

	@Test
	void resolvePropertiesWithUnresolvedGenericsInClass() {
		var pdMap = resolver.resolve(BaseService.class);

		assertReadAndWriteMethodsForClassAndId(pdMap, Object.class, Object.class);
	}

	@Test  // gh-36019
	void resolvePropertiesInSubclassWithOverriddenGetterAndSetter() {
		var pdMap = resolver.resolve(ServiceWithOverriddenGetterAndSetter.class);

		assertReadAndWriteMethodsForClassAndId(pdMap, String.class, String.class);
	}

	@Test  // gh-36019
	void resolvePropertiesWithUnresolvedGenericsInSubclassWithOverloadedSetter() {
		var pdMap = resolver.resolve(ServiceWithOverloadedSetter.class);

		assertReadAndWriteMethodsForClassAndId(pdMap, Object.class, Object.class);
	}

	@Test  // gh-36019
	void resolvePropertiesWithPartiallyUnresolvedGenericsInSubclassWithOverriddenGetter() {
		var pdMap = resolver.resolve(ServiceWithOverriddenGetter.class);

		assertReadAndWriteMethodsForClassAndId(pdMap, String.class, Object.class);
	}

	@Test  // gh-36019
	void resolvePropertiesWithPartiallyUnresolvedGenericsInSubclassWithOverriddenGetterAndOverloadedSetter() {
		var pdMap = resolver.resolve(ServiceWithOverriddenGetterAndOverloadedSetter.class);

		assertReadAndWriteMethodsForClassAndId(pdMap, String.class, Object.class);
	}


	private static void assertReadAndWriteMethodsForClassAndId(Map<String, List<PropertyDescriptor>> pdMap,
			Class<?> readType, Class<?> writeType) {

		assertThat(pdMap).containsOnlyKeys("class", "id");
		assertReadAndWriteMethodsForClass(pdMap.get("class"));
		assertReadAndWriteMethodsForId(pdMap.get("id"), readType, writeType);
	}

	private static void assertReadAndWriteMethodsForClass(List<PropertyDescriptor> pds) {
		assertThat(pds).hasSize(1);
		var pd = pds.get(0);
		assertThat(pd.getName()).isEqualTo("class");

		var readMethod = pd.getReadMethod();
		assertThat(readMethod.getName()).isEqualTo("getClass");
		assertThat(readMethod.getReturnType()).as("read type").isEqualTo(Class.class);
		assertThat(readMethod.getParameterCount()).isZero();

		assertThat(pd.getWriteMethod()).as("write method").isNull();
	}

	private static void assertReadAndWriteMethodsForId(List<PropertyDescriptor> pds, Class<?> readType, Class<?> writeType) {
		assertThat(pds).hasSize(1);
		var pd = pds.get(0);
		assertThat(pd.getName()).isEqualTo("id");

		var readMethod = pd.getReadMethod();
		var writeMethod = pd.getWriteMethod();

		assertSoftly(softly -> {
			softly.assertThat(readMethod.getName()).isEqualTo("getId");
			softly.assertThat(readMethod.getReturnType()).as("read type").isEqualTo(readType);
			softly.assertThat(readMethod.getParameterCount()).isZero();

			softly.assertThat(writeMethod).as("write method").isNotNull();
			if (writeMethod != null) {
				softly.assertThat(writeMethod.getName()).isEqualTo("setId");
				softly.assertThat(writeMethod.getReturnType()).isEqualTo(void.class);
				softly.assertThat(writeMethod.getParameterCount()).isEqualTo(1);
				softly.assertThat(writeMethod.getParameterTypes()[0]).as("write type").isEqualTo(writeType);
			}
		});
	}

	private static Map<String, List<PropertyDescriptor>> toMap(Stream<? extends PropertyDescriptor> stream) {
		return stream.collect(groupingBy(PropertyDescriptor::getName, toList()));
	}


	interface PropertiesResolver {

		Map<String, List<PropertyDescriptor>> resolve(Class<?> beanClass);
	}

	static class BasicPropertiesResolver implements PropertiesResolver {

		@Override
		public Map<String, List<PropertyDescriptor>> resolve(Class<?> beanClass) {
			try {
				var pds = PropertyDescriptorUtils.determineBasicProperties(beanClass);
				return toMap(pds.stream());
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	static class StandardPropertiesResolver implements PropertiesResolver {

		@Override
		public Map<String, List<PropertyDescriptor>> resolve(Class<?> beanClass) {
			try {
				var beanInfo = Introspector.getBeanInfo(beanClass);
				return toMap(Arrays.stream(beanInfo.getPropertyDescriptors()));
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	interface GenericService<T> {

		void setId(T id);

		T getId();
	}

	interface SubGenericService<T> extends GenericService<T> {
	}

	static class BaseService<T> {

		private T id;

		public T getId() {
			return id;
		}

		public void setId(T id) {
			this.id = id;
		}
	}

	static class ServiceWithOverriddenGetterAndSetter extends BaseService<String>
			implements SubGenericService<String> {

		@Override
		public String getId() {
			return super.getId();
		}

		@Override
		public void setId(String id) {
			super.setId(id);
		}
	}

	static class ServiceWithOverloadedSetter extends BaseService<String>
			implements SubGenericService<String> {

		public void setId(int id) {
			setId(String.valueOf(id));
		}
	}

	static class ServiceWithOverriddenGetter extends BaseService<String>
			implements SubGenericService<String> {

		@Override
		public String getId() {
			return super.getId();
		}
	}

	static class ServiceWithOverriddenGetterAndOverloadedSetter extends BaseService<String>
			implements SubGenericService<String> {

		@Override
		public String getId() {
			return super.getId();
		}

		public void setId(int id) {
			setId(String.valueOf(id));
		}
	}

}
