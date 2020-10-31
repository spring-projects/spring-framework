/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.util;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.SortedSet;

import javax.net.ServerSocketFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.util.SocketUtils.PORT_RANGE_MAX;
import static org.springframework.util.SocketUtils.PORT_RANGE_MIN;

/**
 * Unit tests for {@link SocketUtils}.
 *
 * @author Sam Brannen
 * @author Gary Russell
 */
class SocketUtilsTests {

	@Test
	void canBeInstantiated() {
		// Just making sure somebody doesn't try to make SocketUtils abstract,
		// since that would be a breaking change due to the intentional public
		// constructor.
		new SocketUtils();
	}

	// TCP

	@Test
	void findAvailableTcpPortWithZeroMinPort() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SocketUtils.findAvailableTcpPort(0));
	}

	@Test
	void findAvailableTcpPortWithNegativeMinPort() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SocketUtils.findAvailableTcpPort(-500));
	}

	@Test
	void findAvailableTcpPort() {
		int port = SocketUtils.findAvailableTcpPort();
		assertPortInRange(port, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	@Test
	void findAvailableTcpPortWithMinPortEqualToMaxPort() {
		int minMaxPort = SocketUtils.findAvailableTcpPort();
		int port = SocketUtils.findAvailableTcpPort(minMaxPort, minMaxPort);
		assertThat(port).isEqualTo(minMaxPort);
	}

	@Test
	void findAvailableTcpPortWhenPortOnLoopbackInterfaceIsNotAvailable() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		try (ServerSocket socket = ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"))) {
			assertThat(socket).isNotNull();
			// will only look for the exact port
			assertThatIllegalStateException().isThrownBy(() ->
					SocketUtils.findAvailableTcpPort(port, port))
				.withMessageStartingWith("Could not find an available TCP port")
				.withMessageEndingWith("after 1 attempts");
		}
	}

	@Test
	void findAvailableTcpPortWithMin() {
		int port = SocketUtils.findAvailableTcpPort(50000);
		assertPortInRange(port, 50000, PORT_RANGE_MAX);
	}

	@Test
	void findAvailableTcpPortInRange() {
		int minPort = 20000;
		int maxPort = minPort + 1000;
		int port = SocketUtils.findAvailableTcpPort(minPort, maxPort);
		assertPortInRange(port, minPort, maxPort);
	}

	@Test
	void find4AvailableTcpPorts() {
		findAvailableTcpPorts(4);
	}

	@Test
	void find50AvailableTcpPorts() {
		findAvailableTcpPorts(50);
	}

	@Test
	void find4AvailableTcpPortsInRange() {
		findAvailableTcpPorts(4, 30000, 35000);
	}

	@Test
	void find50AvailableTcpPortsInRange() {
		findAvailableTcpPorts(50, 40000, 45000);
	}

	@Test
	void findAvailableTcpPortsWithRequestedNumberGreaterThanSizeOfRange() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				findAvailableTcpPorts(50, 45000, 45010));
	}


	// UDP

	@Test
	void findAvailableUdpPortWithZeroMinPort() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SocketUtils.findAvailableUdpPort(0));
	}

	@Test
	void findAvailableUdpPortWithNegativeMinPort() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SocketUtils.findAvailableUdpPort(-500));
	}

	@Test
	void findAvailableUdpPort() {
		int port = SocketUtils.findAvailableUdpPort();
		assertPortInRange(port, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	@Test
	void findAvailableUdpPortWhenPortOnLoopbackInterfaceIsNotAvailable() throws Exception {
		int port = SocketUtils.findAvailableUdpPort();
		try (DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"))) {
			assertThat(socket).isNotNull();
			// will only look for the exact port
			assertThatIllegalStateException().isThrownBy(() ->
					SocketUtils.findAvailableUdpPort(port, port))
				.withMessageStartingWith("Could not find an available UDP port")
				.withMessageEndingWith("after 1 attempts");
		}
	}

	@Test
	void findAvailableUdpPortWithMin() {
		int port = SocketUtils.findAvailableUdpPort(50000);
		assertPortInRange(port, 50000, PORT_RANGE_MAX);
	}

	@Test
	void findAvailableUdpPortInRange() {
		int minPort = 20000;
		int maxPort = minPort + 1000;
		int port = SocketUtils.findAvailableUdpPort(minPort, maxPort);
		assertPortInRange(port, minPort, maxPort);
	}

	@Test
	void find4AvailableUdpPorts() {
		findAvailableUdpPorts(4);
	}

	@Test
	void find50AvailableUdpPorts() {
		findAvailableUdpPorts(50);
	}

	@Test
	void find4AvailableUdpPortsInRange() {
		findAvailableUdpPorts(4, 30000, 35000);
	}

	@Test
	void find50AvailableUdpPortsInRange() {
		findAvailableUdpPorts(50, 40000, 45000);
	}

	@Test
	void findAvailableUdpPortsWithRequestedNumberGreaterThanSizeOfRange() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				findAvailableUdpPorts(50, 45000, 45010));
	}


	// Helpers

	private void findAvailableTcpPorts(int numRequested) {
		SortedSet<Integer> ports = SocketUtils.findAvailableTcpPorts(numRequested);
		assertAvailablePorts(ports, numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	private void findAvailableTcpPorts(int numRequested, int minPort, int maxPort) {
		SortedSet<Integer> ports = SocketUtils.findAvailableTcpPorts(numRequested, minPort, maxPort);
		assertAvailablePorts(ports, numRequested, minPort, maxPort);
	}

	private void findAvailableUdpPorts(int numRequested) {
		SortedSet<Integer> ports = SocketUtils.findAvailableUdpPorts(numRequested);
		assertAvailablePorts(ports, numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	private void findAvailableUdpPorts(int numRequested, int minPort, int maxPort) {
		SortedSet<Integer> ports = SocketUtils.findAvailableUdpPorts(numRequested, minPort, maxPort);
		assertAvailablePorts(ports, numRequested, minPort, maxPort);
	}
	private void assertPortInRange(int port, int minPort, int maxPort) {
		assertThat(port >= minPort).as("port [" + port + "] >= " + minPort).isTrue();
		assertThat(port <= maxPort).as("port [" + port + "] <= " + maxPort).isTrue();
	}

	private void assertAvailablePorts(SortedSet<Integer> ports, int numRequested, int minPort, int maxPort) {
		assertThat(ports.size()).as("number of ports requested").isEqualTo(numRequested);
		for (int port : ports) {
			assertPortInRange(port, minPort, maxPort);
		}
	}

}
