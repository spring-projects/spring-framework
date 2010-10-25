package org.springframework.core.env;

import org.junit.Ignore;
import org.junit.Test;


/**
 * Unit tests covering the extensibility of AbstractEnvironment
 *
 * @author Chris Beams
 * @since 3.1
 */
public class CustomEnvironmentTests {

	@Ignore
	@Test
	public void noop() {
	}
	/*
	@Retention(RetentionPolicy.RUNTIME)
	public
	static @interface MyEnvironment { }

	/**
	 * A custom {@link Environment} that evaluates class literals
	 * for the presence of a custom annotation.
	 * /
	static class CustomEnvironment extends AbstractEnvironment {
		@Override
		public boolean accepts(Object object) {
			if (object instanceof Class<?>) {
				return ((Class<?>)object).isAnnotationPresent(MyEnvironment.class);
			}
			return super.accepts(object);
		}
	}

	@MyEnvironment
	static class CandidateWithCustomAnnotation { }

	static class CandidateWithoutCustomAnnotation { }

	@Test
	public void subclassOfAbstractEnvironment() {
		ConfigurableEnvironment env = new CustomEnvironment();
		env.setActiveProfiles("test");
		assertThat(env.accepts(CandidateWithCustomAnnotation.class), is(true));
		assertThat(env.accepts(CandidateWithoutCustomAnnotation.class), is(false));
		assertThat(env.accepts("test"), is(true)); // AbstractEnvironment always returns true
		assertThat(env.accepts(new Object()), is(true)); // AbstractEnvironment always returns true
	}

	static class CustomDefaultEnvironment extends DefaultEnvironment {
		@Override
		public boolean accepts(Object object) {
			if (object instanceof Class<?>) {
				return ((Class<?>)object).isAnnotationPresent(MyEnvironment.class);
			}
			return super.accepts(object);
		}
	}

	@Test
	public void subclassOfDefaultEnvironment() {
		ConfigurableEnvironment env = new CustomDefaultEnvironment();
		env.setActiveProfiles("test");
		assertThat(env.accepts(CandidateWithCustomAnnotation.class), is(true));
		assertThat(env.accepts(CandidateWithoutCustomAnnotation.class), is(false));
		assertThat(env.accepts("test"), is(true)); // delegates to DefaultEnvironment
		assertThat(env.accepts("bogus"), is(false)); // delegates to DefaultEnvironment
		assertThat(env.accepts(new Object()), is(false)); // delegates to DefaultEnvironment
	}
	*/

}
