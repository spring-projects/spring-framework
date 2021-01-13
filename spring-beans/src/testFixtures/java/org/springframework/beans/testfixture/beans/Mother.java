package org.springframework.beans.testfixture.beans;

/**
 * @author jinxiong
 */
public class Mother {

	public Mother() {

		// Simulation takes a long time、just for testing
		try {
			Thread.sleep(4 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}
