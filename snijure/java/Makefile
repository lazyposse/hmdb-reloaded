AJC=$(ASPECTJ_HOME)/bin/ajc
LIB=$(ASPECTJ_HOME)/lib
AJC_RT=$(LIB)/aspectjrt.jar
AJC_WE=$(LIB)/aspectjweaver.jar

clean:
	rm -f *.class *.jar

Simple-normal.class: Simple.java
	javac Simple.java && cp Simple.class Simple-normal.class

Simple-compile-time-weaving.class: Simple.java WhereDoesTheTimeGo.java Callback.java CallbackImpl.java
	$(AJC) -cp $(AJC_RT) WhereDoesTheTimeGo.java Simple.java Callback.java CallbackImpl.java && cp Simple.class Simple-compile-time-weaving.class

run-normal: Simple-normal.class
	rm -f Simple.class && cp Simple-normal.class Simple.class && java Simple

run-compile-time-weaving: Simple-compile-time-weaving.class
	rm -f Simple.class && cp Simple-compile-time-weaving.class Simple.class && java -cp ".:$(AJC_RT)" Simple

timing.jar: WhereDoesTheTimeGo.java
	$(AJC) -cp $(AJC_RT) WhereDoesTheTimeGo.java Callback.java CallbackImpl.java -outxml -outjar timing.jar

run-load-time-weaving: Simple-normal.class timing.jar
	java -javaagent":$(AJC_WE)" \
             -classpath ".:timing.jar:$(AJC_RT)" Simple
