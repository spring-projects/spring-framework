/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.env.Environment;

/**
 * Interface to be implemented by any bean that wishes to be notified
 * of the {@link Environment} that it runs in.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.core.env.EnvironmentCapable
 */
public interface EnvironmentAware extends Aware {

	/**
	 * Set the {@code Environment} that this component runs in.
	 */
	void setEnvironment(Environment environment);

}
