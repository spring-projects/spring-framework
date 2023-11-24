/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link ManagedSet}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class ManagedSetTests {

	@Test
	void mergeSunnyDay() {
		ManagedSet parent = ManagedSet.of("one", "two");
		ManagedSet child = ManagedSet.of("three");
		child.add("three");
		child.add("four");
		child.setMergeEnabled(true);
		Set mergedSet = child.merge(parent);
		assertThat(mergedSet).as("merge() obviously did not work.").containsExactly("one", "two", "three", "four");
	}

	@Test
	void mergeWithNullParent() {
		ManagedSet child = ManagedSet.of("one");
		child.setMergeEnabled(true);
		assertThat(child.merge(null)).isSameAs(child);
	}

	@Test
	void mergeNotAllowedWhenMergeNotEnabled() {
		assertThatIllegalStateException().isThrownBy(() -> new ManagedSet().merge(null));
	}

	@Test
	void mergeWithNonCompatibleParentType() {
		ManagedSet child = ManagedSet.of("one");
		child.setMergeEnabled(true);
		assertThatIllegalArgumentException().isThrownBy(() -> child.merge("hello"));
	}

	@Test
	void mergeEmptyChild() {
		ManagedSet parent = ManagedSet.of("one", "two");
		ManagedSet child = new ManagedSet();
		child.setMergeEnabled(true);
		Set mergedSet = child.merge(parent);
		assertThat(mergedSet).as("merge() obviously did not work.").containsExactly("one", "two");
	}

	@Test
	void mergeChildValuesOverrideTheParents() {
		// asserts that the set contract is not violated during a merge() operation...
		ManagedSet parent = ManagedSet.of("one", "two");
		ManagedSet child = ManagedSet.of("one");
		child.setMergeEnabled(true);
		Set mergedSet = child.merge(parent);
		assertThat(mergedSet).as("merge() obviously did not work.").containsExactly("one", "two");
	}

}
