package com.atlwj.demo.ioc.ext.extthird;

import java.util.Arrays;

public class Dog {
	// 主人
	private String hostMan;

	// 孩子
	private String[] sonDogs;

	// character 性格
	private String character;

	// 名字
	private String nickName;

	public String getHostMan() {
		return hostMan;
	}

	public void setHostMan(String hostMan) {
		this.hostMan = hostMan;
	}

	public String[] getSonDogs() {
		return sonDogs;
	}

	public void setSonDogs(String[] sonDogs) {
		this.sonDogs = sonDogs;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String character) {
		this.character = character;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	@Override
	public String toString() {
		return "Dog{" +
				"hostMan='" + hostMan + '\'' +
				", sonDogs=" + Arrays.toString(sonDogs) +
				", character='" + character + '\'' +
				", nickName='" + nickName + '\'' +
				'}';
	}
}
