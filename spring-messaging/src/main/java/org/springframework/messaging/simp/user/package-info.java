/**
 * Support for handling messages to "user" destinations (i.e. destinations that are
 * unique to a user's sessions), primarily translating the destinations and then
 * forwarding the updated message to the broker.
 * <p>
 * Also included is {@link org.springframework.messaging.simp.user.UserSessionRegistry}
 * for keeping track of connected user sessions.
 */
package org.springframework.messaging.simp.user;
