package sample.a;

public class A {

    public static void main(String[]argv) {
        countFast(100);
        countSlow(100);
        throwException();
    }

    private static void throwException() {
        throw new RuntimeException("hi there");
    }

    public static void countSlow(int value) {
        count(value,5);
    }

    private static String doStuff(Object o) {
        return "hello " + o;
    }

    public static void countFast(int value) {
        String s = doStuff(new java.util.Date());
        count(value,0);
    }

    private static void count(int value, int delay) {
        for (int i=0;i<value;i++) {
            try {Thread.sleep(delay);} catch (Exception e) {}
        }
    }

    public static void a(String msg) {
        System.out.println("        This is A!, msg="+msg);
    }
}
