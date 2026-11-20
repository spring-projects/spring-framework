/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ManagedSet}.
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
