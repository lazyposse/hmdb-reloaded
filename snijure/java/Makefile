AJC=$(ASPECTJ_HOME)/bin/ajc
LIB=$(ASPECTJ_HOME)/lib
AJC_RT=$(LIB)/aspectjrt.jar
AJC_WE=$(LIB)/aspectjweaver.jar

clean:
	rm -f *.class *.jar

sample/a/A.class: sample/a/A.java
	javac sample/a/A.java

run: sample/a/A.class
	java sample.a.A

snijure-agent.jar: SnijureAspect.java
	$(AJC) -cp $(AJC_RT) SnijureAspect.java Callback.java CallbackImpl.java -outxml -outjar snijure-agent.jar

run-load-time-weaving: sample/a/A.class snijure-agent.jar
	java -Dsnijure.pkgs=foo,bar    \
             -javaagent":$(AJC_WE)" \
             -classpath ".:snijure-agent.jar:$(AJC_RT)" sample.a.A
