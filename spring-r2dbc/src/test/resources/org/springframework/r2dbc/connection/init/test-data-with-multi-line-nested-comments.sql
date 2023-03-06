/* This is a multi line comment
 * The next comment line has no text

 * The next comment line starts with a space.
 *  x, y, z...
 */

-- This is a single line comment containing single (') and double quotes (").
INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller');
-- This is also a comment.
/*-------------------------------------------
-- A fancy multi-line comment that puts
-- single line comments inside of a multi-line
-- comment block.
Moreover, the block comment end delimiter
appears on a line that can potentially also
be a single-line comment if we weren't
already inside a multi-line comment run.

And here's a line containing single and double quotes (").
-------------------------------------------*/
 INSERT INTO
users(first_name, last_name)    -- This is a single line comment containing the block-end-comment sequence here */ but it's still a single-line comment
VALUES( 'Sam'     -- first_name
      , 'Brannen' -- last_name
);--