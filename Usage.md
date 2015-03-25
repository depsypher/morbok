Morbok features:

The vanilla @Logger annotation generates an apache commons logger with the variable name "log" and a log name reflecting the fully qualified name of the class it's placed on.

You can specify SLF4J or JAVA logging if you don't want to generate the default apache commons logger:
```
@Logger(type=Logger.Type.SLF4J) 
@Logger(type=Logger.Type.JAVA)
```

You can also override generating a logger based on the fully qualified class name:
```
@Logger(value="SPECIAL_LOGGER")
```

And if you don't like your loggers to use a variable named "log" you can override that too:
```
@Logger(var="myLogger")
```