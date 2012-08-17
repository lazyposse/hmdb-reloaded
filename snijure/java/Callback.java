public interface Callback {
    void before(Object signature, Object[] args);
    void after(Object result);
    void afterThrow(Throwable t);
}
