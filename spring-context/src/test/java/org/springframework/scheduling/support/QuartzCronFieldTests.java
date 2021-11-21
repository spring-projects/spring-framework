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

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link QuartzCronField}.
 *
 * @author Arjen Poutsma
 */
class QuartzCronFieldTests {

	@Test
	void lastDayOfMonth() {
		QuartzCronField field = QuartzCronField.parseDaysOfMonth("L");

		LocalDate last = LocalDate.of(2020, 6, 16);
		LocalDate expected = LocalDate.of(2020, 6, 30);
		assertThat(field.nextOrSame(last)).isEqualTo(expected);
	}

	@Test
	void lastDayOfMonthOffset() {
		QuartzCronField field = QuartzCronField.parseDaysOfMonth("L-3");

		LocalDate last = LocalDate.of(2020, 6, 16);
		LocalDate expected = LocalDate.of(2020, 6, 27);
		assertThat(field.nextOrSame(last)).isEqualTo(expected);
	}

	@Test
	void lastWeekdayOfMonth() {
		QuartzCronField field = QuartzCronField.parseDaysOfMonth("LW");

		LocalDate last = LocalDate.of(2020, 6, 16);
		LocalDate expected = LocalDate.of(2020, 6, 30);
		LocalDate actual = field.nextOrSame(last);
		assertThat(actual).isNotNull();
		assertThat(actual.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void lastDayOfWeekOffset() {
		// last Thursday (4) of the month
		QuartzCronField field = QuartzCronField.parseDaysOfWeek("4L");

		LocalDate last = LocalDate.of(2020, 6, 16);
		LocalDate expected = LocalDate.of(2020, 6, 25);
		assertThat(field.nextOrSame(last)).isEqualTo(expected);
	}

	@Test
	void invalidValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth(""));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("1L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("LL"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("4L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("0L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("W"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("W1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("WW"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("32W"));

		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek(""));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("L1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("LL"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("-4L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("8L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("#"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("1#"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("#1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("1#L"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("L#1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("8#1"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("2#1,2#3,2#5"));
		assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("FRI#-1"));
	}

}
