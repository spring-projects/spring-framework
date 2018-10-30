DROP ALIAS IF EXISTS REVERSE;

-- REVERSE function borrowed from http://www.h2database.com/html/grammar.html#create_alias
CREATE ALIAS REVERSE AS $$
	String reverse(String s) {
		return new StringBuilder(s).reverse().toString();
	}
$$;

INSERT INTO users(first_name, last_name) values('Sam', 'Brannen');
