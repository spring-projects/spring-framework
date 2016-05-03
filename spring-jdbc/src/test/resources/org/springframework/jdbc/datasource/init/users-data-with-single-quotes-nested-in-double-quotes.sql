INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller');

-- The following is not actually used; we just want to ensure that it does not
-- result in a parsing exception due to the nested single quote.
SELECT last_name AS "Juergen's Last Name" FROM users WHERE last_name='Hoeller';

INSERT INTO users(first_name, last_name) values('Sam', 'Brannen');