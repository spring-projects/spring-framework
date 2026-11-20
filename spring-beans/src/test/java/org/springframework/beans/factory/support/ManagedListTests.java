/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ManagedList}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class ManagedListTests {

	@Test
	void mergeSunnyDay() {
		ManagedList parent = ManagedList.of("one", "two");
		ManagedList child = ManagedList.of("three");
		child.setMergeEnabled(true);
		List mergedList = child.merge(parent);
		assertThat(mergedList).as("merge() obviously did not work.").containsExactly("one", "two", "three");
	}

	@Test
	void mergeWithNullParent() {
		ManagedList child = ManagedList.of("one");
		child.setMergeEnabled(true);
		assertThat(child.merge(null)).isSameAs(child);
	}

	@Test
	void mergeNotAllowedWhenMergeNotEnabled() {
		ManagedList child = new ManagedList();
		assertThatIllegalStateException().isThrownBy(() -> child.merge(null));
	}

	@Test
	void mergeWithIncompatibleParentType() {
		ManagedList child = ManagedList.of("one");
		child.setMergeEnabled(true);
		assertThatIllegalArgumentException().isThrownBy(() -> child.merge("hello"));
	}

	@Test
	void mergeEmptyChild() {
		ManagedList parent = ManagedList.of("one", "two");
		ManagedList child = new ManagedList();
		child.setMergeEnabled(true);
		List mergedList = child.merge(parent);
		assertThat(mergedList).as("merge() obviously did not work.").containsExactly("one", "two");
	}

	@Test
	void mergedChildValuesDoNotOverrideTheParents() {
		// doesn't make much sense in the context of a list...
		ManagedList parent = ManagedList.of("one", "two");
		ManagedList child = ManagedList.of("one");
		child.setMergeEnabled(true);
		List mergedList = child.merge(parent);
		assertThat(mergedList).as("merge() obviously did not work.").containsExactly("one", "two", "one");
	}

}
