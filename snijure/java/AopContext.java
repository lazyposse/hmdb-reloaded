public class CallbackContext {
    private static Callback cb ;

    public static void set(Callback cb) {
        this.cb = db;
    }

    public static Callback get() {
        return cb;
    }

}
