package org.springframework.debug.spring.nio;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: zhoudong
 * @Description:
 * @Date: 2022/8/31 14:41
 * @Version:
 **/
public class Sort {

	public static void main(String[] args) {
		int sort[] = {23, 12, 11, 21, 20, 1, 2, 33, 66, 67};
		int temp;

		for (int i = 0; i < sort.length - 1; i++) {
			for (int j = 0; j < sort.length - i - 1; j++) {
				// 如果第一个数字比第二个数字小，就把第一个数字赋值给temp变量
				// 然后把第二个大数字赋值给第一个小数字，将temp最小的数字赋值给第二个数字，将最小的数字往最后面放
				if (sort[j] < sort[j + 1]) {
					temp = sort[j];
					sort[j] = sort[j + 1];
					sort[j +1] = temp;
				}
			}
		}
		for (int i = 0; i < sort.length - 1; i++) {
			//System.out.println(sort[i]);
		}

		Map map = new Hashtable();
		map.put(null, null);
		map.put(1, 2);
		map.put(3, 4);
		System.out.println(map);

		ExecutorService executorService = Executors.newFixedThreadPool(1);
	}
}
