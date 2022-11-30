#!/bin/sh
#
# This shell script updates the copyright headers in source code files
# in the current branch that have been added or modified during the
# current year (at least as much as the `git diff` command supports the
# date range in terms of log history).
#
# This has only been tested on mac OS.

current_year=$(date +'%Y')
echo Updating copyright headers in Java, Kotlin, and Groovy source code for year $current_year

# Added/Modified this year and committed
for file in $(git --no-pager diff --name-only --diff-filter=AM @{$current_year-01-01}..@{$current_year-12-31} | egrep "^.+\.(java|kotlin|groovy)$" | uniq); do
	sed -i '' -E "s/Copyright 2002-[0-9]{4}/Copyright 2002-$current_year/g" $file;
done

# Added/Modified and staged but not yet committed
for file in $(git --no-pager diff --name-only --diff-filter=AM --cached | egrep "^.+\.(java|kotlin|groovy)$" | uniq); do
	sed -i '' -E "s/Copyright 2002-[0-9]{4}/Copyright 2002-$current_year/g" $file;
done
