package com.atlwj.demo.ioc.javaconfig.domain;

public class Son {
	private String sonName;
	private String character;

	public Son(String sonName, String character) {
		this.sonName = sonName;
		this.character = character;
	}

	public String getSonName() {
		return sonName;
	}

	public void setSonName(String sonName) {
		this.sonName = sonName;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String character) {
		this.character = character;
	}

	@Override
	public String toString() {
		return "Son{" +
				"sonName='" + sonName + '\'' +
				", character='" + character + '\'' +
				'}';
	}
}
