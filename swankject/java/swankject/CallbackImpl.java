package swankject;

import java.util.Arrays;

public class CallbackImpl implements Callback {

    public void before(Thread t, String className , String methodName, Object[] args) {
        prn("");
        prn("--- before ----------->");
        prn("    ("+t+") "+className+"."+methodName+" "+Arrays.deepToString(args));
        prn("---------------------->");
        prn("");
    }

    public void afterReturning (Thread t, String className, String methodName, Object returnValue) {
        prn("");
        prn("<-- afterReturning ----");
        prn("    ("+t+") "+className+"."+methodName+" => "+returnValue);
        prn("<----------------------");
        prn("");
    }

    public void afterThrowing (Thread t, String className, String methodName, Throwable th) {
        prn("");
        prn("<-- afterThrowing -----");
        prn("    ("+t+") "+className+"."+methodName+" throwed "+th);
        prn("<----------------------");
        prn("");
    }

    private void prn(String msg) {
        System.out.println(msg);
    }
}
