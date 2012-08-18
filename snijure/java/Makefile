AJC=$(ASPECTJ_HOME)/bin/ajc
LIB=$(ASPECTJ_HOME)/lib
AJC_RT=$(LIB)/aspectjrt.jar
AJC_WE=$(LIB)/aspectjweaver.jar

clean:
	find . -name '*.jar' -exec rm {} \; ; find . -name '*.class' -exec rm {} \;

sample/a/A.class: sample/a/A.java
	javac sample/a/A.java

sample/b/B.class: sample/b/B.java
	javac sample/b/B.java

sample/Main.class: sample/Main.java sample/a/A.class sample/b/B.class
	javac sample/Main.java

run: sample/Main.class
	java sample.Main

snijure-agent.jar: SnijureAspect.java Callback.java CallbackImpl.java
	$(AJC) -1.6 -cp $(AJC_RT) SnijureAspect.java Callback.java CallbackImpl.java -outjar snijure-agent.jar

run-load-time-weaving: sample/Main.class snijure-agent.jar
	java -Dsnijure.pkgs=foo,bar    \
             -javaagent":$(AJC_WE)" \
             -classpath ".:snijure-agent.jar:$(AJC_RT)" sample.Main
