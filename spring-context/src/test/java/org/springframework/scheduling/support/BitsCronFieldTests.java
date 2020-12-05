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

package org.springframework.scheduling.support;

import java.util.Arrays;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
public class BitsCronFieldTests {

	@Test
	void parse() {
		assertThat(BitsCronField.parseSeconds("42")).has(clearRange(0, 41)).has(set(42)).has(clearRange(43, 59));
		assertThat(BitsCronField.parseMinutes("1,2,5,9")).has(clear(0)).has(set(1, 2)).has(clearRange(3,4)).has(set(5)).has(clearRange(6,8)).has(set(9)).has(clearRange(10,59));
		assertThat(BitsCronField.parseSeconds("0-4,8-12")).has(setRange(0, 4)).has(clearRange(5,7)).has(setRange(8, 12)).has(clearRange(13,59));
		assertThat(BitsCronField.parseHours("0-23/2")).has(set(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)).has(clear(1,3,5,7,9,11,13,15,17,19,21,23));
		assertThat(BitsCronField.parseDaysOfWeek("0")).has(clearRange(0, 6)).has(set(7, 7));
		assertThat(BitsCronField.parseSeconds("57/2")).has(clearRange(0, 56)).has(set(57)).has(clear(58)).has(set(59));
	}

	@Test
	void invalidRange() {
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds(""));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds("0-12/0"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds("60"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseMinutes("60"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfMonth("0"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfMonth("32"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseMonth("0"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseMonth("13"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfWeek("8"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds("20-10"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfWeek("*SUN"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfWeek("SUN*"));
		assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseHours("*ANYTHING_HERE"));
	}

	@Test
	void parseWildcards() {
		assertThat(BitsCronField.parseSeconds("*")).has(setRange(0, 60));
		assertThat(BitsCronField.parseMinutes("*")).has(setRange(0, 60));
		assertThat(BitsCronField.parseHours("*")).has(setRange(0, 23));
		assertThat(BitsCronField.parseDaysOfMonth("*")).has(clear(0)).has(setRange(1, 31));
		assertThat(BitsCronField.parseDaysOfMonth("?")).has(clear(0)).has(setRange(1, 31));
		assertThat(BitsCronField.parseMonth("*")).has(clear(0)).has(setRange(1, 12));
		assertThat(BitsCronField.parseDaysOfWeek("*")).has(clear(0)).has(setRange(1, 7));
		assertThat(BitsCronField.parseDaysOfWeek("?")).has(clear(0)).has(setRange(1, 7));
	}

	@Test
	void names() {
		assertThat(((BitsCronField)CronField.parseMonth("JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC")))
				.has(clear(0)).has(setRange(1, 12));
		assertThat(((BitsCronField)CronField.parseDaysOfWeek("SUN,MON,TUE,WED,THU,FRI,SAT")))
				.has(clear(0)).has(setRange(1, 7));
	}

	private static Condition<BitsCronField> set(int... indices) {
		return new Condition<BitsCronField>(String.format("set bits %s", Arrays.toString(indices))) {
			@Override
			public boolean matches(BitsCronField value) {
				for (int index : indices) {
					if (!value.getBit(index)) {
						return false;
					}
				}
				return true;
			}
		};
	}

	private static Condition<BitsCronField> setRange(int min, int max) {
		return new Condition<BitsCronField>(String.format("set range %d-%d", min, max)) {
			@Override
			public boolean matches(BitsCronField value) {
				for (int i = min; i < max; i++) {
					if (!value.getBit(i)) {
						return false;
					}
				}
				return true;
			}
		};
	}

	private static Condition<BitsCronField> clear(int... indices) {
		return new Condition<BitsCronField>(String.format("clear bits %s", Arrays.toString(indices))) {
			@Override
			public boolean matches(BitsCronField value) {
				for (int index : indices) {
					if (value.getBit(index)) {
						return false;
					}
				}
				return true;
			}
		};
	}

	private static Condition<BitsCronField> clearRange(int min, int max) {
		return new Condition<BitsCronField>(String.format("clear range %d-%d", min, max)) {
			@Override
			public boolean matches(BitsCronField value) {
				for (int i = min; i < max; i++) {
					if (value.getBit(i)) {
						return false;
					}
				}
				return true;
			}
		};
	}

}
