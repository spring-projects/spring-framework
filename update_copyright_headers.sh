#!/bin/sh
#
# This shell script updates the copyright headers in source code files
# in the current branch so that the headers conform to the pattern:
# <start-year>-present.
# 
# For example, "Copyright 2002-2025" will be replaced with
# "Copyright 2002-current".
#
# This has only been tested on mac OS.

echo Updating copyright headers in Java, Kotlin, and Groovy source code

for file in $(find -E . -type f -regex '.+\.(java|kt|groovy)$' | uniq); do
	sed -i '' -E "s/Copyright ([0-9]{4})-[0-9]{4}/Copyright \1-present/g" $file;
done
