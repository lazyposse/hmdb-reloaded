public class CallbackImpl implements Callback {
    public void before(Object signature, Object[] args) {
        System.out.println("" );
        System.out.println("vvvvvvvvv before vvvvvvvvvvvv" );
        System.out.println("signature " + signature);

        for (int i = 0 ; i < args.length ; i++) {
            System.out.println("arg"+i+"="+args[i]);
        }
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
        System.out.println("" );
    }

    public void after(Object result) {
        System.out.println("" );
        System.out.println("vvvvvvvvv after vvvvvvvvvvvvv" );
        System.out.println("result = " + result);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
        System.out.println("" );
    }

    public void afterThrow(Throwable t) {
        System.out.println("" );
        System.out.println("vvvvvvvvv afterThrow vvvvvvvvvvvvv" );
        System.out.println(" afterThrow = " + t);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
        System.out.println("" );
    }

}
