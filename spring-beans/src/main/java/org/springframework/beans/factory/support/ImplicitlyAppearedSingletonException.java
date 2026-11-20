/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

/**
 * Internal exception to be propagated from {@link ConstructorResolver},
 * passed through to the initiating {@link DefaultSingletonBeanRegistry}
 * (without wrapping in a {@code BeanCreationException}).
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
class ImplicitlyAppearedSingletonException extends IllegalStateException {

	public ImplicitlyAppearedSingletonException() {
		super("About-to-be-created singleton instance implicitly appeared through the " +
				"creation of the factory bean that its bean definition points to");
	}

}
