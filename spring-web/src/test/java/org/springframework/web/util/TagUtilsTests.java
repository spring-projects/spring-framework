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

package org.springframework.web.util;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link TagUtils} class.
 *
 * @author Alef Arendsen
 * @author Rick Evans
 */
public class TagUtilsTests {

	@Test
	public void getScopeSunnyDay() {
		assertEquals("page", TagUtils.SCOPE_PAGE);
		assertEquals("application", TagUtils.SCOPE_APPLICATION);
		assertEquals("session", TagUtils.SCOPE_SESSION);
		assertEquals("request", TagUtils.SCOPE_REQUEST);

		assertEquals(PageContext.PAGE_SCOPE, TagUtils.getScope("page"));
		assertEquals(PageContext.REQUEST_SCOPE, TagUtils.getScope("request"));
		assertEquals(PageContext.SESSION_SCOPE, TagUtils.getScope("session"));
		assertEquals(PageContext.APPLICATION_SCOPE, TagUtils.getScope("application"));

		// non-existent scope
		assertEquals("TagUtils.getScope(..) with a non-existent scope argument must " +
				"just return the default scope (PageContext.PAGE_SCOPE).", PageContext.PAGE_SCOPE,
				TagUtils.getScope("bla"));
	}

	@Test
	public void getScopeWithNullScopeArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.getScope(null));
	}

	@Test
	public void hasAncestorOfTypeWhereAncestorTagIsNotATagType() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.hasAncestorOfType(new TagSupport(), String.class));
	}

	@Test
	public void hasAncestorOfTypeWithNullTagArgument() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.hasAncestorOfType(null, TagSupport.class));
	}

	@Test
	public void hasAncestorOfTypeWithNullAncestorTagClassArgument() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.hasAncestorOfType(new TagSupport(), null));
	}

	@Test
	public void hasAncestorOfTypeTrueScenario() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag c = new TagC();

		a.setParent(b);
		b.setParent(c);

		assertTrue(TagUtils.hasAncestorOfType(a, TagC.class));
	}

	@Test
	public void hasAncestorOfTypeFalseScenario() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag anotherB = new TagB();

		a.setParent(b);
		b.setParent(anotherB);

		assertFalse(TagUtils.hasAncestorOfType(a, TagC.class));
	}

	@Test
	public void hasAncestorOfTypeWhenTagHasNoParent() throws Exception {
		assertFalse(TagUtils.hasAncestorOfType(new TagA(), TagC.class));
	}

	@Test
	public void assertHasAncestorOfTypeWithNullTagName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.assertHasAncestorOfType(new TagA(), TagC.class, null, "c"));
	}

	@Test
	public void assertHasAncestorOfTypeWithNullAncestorTagName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.assertHasAncestorOfType(new TagA(), TagC.class, "a", null));
	}

	@Test
	public void assertHasAncestorOfTypeThrowsExceptionOnFail() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag anotherB = new TagB();

		a.setParent(b);
		b.setParent(anotherB);

		assertThatIllegalStateException().isThrownBy(() ->
				TagUtils.assertHasAncestorOfType(a, TagC.class, "a", "c"));
	}

	@Test
	public void testAssertHasAncestorOfTypeDoesNotThrowExceptionOnPass() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag c = new TagC();

		a.setParent(b);
		b.setParent(c);

		TagUtils.assertHasAncestorOfType(a, TagC.class, "a", "c");
	}

	@SuppressWarnings("serial")
	private static final class TagA extends TagSupport {

	}

	@SuppressWarnings("serial")
	private static final class TagB extends TagSupport {

	}

	@SuppressWarnings("serial")
	private static final class TagC extends TagSupport {

	}

}
