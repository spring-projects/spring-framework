/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

/**
 * @author Juergen Hoeller
 */
public interface ConfigurableMessenger extends Messenger {

	void setMessage(String message);

}
