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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

import javax.net.ServerSocketFactory;

import org.springframework.util.Assert;

/**
 * Removed from spring-core and introduced as an internal test utility in
 * spring-context in Spring Framework 6.0.
 *
 * <p>Simple utility methods for working with network sockets &mdash; for example,
 * for finding available ports on {@code localhost}.
 *
 * <p>Within this class, a TCP port refers to a port for a {@link ServerSocket};
 * whereas, a UDP port refers to a port for a {@link DatagramSocket}.
 *
 * <p>{@code SocketUtils} was introduced in Spring Framework 4.0, primarily to
 * assist in writing integration tests which start an external server on an
 * available random port. However, these utilities make no guarantee about the
 * subsequent availability of a given port and are therefore unreliable. Instead
 * of using {@code SocketUtils} to find an available local port for a server, it
 * is recommended that you rely on a server's ability to start on a random port
 * that it selects or is assigned by the operating system. To interact with that
 * server, you should query the server for the port it is currently using.
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 4.0
 */
public class TestSocketUtils {

	/**
	 * The default minimum value for port ranges used when finding an available
	 * socket port.
	 */
	private static final int PORT_RANGE_MIN = 1024;

	/**
	 * The default maximum value for port ranges used when finding an available
	 * socket port.
	 */
	private static final int PORT_RANGE_MAX = 65535;


	private static final Random random = new Random(System.nanoTime());


	/**
	 * Find an available TCP port randomly selected from the range
	 * [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
	 * @return an available TCP port number
	 * @throws IllegalStateException if no available port could be found
	 */
	public static int findAvailableTcpPort() {
		return findAvailablePort(PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * Find an available port for this {@code SocketType}, randomly selected
	 * from the range [{@code minPort}, {@code maxPort}].
	 * @param minPort the minimum port number
	 * @param maxPort the maximum port number
	 * @return an available port number for this socket type
	 * @throws IllegalStateException if no available port could be found
	 */
	private static int findAvailablePort(int minPort, int maxPort) {
		Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
		Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
		Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

		int portRange = maxPort - minPort;
		int candidatePort;
		int searchCounter = 0;
		do {
			if (searchCounter > portRange) {
				throw new IllegalStateException(String.format(
						"Could not find an available TCP port in the range [%d, %d] after %d attempts",
						minPort, maxPort, searchCounter));
			}
			candidatePort = findRandomPort(minPort, maxPort);
			searchCounter++;
		}
		while (!isPortAvailable(candidatePort));

		return candidatePort;
	}

	/**
	 * Find a pseudo-random port number within the range
	 * [{@code minPort}, {@code maxPort}].
	 * @param minPort the minimum port number
	 * @param maxPort the maximum port number
	 * @return a random port number within the specified range
	 */
	private static int findRandomPort(int minPort, int maxPort) {
		int portRange = maxPort - minPort;
		return minPort + random.nextInt(portRange + 1);
	}

	/**
	 * Determine if the specified port for this {@code SocketType} is
	 * currently available on {@code localhost}.
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
