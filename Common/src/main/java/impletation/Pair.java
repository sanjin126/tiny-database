package impletation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

public class Pair<firstType, secondType> implements Serializable {

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

    /**
     * 仅仅序列化的时候使用
     */
    public Pair(){
        first = null;
        second = null;
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
