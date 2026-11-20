/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	public static class HighestDepthException extends Throwable {
	}

	@SuppressWarnings("serial")
	public static class LowestDepthException extends HighestDepthException {
	}

	@SuppressWarnings("serial")
	public static class TargetException extends LowestDepthException {
	}

	@SuppressWarnings("serial")
	public static class SameDepthException extends LowestDepthException {
	}

	@SuppressWarnings("serial")
	public static class NoDepthException extends TargetException {
	}

}
