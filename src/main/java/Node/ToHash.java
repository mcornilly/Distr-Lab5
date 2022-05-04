package Node;

public class ToHash {

    public static int hash(String string) {
        long max = 2147483647;
        long min = -2147483648;
        return (int) (((long) string.hashCode() + max) * (32768.0 / (max + Math.abs(min))));
    }
}
