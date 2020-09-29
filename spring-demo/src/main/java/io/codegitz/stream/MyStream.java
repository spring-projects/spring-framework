package io.codegitz.stream;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 张观权
 * @date 2020/8/12 13:17
 **/
public class MyStream {
	public static void main(String[] args) {
		Stream.of("one", "two", "three","four").filter(e -> e.length() > 4)
				.peek(e -> System.out.println("Filtered value: " + e))
				.map(String::toUpperCase)
				.peek(e -> System.out.println("Mapped value: " + e))
				.collect(Collectors.toList());

	}
}
