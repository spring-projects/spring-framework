/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.annotation;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

class BridgeMethodAutowiringTests {

	@Test
	void SPR8434() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(UserServiceImpl.class, Foo.class);
		assertThat(ctx.getBean(UserServiceImpl.class).object).isNotNull();
		ctx.close();
	}


	abstract static class GenericServiceImpl<D> {

		public abstract void setObject(D object);
	}


	public static class UserServiceImpl extends GenericServiceImpl<Foo> {

		protected Foo object;

		@Override
		@Inject
		@Named("userObject")
		public void setObject(Foo object) {
			if (this.object != null) {
				throw new IllegalStateException("Already called");
			}
			this.object = object;
		}
	}


	@Component("userObject")
	public static class Foo {
	}

}
