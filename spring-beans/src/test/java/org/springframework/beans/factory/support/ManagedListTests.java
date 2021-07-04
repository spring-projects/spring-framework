/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ManagedListTests {

	@Test
	public void mergeSunnyDay() {
		ManagedList parent = new ManagedList();
		parent.add("one");
		parent.add("two");
		ManagedList child = new ManagedList();
		child.add("three");
		child.setMergeEnabled(true);
		List mergedList = child.merge(parent);
		assertThat(mergedList.size()).as("merge() obviously did not work.").isEqualTo(3);
	}

	@Test
	public void mergeWithNullParent() {
		ManagedList child = new ManagedList();
		child.add("one");
		child.setMergeEnabled(true);
		assertThat(child.merge(null)).isSameAs(child);
	}

	@Test
	public void mergeNotAllowedWhenMergeNotEnabled() {
		ManagedList child = new ManagedList();
		assertThatIllegalStateException().isThrownBy(() ->
				child.merge(null));
	}

	@Test
	public void mergeWithNonCompatibleParentType() {
		ManagedList child = new ManagedList();
		child.add("one");
		child.setMergeEnabled(true);
		assertThatIllegalArgumentException().isThrownBy(() ->
				child.merge("hello"));
	}

	@Test
	public void mergeEmptyChild() {
		ManagedList parent = new ManagedList();
		parent.add("one");
		parent.add("two");
		ManagedList child = new ManagedList();
		child.setMergeEnabled(true);
		List mergedList = child.merge(parent);
		assertThat(mergedList.size()).as("merge() obviously did not work.").isEqualTo(2);
	}

	@Test
	public void mergeChildValuesOverrideTheParents() {
		// doesn't make much sense in the context of a list...
		ManagedList parent = new ManagedList();
		parent.add("one");
		parent.add("two");
		ManagedList child = new ManagedList();
		child.add("one");
		child.setMergeEnabled(true);
		List mergedList = child.merge(parent);
		assertThat(mergedList.size()).as("merge() obviously did not work.").isEqualTo(3);
	}

}
