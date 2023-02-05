package org.springframework.debug.spring.nio;

import java.io.IOException;

/**
 * @Author: zhoudong
 * @Description:
 * @Date: 2022/8/23 14:46
 * @Version:
 **/
public class TestZeroCopy {
	/**
	 *    上面程序的首先经过编译之后，
	 *    在1.该类的class常量池中存放一些符号引用，
	 *    2.然后类加载之后，将class常量池中存放的符号引用转存到运行时常量池中，
	 *    3.然后经过验证，准备阶段之后，在堆中生成驻留字符串的实例对象（也就是上例中str1所指向的”abc”实例对象），
	 *    4.然后将这个对象的引用存到全局String Pool中，也就是StringTable中，
	 *    5.最后在解析阶段，要把运行时常量池中的符号引用替换成直接引用，那么就直接查询StringTable，
	 *    保证StringTable里的引用值与运行时常量池中的引用值一致，大概整个过程就是这样了。
	 *    (先存放类的符号引用（类名 描述符等）之类的东西然后类加载之后把这些东西存放在运行时常量池  然后在堆中生成实例
	 *    然后把对象的引用值存在字符串常量池中 然后解析阶段把，要把运行时常量池中的符号引用替换成直接引用)
	 *   回到上面的那个程序，现在就很容易解释整个程序的内存分配过程了，
	 *   首先，在堆中会有一个”abc”实例，全局StringTable中存放着”abc”的一个引用值，
	 *   然后在运行第二句的时候会生成两个实例，一个是”def”的实例对象，
	 *   并且StringTable中存储一个”def”的引用值，还有一个是new出来的一个”def”的实例对象，
	 *   与上面那个是不同的实例，当在解析str3的时候查找StringTable，里面有”abc”的全局驻留字符串引用，
	 *   所以str3的引用地址与之前的那个已存在的相同，str4是在运行的时候调用intern()函数，
	 *   返回StringTable中”def”的引用值，如果没有就将str2的引用值添加进去，
	 *   在这里，StringTable中已经有了”def”的引用值了，所以返回上面在new str2的时候添加到StringTable中的 “def”引用值，
	 *   最后str5在解析的时候就也是指向存在于StringTable中的”def”的引用值，那么这样一分析之后，下面三个打印的值就容易理解了。
	 * @param args
	 * @throws IOException
	 */

	public static void main(String[] args) throws IOException {
		String str1 = "abc";
		String str2 = new String("def");
		String str3 = "abc";
		String str4 = str2.intern();
		String str5 = "def";
		//true System.out.println(str2 == str4);//false System.out.println(str4 == str5);//true
		System.out.println(str1 == str3);

	}
}
