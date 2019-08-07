/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.util.SocketUtils.PORT_RANGE_MIN;
import static org.springframework.util.SocketUtils.PORT_RANGE_MAX;

/**
 * Unit tests for {@link SocketUtils}.
 *
 * @author Sam Brannen
 * @author Gary Russell
 */
public class SocketUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	// TCP

	@Test
	public void findAvailableTcpPortWithZeroMinPort() {
		exception.expect(IllegalArgumentException.class);
		SocketUtils.findAvailableTcpPort(0);
	}

	@Test
	public void findAvailableTcpPortWithNegativeMinPort() {
		exception.expect(IllegalArgumentException.class);
		SocketUtils.findAvailableTcpPort(-500);
	}

	@Test
	public void findAvailableTcpPort() {
		int port = SocketUtils.findAvailableTcpPort();
		assertPortInRange(port, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	@Test
	public void findAvailableTcpPortWithMinPortEqualToMaxPort() {
		int minMaxPort = SocketUtils.findAvailableTcpPort();
		int port = SocketUtils.findAvailableTcpPort(minMaxPort, minMaxPort);
		assertEquals(minMaxPort, port);
	}

	@Test
	public void findAvailableTcpPortWhenPortOnLoopbackInterfaceIsNotAvailable() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		ServerSocket socket = ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"));
		try {
			exception.expect(IllegalStateException.class);
			exception.expectMessage(startsWith("Could not find an available TCP port"));
			exception.expectMessage(endsWith("after 1 attempts"));
			// will only look for the exact port
			SocketUtils.findAvailableTcpPort(port, port);
		}
		finally {
			socket.close();
		}
	}

	@Test
	public void findAvailableTcpPortWithMin() {
		int port = SocketUtils.findAvailableTcpPort(50000);
		assertPortInRange(port, 50000, PORT_RANGE_MAX);
	}

	@Test
	public void findAvailableTcpPortInRange() {
		int minPort = 20000;
		int maxPort = minPort + 1000;
		int port = SocketUtils.findAvailableTcpPort(minPort, maxPort);
		assertPortInRange(port, minPort, maxPort);
	}

	@Test
	public void find4AvailableTcpPorts() {
		findAvailableTcpPorts(4);
	}

	@Test
	public void find50AvailableTcpPorts() {
		findAvailableTcpPorts(50);
	}

	@Test
	public void find4AvailableTcpPortsInRange() {
		findAvailableTcpPorts(4, 30000, 35000);
	}

	@Test
	public void find50AvailableTcpPortsInRange() {
		findAvailableTcpPorts(50, 40000, 45000);
	}

	@Test
	public void findAvailableTcpPortsWithRequestedNumberGreaterThanSizeOfRange() {
		exception.expect(IllegalArgumentException.class);
		findAvailableTcpPorts(50, 45000, 45010);
	}


	// UDP

	@Test
	public void findAvailableUdpPortWithZeroMinPort() {
		exception.expect(IllegalArgumentException.class);
		SocketUtils.findAvailableUdpPort(0);
	}

	@Test
	public void findAvailableUdpPortWithNegativeMinPort() {
		exception.expect(IllegalArgumentException.class);
		SocketUtils.findAvailableUdpPort(-500);
	}

	@Test
	public void findAvailableUdpPort() {
		int port = SocketUtils.findAvailableUdpPort();
		assertPortInRange(port, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	@Test
	public void findAvailableUdpPortWhenPortOnLoopbackInterfaceIsNotAvailable() throws Exception {
		int port = SocketUtils.findAvailableUdpPort();
		DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
		try {
			exception.expect(IllegalStateException.class);
			exception.expectMessage(startsWith("Could not find an available UDP port"));
			exception.expectMessage(endsWith("after 1 attempts"));
			// will only look for the exact port
			SocketUtils.findAvailableUdpPort(port, port);
		}
		finally {
			socket.close();
		}
	}

	@Test
	public void findAvailableUdpPortWithMin() {
		int port = SocketUtils.findAvailableUdpPort(50000);
		assertPortInRange(port, 50000, PORT_RANGE_MAX);
	}

	@Test
	public void findAvailableUdpPortInRange() {
		int minPort = 20000;
		int maxPort = minPort + 1000;
		int port = SocketUtils.findAvailableUdpPort(minPort, maxPort);
		assertPortInRange(port, minPort, maxPort);
	}

	@Test
	public void find4AvailableUdpPorts() {
		findAvailableUdpPorts(4);
	}

	@Test
	public void find50AvailableUdpPorts() {
		findAvailableUdpPorts(50);
	}

	@Test
	public void find4AvailableUdpPortsInRange() {
		findAvailableUdpPorts(4, 30000, 35000);
	}

	@Test
	public void find50AvailableUdpPortsInRange() {
		findAvailableUdpPorts(50, 40000, 45000);
	}

	@Test
	public void findAvailableUdpPortsWithRequestedNumberGreaterThanSizeOfRange() {
		exception.expect(IllegalArgumentException.class);
		findAvailableUdpPorts(50, 45000, 45010);
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
		assertTrue("port [" + port + "] >= " + minPort, port >= minPort);
		assertTrue("port [" + port + "] <= " + maxPort, port <= maxPort);
	}

	private void assertAvailablePorts(SortedSet<Integer> ports, int numRequested, int minPort, int maxPort) {
		assertEquals("number of ports requested", numRequested, ports.size());
		for (int port : ports) {
			assertPortInRange(port, minPort, maxPort);
		}
	}

}
