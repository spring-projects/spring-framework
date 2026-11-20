/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

/**
 * Interface supplying the information necessary to describe an introduction.
 *
 * <p>{@link IntroductionAdvisor IntroductionAdvisors} must implement this
 * interface. If an {@link org.aopalliance.aop.Advice} implements this,
 * it may be used as an introduction without an {@link IntroductionAdvisor}.
 * In this case, the advice is self-describing, providing not only the
 * necessary behavior, but describing the interfaces it introduces.
 *
 * @author Rod Johnson
 * @since 1.1.1
 */
public interface IntroductionInfo {

	/**
	 * Return the additional interfaces introduced by this Advisor or Advice.
	 * @return the introduced interfaces
	 */
	Class<?>[] getInterfaces();

}
