/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

/**
 * A {@link java.security.Principal} can also implement this contract when
 * {@link java.security.Principal#getName() getName()} isn't globally unique
 * and therefore not suited for use with "user" destinations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.1
 * @see org.springframework.messaging.simp.user.UserDestinationResolver
 */
public interface DestinationUserNameProvider {

	/**
	 * Return a globally unique user name for use with "user" destinations.
	 */
	String getDestinationUserName();

}
