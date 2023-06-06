/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.r2dbc.core;

import io.r2dbc.spi.Readable;
import io.r2dbc.spi.test.MockColumnMetadata;
import io.r2dbc.spi.test.MockOutParameters;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for R2DBC-based {@link BeanPropertyRowMapper}.
 *
 * @since 6.1
 */
class R2dbcBeanPropertyRowMapperTests {

	@Test
	void mappingUnknownReadableRejected() {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
		assertThatIllegalArgumentException().isThrownBy(() -> mapper.apply(Mockito.mock(Readable.class)))
				.withMessageStartingWith("Can only map Readable Row or OutParameters, got io.r2dbc.spi.Readable$MockitoMock$");
	}

	@Test
	void mappingOutParametersAccepted() {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
		assertThatNoException().isThrownBy(() -> mapper.apply(MockOutParameters.empty()));
	}

	@Test
	void mappingRowSimpleObject() {
		MockRow mockRow = SIMPLE_PERSON_ROW;
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);

		Person result = mapper.apply(mockRow);

		assertThat(result.firstName).as("firstName").isEqualTo("John");
		assertThat(result.lastName).as("lastName").isEqualTo("Doe");
		assertThat(result.age).as("age").isEqualTo(30);
	}

	@Test
	void mappingRowMissingAttributeAccepted() {
		MockRow mockRow = SIMPLE_PERSON_ROW;
		BeanPropertyRowMapper<ExtendedPerson> mapper = new BeanPropertyRowMapper<>(ExtendedPerson.class);

		ExtendedPerson result = mapper.apply(mockRow);

		assertThat(result.firstName).as("firstName").isEqualTo("John");
		assertThat(result.lastName).as("lastName").isEqualTo("Doe");
		assertThat(result.age).as("age").isEqualTo(30);
		assertThat(result.address).as("address").isNull();
	}

	@Test
	void mappingRowWithDifferentName() {
		MockRow mockRow = EMAIL_PERSON_ROW;
		BeanPropertyRowMapper<EmailPerson> mapper = new BeanPropertyRowMapper<>(EmailPerson.class);

		EmailPerson result = mapper.apply(mockRow);

		assertThat(result.firstName).as("firstName").isEqualTo("John");
		assertThat(result.lastName).as("lastName").isEqualTo("Doe");
		assertThat(result.age).as("age").isEqualTo(30);
		assertThat(result.email).as("email").isEqualTo("mail@example.org");
	}

	@Test
	void mappingRowMissingAttributeRejected() {
		Class<ExtendedPerson> mappedClass = ExtendedPerson.class;
		MockRow mockRow = SIMPLE_PERSON_ROW;
		BeanPropertyRowMapper<ExtendedPerson> mapper = new BeanPropertyRowMapper<>(mappedClass, true);

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> mapper.apply(mockRow))
				.withMessage("Given readable does not contain all items necessary to populate object of %s"
						+ ": [firstName, lastName, address, age]", mappedClass);
	}

	// TODO cannot trigger a mapping of a read-only property, as mappedProperties don't include properties without a setter.

	@Test
	void rowTypeAndMappingTypeMisaligned() {
		MockRow mockRow = EXTENDED_PERSON_ROW;
		BeanPropertyRowMapper<TypeMismatchExtendedPerson> mapper = new BeanPropertyRowMapper<>(TypeMismatchExtendedPerson.class);

		assertThatExceptionOfType(TypeMismatchException.class)
				.isThrownBy(() -> mapper.apply(mockRow))
				.withMessage("Failed to convert property value of type 'java.lang.String' to required type "
						+ "'java.lang.String' for property 'address'; simulating type mismatch for address");
	}

	@Test
	void usePrimitiveDefaultWithNullValueFromRow() {
		MockRow mockRow = MockRow.builder()
				.metadata(MockRowMetadata.builder()
						.columnMetadata(MockColumnMetadata.builder().name("firstName").javaType(String.class).build())
						.columnMetadata(MockColumnMetadata.builder().name("lastName").javaType(String.class).build())
						.columnMetadata(MockColumnMetadata.builder().name("age").javaType(Integer.class).build())
						.build())
				.identified(0, String.class, "John")
				.identified(1, String.class, "Doe")
				.identified(2, int.class, null)
				.identified(3, String.class, "123 Sesame Street")
				.build();
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
		mapper.setPrimitivesDefaultedForNullValue(true);

		Person result = mapper.apply(mockRow);

		assertThat(result.getAge()).isZero();
	}

	@ParameterizedTest
	@CsvSource({
			"age, age",
			"lastName, last_name",
			"Name, name",
			"FirstName, first_name",
			"EMail, e_mail",
			"URL, u_r_l", // likely undesirable, but that's the status quo
	})
	void underscoreName(String input, String expected) {
		BeanPropertyRowMapper<?> mapper = new BeanPropertyRowMapper<>(Object.class);
		assertThat(mapper.underscoreName(input)).isEqualTo(expected);
	}


	@SuppressWarnings("unused")
	private static class Person {

		String firstName;

		String lastName;

		int age;

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}


	@SuppressWarnings("unused")
	private static class ExtendedPerson extends Person {

		String address;

		public String getAddress() {
			return this.address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
	}


	private static class TypeMismatchExtendedPerson extends ExtendedPerson {

		@Override
		public void setAddress(String address) {
			throw new ClassCastException("simulating type mismatch for address");
		}
	}


	@SuppressWarnings("unused")
	private static class EmailPerson extends Person {

		String email;

		public String getEmail() {
			return this.email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}


	private static final MockRow SIMPLE_PERSON_ROW = MockRow.builder()
			.metadata(MockRowMetadata.builder()
					.columnMetadata(MockColumnMetadata.builder().name("firstName").javaType(String.class).build())
					.columnMetadata(MockColumnMetadata.builder().name("lastName").javaType(String.class).build())
					.columnMetadata(MockColumnMetadata.builder().name("age").javaType(Integer.class).build())
					.build())
			.identified(0, String.class, "John")
			.identified(1, String.class, "Doe")
			.identified(2, int.class, 30)
			.build();

	private static final MockRow EXTENDED_PERSON_ROW = MockRow.builder()
			.metadata(MockRowMetadata.builder()
					.columnMetadata(MockColumnMetadata.builder().name("firstName").javaType(String.class).build())
					.columnMetadata(MockColumnMetadata.builder().name("lastName").javaType(String.class).build())
					.columnMetadata(MockColumnMetadata.builder().name("age").javaType(Integer.class).build())
					.columnMetadata(MockColumnMetadata.builder().name("address").javaType(String.class).build())
					.build())
			.identified(0, String.class, "John")
			.identified(1, String.class, "Doe")
			.identified(2, int.class, 30)
			.identified(3, String.class, "123 Sesame Street")
			.build();

	private static final MockRow EMAIL_PERSON_ROW = buildRowWithExtraColumn("EMail", String.class,
			String.class, "mail@example.org");

	private static MockRow buildRowWithExtraColumn(
			String extraColumnName, Class<?> extraColumnClass, Class<?> identifiedClass, Object value) {

		return MockRow.builder()
				.metadata(MockRowMetadata.builder()
						.columnMetadata(MockColumnMetadata.builder().name("firstName").javaType(String.class).build())
						.columnMetadata(MockColumnMetadata.builder().name("last_name").javaType(String.class).build())
						.columnMetadata(MockColumnMetadata.builder().name("age").javaType(Integer.class).build())
						.columnMetadata(MockColumnMetadata.builder().name(extraColumnName).javaType(extraColumnClass).build())
						.build())
				.identified(0, String.class, "John")
				.identified(1, String.class, "Doe")
				.identified(2, int.class, 30)
				.identified(3, identifiedClass, value)
				.build();
	}

}
