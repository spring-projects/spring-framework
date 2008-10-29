/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.util;

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Unit tests for the {@link TagUtils} class.
 *
 * @author Alef Arendsen
 * @author Rick Evans
 */
public final class TagUtilsTests extends TestCase {

	public void testGetScopeSunnyDay() {
		assertEquals(TagUtils.SCOPE_PAGE, "page");
		assertEquals(TagUtils.SCOPE_APPLICATION, "application");
		assertEquals(TagUtils.SCOPE_SESSION, "session");
		assertEquals(TagUtils.SCOPE_REQUEST, "request");

		assertEquals(TagUtils.getScope("page"), PageContext.PAGE_SCOPE);
		assertEquals(TagUtils.getScope("request"), PageContext.REQUEST_SCOPE);
		assertEquals(TagUtils.getScope("session"), PageContext.SESSION_SCOPE);
		assertEquals(TagUtils.getScope("application"), PageContext.APPLICATION_SCOPE);

		// non-existent scope
		assertEquals("TagUtils.getScope(..) with a non-existent scope argument must " +
				"just return the default scope (PageContext.PAGE_SCOPE).",
				TagUtils.getScope("bla"), PageContext.PAGE_SCOPE);
	}

	public void testGetScopeWithNullScopeArgument() {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TagUtils.getScope(null);
			}
		}.runTest();
	}

	public void testHasAncestorOfTypeWhereAncestorTagIsNotATagType() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				assertFalse(TagUtils.hasAncestorOfType(
						new TagSupport(), String.class));
			}
		}.runTest();
	}

	public void testHasAncestorOfTypeWithNullTagArgument() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				assertFalse(TagUtils.hasAncestorOfType(null, TagSupport.class));
			}
		}.runTest();
	}

	public void testHasAncestorOfTypeWithNullAncestorTagClassArgument() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				assertFalse(TagUtils.hasAncestorOfType(new TagSupport(), null));
			}
		}.runTest();
	}

	public void testHasAncestorOfTypeTrueScenario() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag c = new TagC();

		a.setParent(b);
		b.setParent(c);

		assertTrue(TagUtils.hasAncestorOfType(a, TagC.class));
	}

	public void testHasAncestorOfTypeFalseScenario() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag anotherB = new TagB();

		a.setParent(b);
		b.setParent(anotherB);

		assertFalse(TagUtils.hasAncestorOfType(a, TagC.class));
	}

	public void testHasAncestorOfTypeWhenTagHasNoParent() throws Exception {
		assertFalse(TagUtils.hasAncestorOfType(new TagA(), TagC.class));
	}

	public void testAssertHasAncestorOfTypeWithNullTagName() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TagUtils.assertHasAncestorOfType(new TagA(), TagC.class, null, "c");
			}
		}.runTest();
	}

	public void testAssertHasAncestorOfTypeWithNullAncestorTagName() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TagUtils.assertHasAncestorOfType(new TagA(), TagC.class, "a", null);
			}
		}.runTest();
	}

	public void testAssertHasAncestorOfTypeThrowsExceptionOnFail() throws Exception {
		new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				Tag a = new TagA();
				Tag b = new TagB();
				Tag anotherB = new TagB();

				a.setParent(b);
				b.setParent(anotherB);

				TagUtils.assertHasAncestorOfType(a, TagC.class, "a", "c");
			}
		}.runTest();
	}

	public void testAssertHasAncestorOfTypeDoesNotThrowExceptionOnPass() throws Exception {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag c = new TagC();

		a.setParent(b);
		b.setParent(c);

		TagUtils.assertHasAncestorOfType(a, TagC.class, "a", "c");
	}


	private static final class TagA extends TagSupport {
	}

	private static final class TagB extends TagSupport {
	}

	private static final class TagC extends TagSupport {
	}

}
