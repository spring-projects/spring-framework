package com.atlwj.demo.ioc.javaconfig.domain;

public class User {
	private String username;
	private String character;
	private Son son;

	public User(String username, String character ,Son son) {
		this.username = username;
		this.character = character;
		this.son = son;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String character) {
		this.character = character;
	}

	public Son getSon() {
		return son;
	}

	public void setSon(Son son) {
		this.son = son;
	}

	@Override
	public String toString() {
		return "User{" +
				"username='" + username + '\'' +
				", character='" + character + '\'' +
				", son=" + son +
				'}';
	}
}
