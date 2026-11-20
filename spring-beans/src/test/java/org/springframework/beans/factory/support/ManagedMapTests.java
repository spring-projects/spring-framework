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
@SuppressWarnings({ "rawtypes", "unchecked" })
class ManagedMapTests {

	@Test
	void mergeSunnyDay() {
		ManagedMap parent = ManagedMap.ofEntries(Map.entry("one", "one"),
				Map.entry("two", "two"));
		ManagedMap child = ManagedMap.ofEntries(Map.entry("tree", "three"));
		child.setMergeEnabled(true);
		Map mergedMap = (Map) child.merge(parent);
		assertThat(mergedMap).as("merge() obviously did not work.").hasSize(3);
	}

	@Test
	void mergeWithNullParent() {
		ManagedMap child = new ManagedMap();
		child.setMergeEnabled(true);
		assertThat(child.merge(null)).isSameAs(child);
	}

	@Test
	void mergeWithNonCompatibleParentType() {
		ManagedMap map = new ManagedMap();
		map.setMergeEnabled(true);
		assertThatIllegalArgumentException().isThrownBy(() ->
				map.merge("hello"));
	}

	@Test
	void mergeNotAllowedWhenMergeNotEnabled() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ManagedMap().merge(null));
	}

	@Test
	void mergeEmptyChild() {
		ManagedMap parent = ManagedMap.ofEntries(Map.entry("one", "one"),
				Map.entry("two", "two"));
		ManagedMap child = new ManagedMap();
		child.setMergeEnabled(true);
		Map mergedMap = (Map) child.merge(parent);
		assertThat(mergedMap).as("merge() obviously did not work.").hasSize(2);
	}

	@Test
	void mergeChildValuesOverrideTheParents() {
		ManagedMap parent = ManagedMap.ofEntries(Map.entry("one", "one"),
				Map.entry("two", "two"));
		ManagedMap child = ManagedMap.ofEntries(Map.entry("one", "fork"));
		child.setMergeEnabled(true);
		Map mergedMap = (Map) child.merge(parent);
		// child value for 'one' must override parent value...
		assertThat(mergedMap).as("merge() obviously did not work.").hasSize(2);
		assertThat(mergedMap.get("one")).as("Parent value not being overridden during merge().").isEqualTo("fork");
	}

}
