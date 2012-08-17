import java.util.Arrays;

aspect SnijureAspect {

  pointcut methodsOfInterest(): execution(* *(..))        &&
                              !within(SnijureAspect) &&
                              !within(CallbackImpl);;

  private int nesting = 0;

  private static Callback cb = new CallbackImpl();

  private static String[] pkgs = System.getProperty("snijure.pkgs").split(",");

  private Callback callback = cb;

  public static void setCallback(Callback callback) {
      cb = callback;
  }

  Object around(): methodsOfInterest()  {
      System.out.println("-----> " + Arrays.deepToString(pkgs));

      Object sig = thisJoinPoint.getSignature();
      callback.before(sig, thisJoinPoint.getArgs());
      nesting++;
      long stime=System.currentTimeMillis();

      Object o = null;
      try {
          o = proceed();
      } catch (Throwable t) {
          callback.afterThrow(t);
          // TODO fix me
          throw new RuntimeException(t);
      }

      long etime=System.currentTimeMillis();
      nesting--;

      callback.after(o);

      StringBuilder info = new StringBuilder();
      for (int i=0;i<nesting;i++) {
          info.append("  ");
      }
      info.append(thisJoinPoint+" took "+(etime-stime)+"ms");
      System.out.println(info.toString());
      return o;
  }
}
