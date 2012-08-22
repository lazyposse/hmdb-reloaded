package swankject;

public interface Callback {
    void before         (Thread t, String className, String methodName, Object[]  args       );
    void afterReturning (Thread t, String className, String methodName, Object    returnValue);
    void afterThrowing  (Thread t, String className, String methodName, Throwable th         );
}
