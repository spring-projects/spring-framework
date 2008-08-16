List of outstanding things to think about - turn into JIRAs once distilled to a core set of issues

High Importance

- In the resolver/executor model we cache executors.  They are currently recorded in the AST and so if the user chooses to evaluate an expression
in a different context then the stored executor may be incorrect.  It may harmless 'fail' which would cause us to retrieve a new one, but 
can it do anything malicious? In which case we either need to forget them when the context changes or store them elsewhere.  Should caching be
something that can be switched on/off by the context? (shouldCacheExecutors() on the interface?)

Low Importance

- For the ternary operator, should isWritable() return true/false depending on evaluating the condition and check isWritable() of whichever branch it
would have taken?  At the moment ternary expressions are just considered NOT writable.
