/**
 * Support for handling messages to "user" destinations (i.e. destinations that are
 * unique to a user's sessions), primarily translating the destinations and then
 * forwarding the updated message to the broker.
 *
 * <p>Also included is {@link org.springframework.messaging.simp.user.SimpUserRegistry}
 * for keeping track of connected user sessions.
 */
@NonNullApi
@NonNullFields
package org.springframework.messaging.simp.user;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
