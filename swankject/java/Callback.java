public interface Callback {
    void before         (String className, String methodName, Object[]  args       );
    void afterReturning (String className, String methodName, Object    returnValue);
    void afterThrowing  (String className, String methodName, Throwable t          );
}
