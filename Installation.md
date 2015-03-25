## Installation Instructions ##
### Getting started ###
  * First start by installing lombok.
  * Morbok needs lombok in order to work.  Morbok is compatible with lombok 0.9.x+.
  * Go to http://projectlombok.org for details.

### Using javac ###
  * In order to get morbok working via javac just put it on the compile classpath of your project along with lombok.

### Using Eclipse ###
  * Morbok ships with it's own installer... just run the jar.
  * If for some reason the installer messes up you can do some manual tweaking.  Do this:
  * Find where the lombok installer put the lombok jar.
    1. Windows example: C:\eclipse
    1. Mac example: /Applications/eclipse/Eclipse.app/Contents/MacOS
  * Copy morbok.jar to that same location alongside lombok.jar.
  * Find the eclipse.ini file in that folder
  * Add morbok.jar to the bootclasspath.  You'll modify it to look like this:
    1. Windows:-Xbootclasspath/a;lombok.jar;morbok.jar
    1. Mac/Linux: -Xbootclasspath/a:lombok.jar:morbok.jar
  * Now restart eclipse and you're ready to go.