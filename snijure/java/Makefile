AJC=$(ASPECTJ_HOME)/bin/ajc
LIB=$(ASPECTJ_HOME)/lib
AJC_RT=$(LIB)/aspectjrt.jar
AJC_WE=$(LIB)/aspectjweaver.jar

clean:
	rm -f *.class *.jar

Simple.class: Simple.java
	javac Simple.java

run: Simple.class
	java Simple

snijure-agent.jar: SnijureAspect.java
	$(AJC) -cp $(AJC_RT) SnijureAspect.java Callback.java CallbackImpl.java -outxml -outjar snijure-agent.jar

run-load-time-weaving: Simple.class snijure-agent.jar
	java -Dsnijure.pkgs=foo,bar    \
             -javaagent":$(AJC_WE)" \
             -classpath ".:snijure-agent.jar:$(AJC_RT)" Simple
