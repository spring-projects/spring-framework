/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.mixin;

import org.springframework.aop.support.DefaultIntroductionAdvisor;

/**
 * Advisor for use with a LockMixin. Applies to all classes.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class LockMixinAdvisor extends DefaultIntroductionAdvisor {

	public LockMixinAdvisor() {
		super(new LockMixin(), Lockable.class);
	}

}
