AJC=$(ASPECTJ_HOME)/bin/ajc
LIB=$(ASPECTJ_HOME)/lib
AJC_RT=$(LIB)/aspectjrt.jar
AJC_WE=$(LIB)/aspectjweaver.jar
CLJ_LIB=../target/swankject-0.1.0-SNAPSHOT-standalone.jar

clean:
	find . -name '*.jar' -exec rm {} \; ; find . -name '*.class' -exec rm {} \;

sample/a/A.class: sample/a/A.java
	javac sample/a/A.java

sample/b/B.class: sample/b/B.java
	javac sample/b/B.java

sample/Main.class: sample/Main.java sample/a/A.class sample/b/B.class
	javac sample/Main.java

sample/Graph.class: sample/Graph.java
	javac sample/Graph.java

run: sample/Main.class
	java sample.Main

$(CLJ_LIB): ../project.clj
	cd .. && lein uberjar

swankject-agent.jar: swankject/SwankjectAspect.java swankject/Callback.java swankject/CallbackImpl.java $(CLJ_LIB)
	$(AJC) -1.6 -cp "$(AJC_RT):$(CLJ_LIB)" swankject/SwankjectAspect.java swankject/Callback.java swankject/CallbackImpl.java -outjar swankject-agent.jar

run-load-time-weaving: sample/Main.class swankject-agent.jar $(CLJ_LIB)
	java -javaagent":$(AJC_WE)" \
             -classpath ".:swankject-agent.jar:$(AJC_RT):$(CLJ_LIB)" sample.Main

sample/awt/BufferedShapeMover.class: sample/awt/BufferedShapeMover.java
	javac sample/awt/BufferedShapeMover.java

run-load-time-weaving-awt: sample/awt/BufferedShapeMover.class sample/Graph.class swankject-agent.jar $(CLJ_LIB)
	java -javaagent":$(AJC_WE)" \
             -classpath ".:swankject-agent.jar:$(AJC_RT):$(CLJ_LIB)" sample.awt.BufferedShapeMover
