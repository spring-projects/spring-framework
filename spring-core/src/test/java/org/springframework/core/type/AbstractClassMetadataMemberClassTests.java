/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.type;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Abstract base class for testing implementations of
 * {@link ClassMetadata#getMemberClassNames()}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class AbstractClassMetadataMemberClassTests {

	public abstract ClassMetadata getClassMetadataFor(Class<?> clazz);

	@Test
	public void withNoMemberClasses() {
		ClassMetadata metadata = getClassMetadataFor(L0_a.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses, equalTo(new String[]{}));
	}

	public static class L0_a {
	}


	@Test
	public void withPublicMemberClasses() {
		ClassMetadata metadata = getClassMetadataFor(L0_b.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses, equalTo(new String[]{L0_b.L1.class.getName()}));
	}

	public static class L0_b {
		public static class L1 { }
	}


	@Test
	public void withNonPublicMemberClasses() {
		ClassMetadata metadata = getClassMetadataFor(L0_c.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses, equalTo(new String[]{L0_c.L1.class.getName()}));
	}

	public static class L0_c {
		private static class L1 { }
	}


	@Test
	public void againstMemberClass() {
		ClassMetadata metadata = getClassMetadataFor(L0_b.L1.class);
		String[] nestedClasses = metadata.getMemberClassNames();
		assertThat(nestedClasses, equalTo(new String[]{}));
	}
}
