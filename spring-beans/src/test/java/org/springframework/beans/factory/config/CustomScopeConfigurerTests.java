/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CustomScopeConfigurer}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class CustomScopeConfigurerTests {

	private static final String FOO_SCOPE = "fooScope";

	private final ConfigurableListableBeanFactory factory = new DefaultListableBeanFactory();


	@Test
	void withNoScopes() {
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.postProcessBeanFactory(factory);
	}

	@Test
	void sunnyDayWithBonaFideScopeInstance() {
		Scope scope = mock();
		factory.registerScope(FOO_SCOPE, scope);
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(FOO_SCOPE, scope);
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		figurer.postProcessBeanFactory(factory);
	}

	@Test
	void sunnyDayWithBonaFideScopeClass() {
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(FOO_SCOPE, NoOpScope.class);
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		figurer.postProcessBeanFactory(factory);
		assertThat(factory.getRegisteredScope(FOO_SCOPE)).isInstanceOf(NoOpScope.class);
	}

	@Test
	void sunnyDayWithBonaFideScopeClassName() {
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(FOO_SCOPE, NoOpScope.class.getName());
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		figurer.postProcessBeanFactory(factory);
		assertThat(factory.getRegisteredScope(FOO_SCOPE)).isInstanceOf(NoOpScope.class);
	}

	@Test
	void whereScopeMapHasNullScopeValueInEntrySet() {
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(FOO_SCOPE, null);
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		assertThatIllegalArgumentException().isThrownBy(() ->
				figurer.postProcessBeanFactory(factory));
	}

	@Test
	void whereScopeMapHasNonScopeInstanceInEntrySet() {
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(FOO_SCOPE, this);  // <-- not a valid value...
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		assertThatIllegalArgumentException().isThrownBy(() ->
				figurer.postProcessBeanFactory(factory));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void whereScopeMapHasNonStringTypedScopeNameInKeySet() {
		Map scopes = new HashMap();
		scopes.put(this, new NoOpScope());  // <-- not a valid value (the key)...
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
				figurer.postProcessBeanFactory(factory));
	}

}
