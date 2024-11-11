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

package org.springframework.core;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Shepperd
 */
@SuppressWarnings("unchecked")
class ExceptionDepthComparatorTests {

	@Test
	void targetBeforeSameDepth() {
		Class<? extends Throwable> foundClass = findClosestMatch(TargetException.class, SameDepthException.class);
		assertThat(foundClass).isEqualTo(TargetException.class);
	}

	@Test
	void sameDepthBeforeTarget() {
		Class<? extends Throwable> foundClass = findClosestMatch(SameDepthException.class, TargetException.class);
		assertThat(foundClass).isEqualTo(TargetException.class);
	}

	@Test
	void lowestDepthBeforeTarget() {
		Class<? extends Throwable> foundClass = findClosestMatch(LowestDepthException.class, TargetException.class);
		assertThat(foundClass).isEqualTo(TargetException.class);
	}

	@Test
	void targetBeforeLowestDepth() {
		Class<? extends Throwable> foundClass = findClosestMatch(TargetException.class, LowestDepthException.class);
		assertThat(foundClass).isEqualTo(TargetException.class);
	}

	@Test
	void noDepthBeforeTarget() {
		Class<? extends Throwable> foundClass = findClosestMatch(NoDepthException.class, TargetException.class);
		assertThat(foundClass).isEqualTo(TargetException.class);
	}

	@Test
	void noDepthBeforeHighestDepth() {
		Class<? extends Throwable> foundClass = findClosestMatch(NoDepthException.class, HighestDepthException.class);
		assertThat(foundClass).isEqualTo(HighestDepthException.class);
	}

	@Test
	void highestDepthBeforeNoDepth() {
		Class<? extends Throwable> foundClass = findClosestMatch(HighestDepthException.class, NoDepthException.class);
		assertThat(foundClass).isEqualTo(HighestDepthException.class);
	}

	@Test
	void highestDepthBeforeLowestDepth() {
		Class<? extends Throwable> foundClass = findClosestMatch(HighestDepthException.class, LowestDepthException.class);
		assertThat(foundClass).isEqualTo(LowestDepthException.class);
	}

	@Test
	void lowestDepthBeforeHighestDepth() {
		Class<? extends Throwable> foundClass = findClosestMatch(LowestDepthException.class, HighestDepthException.class);
		assertThat(foundClass).isEqualTo(LowestDepthException.class);
	}

	private Class<? extends Throwable> findClosestMatch(
			Class<? extends Throwable>... classes) {
		return ExceptionDepthComparator.findClosestMatch(Arrays.asList(classes), new TargetException());
	}

	@SuppressWarnings("serial")
	public class HighestDepthException extends Throwable {
	}

	@SuppressWarnings("serial")
	public class LowestDepthException extends HighestDepthException {
	}

	@SuppressWarnings("serial")
	public class TargetException extends LowestDepthException {
	}

	@SuppressWarnings("serial")
	public class SameDepthException extends LowestDepthException {
	}

	@SuppressWarnings("serial")
	public class NoDepthException extends TargetException {
	}

}
