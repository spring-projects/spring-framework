package cn.dongliwei.springstudy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hello world!
 */
public class App {
	public static void main(String[] args) {

		System.out.println("Hello World!");

		ReentrantLock lock = new ReentrantLock();

		try {



				long timeStart = System.currentTimeMillis();

				lock.tryLock();
				int count=1;
				boolean mark=true;
			     while (lock.isLocked()){
				   System.out.println("循环次数="+count);
				   Thread.sleep(3000);
				   long timeEnd = System.currentTimeMillis();
				   long time = timeEnd - timeStart;

				    System.out.println("时间间隔=" + time);
				    if(count==2) {
						lock.unlock();
					}
				    count++;
			    }




		} catch (Exception e) {

                System.out.println("异常："+e);
                lock.unlock();
		}


	}
}
