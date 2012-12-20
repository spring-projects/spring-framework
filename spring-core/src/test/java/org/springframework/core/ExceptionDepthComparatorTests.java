/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Shepperd
 */
public class ExceptionDepthComparatorTests {

	@Test
	public void targetBeforeSameDepth() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(TargetException.class,
			SameDepthException.class);
		assertEquals(TargetException.class, foundClass);
	}

	@Test
	public void sameDepthBeforeTarget() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(
			SameDepthException.class, TargetException.class);
		assertEquals(TargetException.class, foundClass);
	}

	@Test
	public void lowestDepthBeforeTarget() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(
			LowestDepthException.class, TargetException.class);
		assertEquals(TargetException.class, foundClass);
	}

	@Test
	public void targetBeforeLowestDepth() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(TargetException.class,
			LowestDepthException.class);
		assertEquals(TargetException.class, foundClass);
	}

	@Test
	public void noDepthBeforeTarget() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(NoDepthException.class,
			TargetException.class);
		assertEquals(TargetException.class, foundClass);
	}

	@Test
	public void noDepthBeforeHighestDepth() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(NoDepthException.class,
			HighestDepthException.class);
		assertEquals(HighestDepthException.class, foundClass);
	}

	@Test
	public void highestDepthBeforeNoDepth() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(
			HighestDepthException.class, NoDepthException.class);
		assertEquals(HighestDepthException.class, foundClass);
	}

	@Test
	public void highestDepthBeforeLowestDepth() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(
			HighestDepthException.class, LowestDepthException.class);
		assertEquals(LowestDepthException.class, foundClass);
	}

	@Test
	public void lowestDepthBeforeHighestDepth() throws Exception {
		Class<? extends Throwable> foundClass = findClosestMatch(
			LowestDepthException.class, HighestDepthException.class);
		assertEquals(LowestDepthException.class, foundClass);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Throwable> findClosestMatch(Class<? extends Throwable> t1,
		Class<? extends Throwable> t2) {
		return ExceptionDepthComparator.findClosestMatch(Arrays.asList(t1, t2),
			new TargetException());
	}

	public class HighestDepthException extends Throwable {
	}

	public class LowestDepthException extends HighestDepthException {
	}

	public class TargetException extends LowestDepthException {
	}

	public class SameDepthException extends LowestDepthException {
	}

	public class NoDepthException extends TargetException {
	}

}
