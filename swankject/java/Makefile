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

swankject-agent.jar: SwankjectAspect.java Callback.java CallbackImpl.java
	$(AJC) -1.6 -cp $(AJC_RT) SwankjectAspect.java Callback.java CallbackImpl.java -outjar swankject-agent.jar

run-load-time-weaving: sample/Main.class swankject-agent.jar
	java -javaagent":$(AJC_WE)" \
             -classpath ".:swankject-agent.jar:$(AJC_RT)" sample.Main
