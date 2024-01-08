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

package org.springframework.r2dbc.core;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import io.r2dbc.spi.test.MockColumnMetadata;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for R2DBC-based {@link DataClassRowMapper}.
 *
 * @author Simon Baslé
 * @author Juergen Hoeller
 * @since 6.1
 */
class R2dbcDataClassRowMapperTests {

	@Test
	void staticQueryWithDataClass() {
		DataClassRowMapper<ConstructorPerson> mapper = new DataClassRowMapper<>(ConstructorPerson.class);

		// uses name, age, birth_date
		ConstructorPerson person = mapper.apply(MOCK_ROW);

		assertThat(person.name).as("name").isEqualTo("Bubba");
		assertThat(person.age).as("age").isEqualTo(22L);
		assertThat(person.birth_date).as("birth_date").isNotNull();
	}

	@Test
	void staticQueryWithDataClassAndGenerics() {
		MockRow mockRow = buildMockRow("birth_date", true); // uses name, age, birth_date, balance (as list)
		// TODO validate actual R2DBC Row implementations would return something for balance if requesting a List
		DataClassRowMapper<ConstructorPersonWithGenerics> mapper = new DataClassRowMapper<>(ConstructorPersonWithGenerics.class);
		ConstructorPersonWithGenerics person = mapper.apply(mockRow);

		assertThat(person.name()).isEqualTo("Bubba");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance()).containsExactly(new BigDecimal("1234.56"));
	}

	@Test
	void staticQueryWithDataRecord() {
		DataClassRowMapper<RecordPerson> mapper = new DataClassRowMapper<>(RecordPerson.class);

		// uses name, age, birth_date, balance
		RecordPerson person = mapper.apply(MOCK_ROW);

		assertThat(person.name()).isEqualTo("Bubba");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));
	}

	@Test
	void staticQueryWithDataClassAndSetters() {
		MockRow mockRow = buildMockRow("birthdate", false); // uses name, age, birthdate (no underscore), balance
		DataClassRowMapper<ConstructorPersonWithSetters> mapper = new DataClassRowMapper<>(ConstructorPersonWithSetters.class);
		ConstructorPersonWithSetters person = mapper.apply(mockRow);

		assertThat(person.name()).isEqualTo("BUBBA");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birthDate()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));
	}


	static class ConstructorPerson {

		final String name;

		final long age;

		final Date birth_date;

		public ConstructorPerson(String name, long age, Date birth_date) {
			this.name = name;
			this.age = age;
			this.birth_date = birth_date;
		}

		public String name() {
			return this.name;
		}

		public long age() {
			return this.age;
		}

		public Date birth_date() {
			return this.birth_date;
		}
	}


	static class ConstructorPersonWithGenerics extends ConstructorPerson {

		private final List<BigDecimal> balance;

		public ConstructorPersonWithGenerics(String name, long age, Date birth_date, List<BigDecimal> balance) {
			super(name, age, birth_date);
			this.balance = balance;
		}

		public List<BigDecimal> balance() {
			return this.balance;
		}
	}


	static class ConstructorPersonWithSetters {

		private String name;

		private long age;

		private Date birthDate;

		private BigDecimal balance;

		public ConstructorPersonWithSetters(String name, long age, Date birthDate, BigDecimal balance) {
			this.name = name.toUpperCase();
			this.age = age;
			this.birthDate = birthDate;
			this.balance = balance;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setAge(long age) {
			this.age = age;
		}

		public void setBirthDate(Date birthDate) {
			this.birthDate = birthDate;
		}

		public void setBalance(BigDecimal balance) {
			this.balance = balance;
		}

		public String name() {
			return this.name;
		}

		public long age() {
			return this.age;
		}

		public Date birthDate() {
			return this.birthDate;
		}

		public BigDecimal balance() {
			return this.balance;
		}
	}


	record RecordPerson(String name, long age, Date birth_date, BigDecimal balance) {
	}


	static final MockRow MOCK_ROW = buildMockRow("birth_date", false);

	private static MockRow buildMockRow(String birthDateColumnName, boolean balanceObjectIdentifier) {
		MockRow.Builder builder = MockRow.builder();
		builder.metadata(MockRowMetadata.builder()
						.columnMetadata(MockColumnMetadata.builder().name("name").javaType(String.class).build())
						.columnMetadata(MockColumnMetadata.builder().name("age").javaType(long.class).build())
						.columnMetadata(MockColumnMetadata.builder().name(birthDateColumnName).javaType(Date.class).build())
						.columnMetadata(MockColumnMetadata.builder().name("balance").javaType(BigDecimal.class).build())
						.build())
				.identified(0, String.class, "Bubba")
				.identified(1, long.class, 22)
				.identified(2, Date.class, new Date(1221222L))
				.identified(3, BigDecimal.class, new BigDecimal("1234.56"));
		if (balanceObjectIdentifier) {
			builder.identified(3, Object.class, new BigDecimal("1234.56"));
		}
		return builder.build();
	}

}
