package com.atlwj.demo.ioc.di;


import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.beans.ConstructorProperties;

public class SimpleMovieLister {

	/**
	 * the SimpleMovieLister has a dependency on a MovieFinder
	 */
	private MovieFinder movieFinder;

	/**
	 * a constructor so that the Spring container can inject a MovieFinder
	 * @param movieFinder
	 */
//	@ConstructorProperties({"movieFinder"})
//	public SimpleMovieLister(MovieFinder movieFinder) {
//		this.movieFinder = movieFinder;
//	}

	// business logic that actually uses the injected MovieFinder is omitted...
	public void say(){
		System.out.println("SimpleMovieLister....say...");
	}

	public void setMovieFinder(MovieFinder movieFinder) {
		this.movieFinder = movieFinder;
	}

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("di.xml");
		SimpleMovieLister simpleMovieLister = (SimpleMovieLister) ioc.getBean("simpleMovieLister");
		simpleMovieLister.say();
	}
}