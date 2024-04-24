package impletation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

public class Pair<firstType, secondType> {

    public final firstType first;
    public final secondType second;

    public static void main(String[] args) {
        Pair<Integer, Integer> integerIntegerPair = new Pair<>(3, 4);

        for (Field field : Pair.class.getDeclaredFields()) {
            System.out.println(Pair.class.getName());

            if (field.getGenericType() instanceof TypeVariable<?>) {
                System.out.println(((TypeVariable) field.getGenericType()).getName());
                System.out.println(((TypeVariable) field.getGenericType()).getGenericDeclaration());
            }
        }
    }


    public Pair(firstType first, secondType second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
