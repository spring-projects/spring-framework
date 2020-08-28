/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jdbc.core;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.test.ConstructorPerson;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 5.3
 */
public class DataClassRowMapperTests extends AbstractRowMapperTests {

	@Test
	public void testStaticQueryWithDataClass() throws Exception {
		Mock mock = new Mock();
		List<ConstructorPerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new DataClassRowMapper<>(ConstructorPerson.class));
		assertThat(result.size()).isEqualTo(1);
		verifyPerson(result.get(0));

		mock.verifyClosed();
	}

}
