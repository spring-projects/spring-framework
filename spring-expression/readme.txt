List of outstanding things to think about - turn into JIRAs once distilled to a core set of issues

High Importance

- In the resolver/executor model we cache executors.  They are currently recorded in the AST and so if the user chooses to evaluate an expression
in a different context then the stored executor may be incorrect.  It may harmless 'fail' which would cause us to retrieve a new one, but 
can it do anything malicious? In which case we either need to forget them when the context changes or store them elsewhere.  Should caching be
something that can be switched on/off by the context? (shouldCacheExecutors() on the interface?)
- Expression serialization needs supporting
- expression basic interface and common package.  Should LiteralExpression be settable? should getExpressionString return quoted value?

Low Importance

- For the ternary operator, should isWritable() return true/false depending on evaluating the condition and check isWritable() of whichever branch it
would have taken?  At the moment ternary expressions are just considered NOT writable.
- Enhance type locator interface with direct support for register/unregister imports and ability to set class loader?
- Should some of the common errors (like SpelMessages.TYPE_NOT_FOUND) be promoted to top level exceptions?
- Expression comparison - is it necessary?

Syntax

- should the 'is' operator change to 'instanceof' ?
- in this expression we hit the problem of not being able to write chars, since '' always means string:
  evaluate("new java.lang.String('hello').charAt(2).equals('l'.charAt(0))", true, Boolean.class);
  So 'l'.charAt(0) was required - wonder if we can build in a converter for a single length string to char?
  Can't do that as equals take Object and so we don't know to do a cast in order to pass a char into equals
  We certainly cannot do a cast (unless casts are added to the syntax).  See MethodInvocationTest.testStringClass()
- MATCHES is now the thing that takes a java regex.  What does 'like' do? right now it is the SQL LIKE that supports
  wildcards % and _.  It has a poor implementation but I need to know whether to keep it in the language before
  fixing that.
- Need to agree on a standard date format for 'default' processing of dates.  Currently it is:
  formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.UK);
  // this is something of this format: "Wed, 4 Jul 2001 12:08:56 GMT"
  // http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html
- See LiteralTests for Date (4,5,6) - should date take an expression rather than be hardcoded in the grammar
  to take 2 strings only?
- when doing arithmetic, eg. 8.4 / 4  and the user asks for an Integer return type - do we silently coerce or
  say we cannot as it won't fit into an int? (see OperatorTests.testMathOperatorDivide04)
- Is $index within projection/selection useful or just cute?
- All reals are represented as Doubles (so 1.25f is held internally as a double, can be converted to float when required though) - is that ok?