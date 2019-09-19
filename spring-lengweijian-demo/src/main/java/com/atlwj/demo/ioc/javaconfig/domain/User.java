package com.atlwj.demo.ioc.javaconfig.domain;

public class User {
	private String username;
	private String character;

	public User(String username, String character ) {
		this.username = username;
		this.character = character;
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


	@Override
	public String toString() {
		return "User{" +
				"username='" + username + '\'' +
				", character='" + character + '\''+
				'}';
	}
}
