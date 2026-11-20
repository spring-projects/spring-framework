/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.assertj;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to {@link MockHttpServletRequest}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <SELF> the type of assertions
 */
public abstract class AbstractMockHttpServletRequestAssert<SELF extends AbstractMockHttpServletRequestAssert<SELF>>
		extends AbstractHttpServletRequestAssert<SELF, MockHttpServletRequest> {

	protected AbstractMockHttpServletRequestAssert(MockHttpServletRequest request, Class<?> selfType) {
		super(request, selfType);
	}

}
