/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import jakarta.servlet.jsp.tagext.TagSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for the {@link TagUtils} class.
 *
 * @author Alef Arendsen
 * @author Rick Evans
 */
class TagUtilsTests {

	@Test
	void getScopeSunnyDay() {
		assertThat(TagUtils.SCOPE_PAGE).isEqualTo("page");
		assertThat(TagUtils.SCOPE_APPLICATION).isEqualTo("application");
		assertThat(TagUtils.SCOPE_SESSION).isEqualTo("session");
		assertThat(TagUtils.SCOPE_REQUEST).isEqualTo("request");

		assertThat(TagUtils.getScope("page")).isEqualTo(PageContext.PAGE_SCOPE);
		assertThat(TagUtils.getScope("request")).isEqualTo(PageContext.REQUEST_SCOPE);
		assertThat(TagUtils.getScope("session")).isEqualTo(PageContext.SESSION_SCOPE);
		assertThat(TagUtils.getScope("application")).isEqualTo(PageContext.APPLICATION_SCOPE);

		// non-existent scope
		assertThat(TagUtils.getScope("bla")).as("TagUtils.getScope(..) with a non-existent scope argument must " +
				"just return the default scope (PageContext.PAGE_SCOPE).").isEqualTo(PageContext.PAGE_SCOPE);
	}

	@Test
	void getScopeWithNullScopeArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.getScope(null));
	}

	@Test
	void hasAncestorOfTypeWhereAncestorTagIsNotATagType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.hasAncestorOfType(new TagSupport(), String.class));
	}

	@Test
	void hasAncestorOfTypeWithNullTagArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.hasAncestorOfType(null, TagSupport.class));
	}

	@Test
	void hasAncestorOfTypeWithNullAncestorTagClassArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.hasAncestorOfType(new TagSupport(), null));
	}

	@Test
	void hasAncestorOfTypeTrueScenario() {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag c = new TagC();

		a.setParent(b);
		b.setParent(c);

		assertThat(TagUtils.hasAncestorOfType(a, TagC.class)).isTrue();
	}

	@Test
	void hasAncestorOfTypeFalseScenario() {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag anotherB = new TagB();

		a.setParent(b);
		b.setParent(anotherB);

		assertThat(TagUtils.hasAncestorOfType(a, TagC.class)).isFalse();
	}

	@Test
	void hasAncestorOfTypeWhenTagHasNoParent() {
		assertThat(TagUtils.hasAncestorOfType(new TagA(), TagC.class)).isFalse();
	}

	@Test
	void assertHasAncestorOfTypeWithNullTagName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.assertHasAncestorOfType(new TagA(), TagC.class, null, "c"));
	}

	@Test
	void assertHasAncestorOfTypeWithNullAncestorTagName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TagUtils.assertHasAncestorOfType(new TagA(), TagC.class, "a", null));
	}

	@Test
	void assertHasAncestorOfTypeThrowsExceptionOnFail() {
		Tag a = new TagA();
		Tag b = new TagB();
		Tag anotherB = new TagB();

		a.setParent(b);
		b.setParent(anotherB);

		assertThatIllegalStateException().isThrownBy(() ->
				TagUtils.assertHasAncestorOfType(a, TagC.class, "a", "c"));
	}

	@Test
	void assertHasAncestorOfTypeDoesNotThrowExceptionOnPass() {
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
