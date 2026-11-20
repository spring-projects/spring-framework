/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.mailusage;

import org.springframework.docs.integration.mailusagesimple.Order;

// tag::snippet[]
public interface OrderManager {

	void placeOrder(Order order);
}
// end::snippet[]
