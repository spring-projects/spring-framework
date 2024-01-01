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

package org.springframework.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;






/**
 * Tests for {@link PropertyMatches}.
 *
 * @author Stephane Nicoll
 */
class PropertyMatchesTests {

	@Test
	void simpleBeanPropertyTypo() {
		PropertyMatches matches = PropertyMatches.forProperty("naem", SampleBeanProperties.class);
		assertThat(matches.getPossibleMatches()).contains("name");
	}

	@Test
	void complexBeanPropertyTypo() {
		PropertyMatches matches = PropertyMatches.forProperty("desriptn", SampleBeanProperties.class);
		assertThat(matches.getPossibleMatches()).isEmpty();
	}

	@Test
	void unknownBeanProperty() {
		PropertyMatches matches = PropertyMatches.forProperty("unknown", SampleBeanProperties.class);
		assertThat(matches.getPossibleMatches()).isEmpty();
	}

	@Test
	void severalMatchesBeanProperty() {
		PropertyMatches matches = PropertyMatches.forProperty("counter", SampleBeanProperties.class);
		assertThat(matches.getPossibleMatches()).contains("counter1");
		assertThat(matches.getPossibleMatches()).contains("counter2");
		assertThat(matches.getPossibleMatches()).contains("counter3");
	}

	@Test
	void simpleBeanPropertyErrorMessage() {
		PropertyMatches matches = PropertyMatches.forProperty("naem", SampleBeanProperties.class);
		String msg = matches.buildErrorMessage();
		assertThat(msg).contains("naem");
		assertThat(msg).contains("name");
		assertThat(msg).contains("setter");
		assertThat(msg).doesNotContain("field");
	}

	@Test
	void complexBeanPropertyErrorMessage() {
		PropertyMatches matches = PropertyMatches.forProperty("counter", SampleBeanProperties.class);
		String msg = matches.buildErrorMessage();
		assertThat(msg).contains("counter");
		assertThat(msg).contains("counter1");
		assertThat(msg).contains("counter2");
		assertThat(msg).contains("counter3");
	}

	@Test
	void simpleFieldPropertyTypo() {
		PropertyMatches matches = PropertyMatches.forField("naem", SampleFieldProperties.class);
		assertThat(matches.getPossibleMatches()).contains("name");
	}

	@Test
	void complexFieldPropertyTypo() {
		PropertyMatches matches = PropertyMatches.forField("desriptn", SampleFieldProperties.class);
		assertThat(matches.getPossibleMatches()).isEmpty();
	}

	@Test
	void unknownFieldProperty() {
		PropertyMatches matches = PropertyMatches.forField("unknown", SampleFieldProperties.class);
		assertThat(matches.getPossibleMatches()).isEmpty();
	}

	@Test
	void severalMatchesFieldProperty() {
		PropertyMatches matches = PropertyMatches.forField("counter", SampleFieldProperties.class);
		assertThat(matches.getPossibleMatches()).contains("counter1");
		assertThat(matches.getPossibleMatches()).contains("counter2");
		assertThat(matches.getPossibleMatches()).contains("counter3");
	}

	@Test
	void simpleFieldPropertyErrorMessage() {
		PropertyMatches matches = PropertyMatches.forField("naem", SampleFieldProperties.class);
		String msg = matches.buildErrorMessage();
		assertThat(msg).contains("naem");
		assertThat(msg).contains("name");
		assertThat(msg).contains("field");
		assertThat(msg).doesNotContain("setter");
	}

	@Test
	void complexFieldPropertyErrorMessage() {
		PropertyMatches matches = PropertyMatches.forField("counter", SampleFieldProperties.class);
		String msg = matches.buildErrorMessage();
		assertThat(msg).contains("counter");
		assertThat(msg).contains("counter1");
		assertThat(msg).contains("counter2");
		assertThat(msg).contains("counter3");
	}


	@SuppressWarnings("unused")
	private static class SampleBeanProperties {

		private String name;

		private String description;

		private int counter1;

		private int counter2;

		private int counter3;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public int getCounter1() {
			return counter1;
		}

		public void setCounter1(int counter1) {
			this.counter1 = counter1;
		}

		public int getCounter2() {
			return counter2;
		}

		public void setCounter2(int counter2) {
			this.counter2 = counter2;
		}

		public int getCounter3() {
			return counter3;
		}

		public void setCounter3(int counter3) {
			this.counter3 = counter3;
		}
	}


	@SuppressWarnings("unused")
	private static class SampleFieldProperties {

		private String name;

		private String description;

		private int counter1;

		private int counter2;

		private int counter3;

	}

}
