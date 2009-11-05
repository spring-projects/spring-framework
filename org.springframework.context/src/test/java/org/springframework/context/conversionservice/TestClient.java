package org.springframework.context.conversionservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class TestClient {

	private List<Bar> bars;

	private boolean bool;

	public List<Bar> getBars() {
		return bars;
	}

	@Autowired
	public void setBars(List<Bar> bars) {
		this.bars = bars;
	}

	public boolean isBool() {
		return bool;
	}

	public void setBool(boolean bool) {
		this.bool = bool;
	}

}
