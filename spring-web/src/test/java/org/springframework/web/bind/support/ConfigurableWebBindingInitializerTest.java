package org.springframework.web.bind.support;

import org.junit.jupiter.api.Test;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConfigurableWebBindingInitializer}.
 *
 * @author Michaël van de Giessen
 */
public class ConfigurableWebBindingInitializerTest {
	private static final String[] ALLOWED = new String[]{"allowed"};
	private static final String[] DISALLOWED =  new String[]{"disallowed"};
	private WebDataBinder binder = new WebDataBinder(new TestBean());

	@Test
	void initBinderAllowedSet() {
		// if we set allowed / disallowed
		ConfigurableWebBindingInitializer sut = new ConfigurableWebBindingInitializer();
		sut.setAllowedFields(ALLOWED);
		sut.setDisallowedFields(DISALLOWED);
		sut.initBinder(this.binder);

		// we expect them to be set on the initialising WebDataBinder
		assertThat(this.binder.getAllowedFields()).containsExactly(ALLOWED);
		assertThat(this.binder.getDisallowedFields()).containsExactly(DISALLOWED);
	}

	@Test
	void initBinderAllowedNullIgnore() {
		this.binder.setAllowedFields(ALLOWED);
		this.binder.setDisallowedFields(DISALLOWED);

		// if we dont set allowed / disallowed (null = default)
		ConfigurableWebBindingInitializer sut = new ConfigurableWebBindingInitializer();
		sut.setAllowedFields(null);
		sut.setDisallowedFields(null);
		sut.initBinder(this.binder);

		// we expect original values to be retained on the initialising WebDataBinder
		assertThat(this.binder.getAllowedFields()).containsExactly(ALLOWED);
		assertThat(this.binder.getDisallowedFields()).containsExactly(DISALLOWED);
	}
}
