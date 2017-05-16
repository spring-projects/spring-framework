/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ManagedSetTests {

	@Test
	public void mergeSunnyDay() {
		ManagedSet parent = new ManagedSet();
		parent.add("one");
		parent.add("two");
		ManagedSet child = new ManagedSet();
		child.add("three");
		child.setMergeEnabled(true);
		Set mergedSet = child.merge(parent);
		assertEquals("merge() obviously did not work.", 3, mergedSet.size());
	}

	@Test
	public void mergeWithNullParent() {
		ManagedSet child = new ManagedSet();
		child.add("one");
		child.setMergeEnabled(true);
		assertSame(child, child.merge(null));
	}

	@Test(expected = IllegalStateException.class)
	public void mergeNotAllowedWhenMergeNotEnabled() {
		new ManagedSet().merge(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void mergeWithNonCompatibleParentType() {
		ManagedSet child = new ManagedSet();
		child.add("one");
		child.setMergeEnabled(true);
		child.merge("hello");
	}

	@Test
	public void mergeEmptyChild() {
		ManagedSet parent = new ManagedSet();
		parent.add("one");
		parent.add("two");
		ManagedSet child = new ManagedSet();
		child.setMergeEnabled(true);
		Set mergedSet = child.merge(parent);
		assertEquals("merge() obviously did not work.", 2, mergedSet.size());
	}

	@Test
	public void mergeChildValuesOverrideTheParents() {
		// asserts that the set contract is not violated during a merge() operation...
		ManagedSet parent = new ManagedSet();
		parent.add("one");
		parent.add("two");
		ManagedSet child = new ManagedSet();
		child.add("one");
		child.setMergeEnabled(true);
		Set mergedSet = child.merge(parent);
		assertEquals("merge() obviously did not work.", 2, mergedSet.size());
	}

}
