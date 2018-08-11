package org.springframework.expression.spel.testresources;

public class TestPerson {
		private String name;
		private TestAddress address;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public TestAddress getAddress() {
			return address;
		}
		public void setAddress(TestAddress address) {
			this.address = address;
		}
	}
