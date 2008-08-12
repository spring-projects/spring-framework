rem Used to test jar referencing, to rebuild

javac *.java -d .
jar -cvMf testcode.jar *
