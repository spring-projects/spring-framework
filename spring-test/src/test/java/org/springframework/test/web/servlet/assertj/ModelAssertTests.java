/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.assertj;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ModelAssert}.
 *
 * @author Stephane Nicoll
 */
class ModelAssertTests {

	@Test
	void hasErrors() {
		assertThat(forModel(new TestBean(), Map.of("name", "John", "age", "4x"))).hasErrors();
	}

	@Test
	void hasErrorsWithNoError() {
		AssertProvider<ModelAssert> actual = forModel(new TestBean(), Map.of("name", "John", "age", "42"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).hasErrors())
				.withMessageContainingAll("John", "to have at least one error");
	}

	@Test
	void doesNotHaveErrors() {
		assertThat(forModel(new TestBean(), Map.of("name", "John", "age", "42"))).doesNotHaveErrors();
	}

	@Test
	void doesNotHaveErrorsWithError() {
		AssertProvider<ModelAssert> actual = forModel(new TestBean(), Map.of("name", "John", "age", "4x"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).doesNotHaveErrors())
				.withMessageContainingAll("John", "to not have an error, but got 1");
	}

	@Test
	void extractBindingResultForAttributeInError() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "person", new TestBean(), Map.of("name", "John", "age", "4x", "touchy", "invalid.value"));
		assertThat(forModel(model)).extractingBindingResult("person").hasErrorsCount(2);
	}

	@Test
	void hasErrorCountForUnknownAttribute() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "person", new TestBean(), Map.of("name", "John", "age", "42"));
		AssertProvider<ModelAssert> actual = forModel(model);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).extractingBindingResult("user"))
				.withMessageContainingAll("to have a binding result for attribute 'user'");
	}

	@Test
	void hasErrorsWithMatchingAttributes() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "wrong1", new TestBean(), Map.of("name", "first", "age", "4x"));
		augmentModel(model, "valid", new TestBean(), Map.of("name", "second"));
		augmentModel(model, "wrong2", new TestBean(), Map.of("name", "third", "touchy", "invalid.name"));
		assertThat(forModel(model)).hasAttributeErrors("wrong1", "wrong2");
	}

	@Test
	void hasErrorsWithOneNonMatchingAttribute() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "wrong1", new TestBean(), Map.of("name", "first", "age", "4x"));
		augmentModel(model, "valid", new TestBean(), Map.of("name", "second"));
		augmentModel(model, "wrong2", new TestBean(), Map.of("name", "third", "touchy", "invalid.name"));
		AssertProvider<ModelAssert> actual = forModel(model);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).hasAttributeErrors("wrong1", "valid"))
				.withMessageContainingAll("to have attribute errors for:", "wrong1, valid",
						"but these attributes do not have any errors:", "valid");
	}

	@Test
	void hasErrorsWithOneNonMatchingAttributeAndOneUnknownAttribute() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "wrong1", new TestBean(), Map.of("name", "first", "age", "4x"));
		augmentModel(model, "valid", new TestBean(), Map.of("name", "second"));
		augmentModel(model, "wrong2", new TestBean(), Map.of("name", "third", "touchy", "invalid.name"));
		AssertProvider<ModelAssert> actual = forModel(model);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).hasAttributeErrors("wrong1", "unknown", "valid"))
				.withMessageContainingAll("to have attribute errors for:", "wrong1, unknown, valid",
						"but could not find these attributes:", "unknown",
						"and these attributes do not have any errors:", "valid");
	}

	@Test
	void doesNotHaveErrorsWithMatchingAttributes() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "valid1", new TestBean(), Map.of("name", "first"));
		augmentModel(model, "wrong", new TestBean(), Map.of("name", "second", "age", "4x"));
		augmentModel(model, "valid2", new TestBean(), Map.of("name", "third"));
		assertThat(forModel(model)).doesNotHaveAttributeErrors("valid1", "valid2");
	}

	@Test
	void doesNotHaveErrorsWithOneNonMatchingAttribute() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "valid1", new TestBean(), Map.of("name", "first"));
		augmentModel(model, "wrong", new TestBean(), Map.of("name", "second", "age", "4x"));
		augmentModel(model, "valid2", new TestBean(), Map.of("name", "third"));
		AssertProvider<ModelAssert> actual = forModel(model);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).doesNotHaveAttributeErrors("valid1", "wrong"))
				.withMessageContainingAll("to have attribute without errors for:", "valid1, wrong",
						"but these attributes have at least one error:", "wrong");
	}

	@Test
	void doesNotHaveErrorsWithOneNonMatchingAttributeAndOneUnknownAttribute() {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "valid1", new TestBean(), Map.of("name", "first"));
		augmentModel(model, "wrong", new TestBean(), Map.of("name", "second", "age", "4x"));
		augmentModel(model, "valid2", new TestBean(), Map.of("name", "third"));
		AssertProvider<ModelAssert> actual = forModel(model);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(actual).doesNotHaveAttributeErrors("valid1", "unknown", "wrong"))
				.withMessageContainingAll("to have attribute without errors for:", "valid1, unknown, wrong",
						"but could not find these attributes:", "unknown",
						"and these attributes have at least one error:", "wrong");
	}

	private AssertProvider<ModelAssert> forModel(Map<String, Object> model) {
		return () -> new ModelAssert(model);
	}

	private AssertProvider<ModelAssert> forModel(Object instance, Map<String, Object> propertyValues) {
		Map<String, Object> model = new HashMap<>();
		augmentModel(model, "test", instance, propertyValues);
		return forModel(model);
	}

	private static void augmentModel(Map<String, Object> model, String attribute, Object instance, Map<String, Object> propertyValues) {
		DataBinder binder = new DataBinder(instance, attribute);
		MutablePropertyValues pvs = new MutablePropertyValues(propertyValues);
		binder.bind(pvs);
		try {
			binder.close();
			model.putAll(binder.getBindingResult().getModel());
		}
		catch (BindException ex) {
			model.putAll(ex.getBindingResult().getModel());
		}
	}

}
