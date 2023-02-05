package org.springframework.debug.spring.nio;

/**
 * @Author: zhoudong
 * @Description:
 * @Date: 2022/9/9 10:41
 * @Version:
 **/
public class Test2 {

	public static void main(String[] args) {
		double v = calculate_it();
//		System.out.println(v);
		System.out.println(Math.sqrt(0));
	}

	// 0.7854738571428571
	public static double calculate_it(){
		double x =0.0d;
		double y =0.0d;
		int total =0;
		for (int i =0;i<7000000;i++){
			x=Math.random();
			y=Math.random();
			if(Math.sqrt(x*x+y*y)<1){
				total++;
			}

		}
		return total/7000000.0;
	}
}
