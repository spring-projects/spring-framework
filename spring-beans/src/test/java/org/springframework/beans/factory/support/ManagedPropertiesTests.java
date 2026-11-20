/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("rawtypes")
class ManagedPropertiesTests {

	@Test
	@SuppressWarnings("unchecked")
	void mergeSunnyDay() {
		ManagedProperties parent = new ManagedProperties();
		parent.setProperty("one", "one");
		parent.setProperty("two", "two");
		ManagedProperties child = new ManagedProperties();
		child.setProperty("three", "three");
		child.setMergeEnabled(true);
		Map mergedMap = (Map) child.merge(parent);
		assertThat(mergedMap).as("merge() obviously did not work.").hasSize(3);
	}

	@Test
	void mergeWithNullParent() {
		ManagedProperties child = new ManagedProperties();
		child.setMergeEnabled(true);
		assertThat(child.merge(null)).isSameAs(child);
	}

	@Test
	void mergeWithNonCompatibleParentType() {
		ManagedProperties map = new ManagedProperties();
		map.setMergeEnabled(true);
		assertThatIllegalArgumentException().isThrownBy(() ->
				map.merge("hello"));
	}

	@Test
	void mergeNotAllowedWhenMergeNotEnabled() {
		ManagedProperties map = new ManagedProperties();
		assertThatIllegalStateException().isThrownBy(() ->
				map.merge(null));
	}

	@Test
	@SuppressWarnings("unchecked")
	void mergeEmptyChild() {
		ManagedProperties parent = new ManagedProperties();
		parent.setProperty("one", "one");
		parent.setProperty("two", "two");
		ManagedProperties child = new ManagedProperties();
		child.setMergeEnabled(true);
		Map mergedMap = (Map) child.merge(parent);
		assertThat(mergedMap).as("merge() obviously did not work.").hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void mergeChildValuesOverrideTheParents() {
		ManagedProperties parent = new ManagedProperties();
		parent.setProperty("one", "one");
		parent.setProperty("two", "two");
		ManagedProperties child = new ManagedProperties();
		child.setProperty("one", "fork");
		child.setMergeEnabled(true);
		Map mergedMap = (Map) child.merge(parent);
		// child value for 'one' must override parent value...
		assertThat(mergedMap).as("merge() obviously did not work.").hasSize(2);
		assertThat(mergedMap.get("one")).as("Parent value not being overridden during merge().").isEqualTo("fork");
	}

}
