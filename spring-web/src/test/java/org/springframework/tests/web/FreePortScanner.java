/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.tests.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Random;

import org.springframework.util.Assert;

/**
 * Utility class that finds free BSD ports for use in testing scenario's.
 *
 * @author Ben Hale
 * @author Arjen Poutsma
 */
public abstract class FreePortScanner {

	private static final int MIN_SAFE_PORT = 1024;

	private static final int MAX_PORT = 65535;

	private static final Random random = new Random();

	/**
	 * Returns the number of a free port in the default range.
	 */
	public static int getFreePort() {
		return getFreePort(MIN_SAFE_PORT, MAX_PORT);
	}

	/**
	 * Returns the number of a free port in the given range.
	 */
	public static int getFreePort(int minPort, int maxPort) {
		Assert.isTrue(minPort > 0, "'minPort' must be larger than 0");
		Assert.isTrue(maxPort > minPort, "'maxPort' must be larger than minPort");
		int portRange = maxPort - minPort;
		int candidatePort;
		int searchCounter = 0;
		do {
			if (++searchCounter > portRange) {
				throw new IllegalStateException(
						String.format("There were no ports available in the range %d to %d", minPort, maxPort));
			}
			candidatePort = getRandomPort(minPort, portRange);
		}
		while (!isPortAvailable(candidatePort));

		return candidatePort;
	}

	private static int getRandomPort(int minPort, int portRange) {
		return minPort + random.nextInt(portRange);
	}

	private static boolean isPortAvailable(int port) {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to create ServerSocket.", ex);
		}

		try {
			InetSocketAddress sa = new InetSocketAddress(port);
			serverSocket.bind(sa);
			return true;
		}
		catch (IOException ex) {
			return false;
		}
		finally {
			try {
				serverSocket.close();
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

}
