/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.metrics.ApplicationStartup;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the {@link ApplicationStartup} that it runs with.
 *
 * @author Brian Clozel
 * @since 5.3
 * @see ApplicationContextAware
 */
public interface ApplicationStartupAware extends Aware {

	/**
	 * Set the ApplicationStartup that this object runs with.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked before ApplicationContextAware's setApplicationContext.
	 * @param applicationStartup application startup to be used by this object
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

}
