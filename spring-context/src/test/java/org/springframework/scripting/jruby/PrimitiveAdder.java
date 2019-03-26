package org.springframework.scripting.jruby;

/**
 * https://opensource.atlassian.com/projects/spring/browse/SPR-3026
 *
 * @author Rick Evans
 */
public interface PrimitiveAdder {

	int addInts(int x, int y);

	short addShorts(short x, short y);

	long addLongs(long x, long y);

	float addFloats(float x, float y);

	double addDoubles(double x, double y);

	boolean resultIsPositive(int x, int y);

	String concatenate(char c, char d);

	char echo(char c);
}
