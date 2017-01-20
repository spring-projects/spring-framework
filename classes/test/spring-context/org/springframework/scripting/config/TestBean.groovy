package org.springframework.scripting.config

class TestBean implements ITestBean {

	ITestBean otherBean

	boolean initialized

	boolean destroyed

	void setOtherBean(ITestBean otherBean) {
		this.otherBean = otherBean;
	}

	void startup() { this.initialized = true }

	void shutdown() { this.destroyed = true }

}