/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanTypeIndex}.
 *
 * @author Nazar Shuliak
 */
class BeanTypeIndexTests {

	private final BeanTypeIndex index = new BeanTypeIndex(new String[] {"first", "second", "third"});


	@Test
	void registerTypeWithSuperclassesAndInterfaces() {
		index.registerType("second", ArrayList.class);

		assertThat(index.getCandidateNames(ArrayList.class)).containsExactly("second");
		assertThat(index.getCandidateNames(AbstractList.class)).containsExactly("second");
		assertThat(index.getCandidateNames(Object.class)).containsExactly("second");
		assertThat(index.getCandidateNames(List.class)).containsExactly("second");
		assertThat(index.getCandidateNames(RandomAccess.class)).containsExactly("second");
		assertThat(index.getCandidateNames(Collection.class)).containsExactly("second");
		assertThat(index.getCandidateNames(Iterable.class)).containsExactly("second");
		assertThat(index.getCandidateNames(Serializable.class)).containsExactly("second");
		assertThat(index.getCandidateNames(String.class)).isEmpty();
	}

	@Test
	void registerInterfaceWithSuperInterfaces() {
		index.registerType("first", List.class);

		assertThat(index.getCandidateNames(Collection.class)).containsExactly("first");
		assertThat(index.getCandidateNames(Iterable.class)).containsExactly("first");
		assertThat(index.getCandidateNames(ArrayList.class)).isEmpty();
	}

	@Test
	void registerPrimitiveTypeAsWrapperType() {
		index.registerType("first", int.class);
		index.registerType("second", Integer.class);

		assertThat(index.getCandidateNames(int.class)).containsExactly("first", "second");
		assertThat(index.getCandidateNames(Integer.class)).containsExactly("first", "second");
		assertThat(index.getCandidateNames(Number.class)).containsExactly("first", "second");
		assertThat(index.getCandidateNames(long.class)).isEmpty();
	}

	@Test
	void registerArrayTypeAsCandidateForEveryType() {
		index.registerType("first", String[].class);

		assertThat(index.getCandidateNames(Object[].class)).containsExactly("first");
		assertThat(index.getCandidateNames(CharSequence[].class)).containsExactly("first");
		assertThat(index.getCandidateNames(String.class)).containsExactly("first");
	}

	@Test
	void registerUnknownTypeAsCandidateForEveryType() {
		index.registerType("second", String.class);
		index.registerUnknownType("third");

		assertThat(index.getCandidateNames(String.class)).containsExactly("second", "third");
		assertThat(index.getCandidateNames(Integer.class)).containsExactly("third");
	}

	@Test
	void registerEarlyReferenceAsCandidateUntilSingletonRegistered() {
		index.registerType("first", String.class);
		index.registerEarlyReference("first");
		assertThat(index.getCandidateNames(Integer.class)).containsExactly("first");

		index.registerSingleton("first", "value");
		assertThat(index.getCandidateNames(Integer.class)).isEmpty();
		assertThat(index.getCandidateNames(String.class)).containsExactly("first");
	}

	@Test
	void registerSingletonWithTypeNotPredicted() {
		index.registerType("first", Object.class);
		index.registerSingleton("first", "value");

		assertThat(index.getCandidateNames(CharSequence.class)).containsExactly("first");
	}

	@Test
	void registerFactoryBeanSingletonAsCandidateForEveryType() {
		index.registerType("first", Object.class);
		index.registerSingleton("first", new StringFactoryBean());

		assertThat(index.getCandidateNames(String.class)).containsExactly("first");
		assertThat(index.getCandidateNames(Integer.class)).containsExactly("first");
	}

	@Test
	void getCandidateNamesInRegistrationOrder() {
		index.registerType("third", String.class);
		index.registerUnknownType("second");
		index.registerType("first", String.class);

		assertThat(index.getCandidateNames(String.class)).containsExactly("first", "second", "third");
	}

	@Test
	void ignoreBeanNamesNotCoveredByIndex() {
		index.registerType("other", String.class);
		index.registerUnknownType("other");
		index.registerEarlyReference("other");
		index.registerSingleton("other", "value");

		assertThat(index.getCandidateNames(String.class)).isEmpty();
	}

	@Test
	void markComplete() {
		assertThat(index.isComplete()).isFalse();

		index.markComplete();
		assertThat(index.isComplete()).isTrue();
	}


	private static class StringFactoryBean implements FactoryBean<String> {

		@Override
		public String getObject() {
			return "value";
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}
	}

}
