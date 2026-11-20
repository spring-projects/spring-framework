/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing implementations of
 * {@link ClassMetadata#getMemberClassNames()}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class AbstractClassMetadataMemberClassTests {

	protected abstract ClassMetadata getClassMetadataFor(Class<?> clazz);

	@Test
	void withNoMemberClasses() {
		ClassMetadata metadata = getClassMetadataFor(L0_a.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses).isEmpty();
	}

	@Test
	void withPublicMemberClasses() {
		ClassMetadata metadata = getClassMetadataFor(L0_b.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses).containsOnly(L0_b.L1.class.getName());
	}

	@Test
	void withNonPublicMemberClasses() {
		ClassMetadata metadata = getClassMetadataFor(L0_c.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses).containsOnly(L0_c.L1.class.getName());
	}

	@Test
	void againstMemberClass() {
		ClassMetadata metadata = getClassMetadataFor(L0_b.L1.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses).isEmpty();
	}

	public static class L0_a {
	}

	public static class L0_b {
		public static class L1 { }
	}

	public static class L0_c {
		private static class L1 { }
	}

}
