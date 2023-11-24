/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.testfixture.net;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

import javax.net.ServerSocketFactory;

/**
 * Simple utility for finding available TCP ports on {@code localhost} for use in
 * integration testing scenarios.
 *
 * <p>{@code SocketUtils} was removed from the public API in {@code spring-core}
 * in Spring Framework 6.0 and reintroduced as {@code TestSocketUtils}, which is
 * made available to all tests in Spring Framework's test suite as a Gradle
 * <em>test fixture</em>.
 *
 * <p>{@code SocketUtils} was introduced in Spring Framework 4.0, primarily to
 * assist in writing integration tests which start an external server on an
 * available random port. However, these utilities make no guarantee about the
 * subsequent availability of a given port and are therefore unreliable. Instead
 * of using {@code TestSocketUtils} to find an available local port for a server,
 * it is recommended that you rely on a server's ability to start on a random port
 * that it selects or is assigned by the operating system. To interact with that
 * server, you should query the server for the port it is currently using.
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 6.0
 */
public abstract class TestSocketUtils {

	/**
	 * The minimum value for port ranges used when finding an available TCP port.
	 */
	private static final int PORT_RANGE_MIN = 1024;

	/**
	 * The maximum value for port ranges used when finding an available TCP port.
	 */
	private static final int PORT_RANGE_MAX = 65535;

	private static final int PORT_RANGE = PORT_RANGE_MAX - PORT_RANGE_MIN;

	private static final int MAX_ATTEMPTS = 1_000;

	private static final Random random = new Random(System.nanoTime());


	/**
	 * Find an available TCP port randomly selected from the range [1024, 65535].
	 * @return an available TCP port number
	 * @throws IllegalStateException if no available port could be found
	 */
	public static int findAvailableTcpPort() {
		int candidatePort;
		int searchCounter = 0;
		do {
			if (searchCounter > MAX_ATTEMPTS) {
				throw new IllegalStateException(String.format(
						"Could not find an available TCP port in the range [%d, %d] after %d attempts",
						PORT_RANGE_MIN, PORT_RANGE_MAX, MAX_ATTEMPTS));
			}
			candidatePort = PORT_RANGE_MIN + random.nextInt(PORT_RANGE + 1);
			searchCounter++;
		}
		while (!isPortAvailable(candidatePort));

		return candidatePort;
	}

	/**
	 * Determine if the specified TCP port is currently available on {@code localhost}.
	 */
	private static boolean isPortAvailable(int port) {
		try {
			ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
					port, 1, InetAddress.getByName("localhost"));
			serverSocket.close();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
