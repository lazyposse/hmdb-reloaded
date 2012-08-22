package swankject;

import java.util.Arrays;

public class CallbackImpl implements Callback {

    public void before(String className , String methodName, Object[] args) {
        prn("");
        prn("--- before ----------->");
        prn("    "+className+"."+methodName+" "+Arrays.deepToString(args));
        prn("---------------------->");
        prn("");
    }

    public void afterReturning (String className, String methodName, Object returnValue) {
        prn("");
        prn("<-- afterReturning ----");
        prn("    "+className+"."+methodName+" => "+returnValue);
        prn("<----------------------");
        prn("");
    }

    public void afterThrowing (String className, String methodName, Throwable t) {
        prn("");
        prn("<-- afterThrowing -----");
        prn("    "+className+"."+methodName+" throwed "+t);
        prn("<----------------------");
        prn("");
    }

    private void prn(String msg) {
        System.out.println(msg);
    }
}
