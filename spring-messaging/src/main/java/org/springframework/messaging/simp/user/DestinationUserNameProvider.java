package org.springframework.messaging.simp.user;

/**
 * An interface to be implemented in addition to {@link java.security.Principal}
 * when {@link java.security.Principal#getName()} is not globally unique enough
 * for use in user destinations. For more on user destination see
 * {@link org.springframework.messaging.simp.user.UserDestinationResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
public interface DestinationUserNameProvider {


	/**
	 * Return the (globally unique) user name to use with user destinations.
	 */
	String getDestinationUserName();

}
