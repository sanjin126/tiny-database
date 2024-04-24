package util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.Supplier;

public class TypeUtils {
    public static <T> T getAs(Class<T> clazz, Object o) throws ClassCastException{
        return clazz.cast(o);
    }

    public static Condition<TypeVariable> isFieldTypeVariableThenConvert(Field field) {
        Type genericType = field.getGenericType();
        boolean flag = genericType instanceof TypeVariable<?>;
        return new Condition<>(flag, ()-> ((TypeVariable) genericType));
    }

    public static class Condition<T> {
        private final boolean flag;
        private final Supplier<T> supplier;

        Condition(boolean flag, Supplier<T> supplier) {
            this.flag = flag;
            this.supplier = supplier;
        }

        public T then() {
            if (flag) {
                return supplier.get();
            } else {
                return null;
            }
        }
        public boolean getFlag() {return flag;}
    }

    public static Condition<ParameterizedType> isFieldParameterizedTypeThenConvert(Field field) {
        Type genericType = field.getGenericType();
        boolean flag = genericType instanceof ParameterizedType;
        return new Condition<>(flag, ()-> ((ParameterizedType) genericType));
    }

    public static String getFieldParameterizedTypeName(Field field) {
        Condition<ParameterizedType> condition = isFieldParameterizedTypeThenConvert(field);
        if (condition.getFlag()) {
           return condition.then().getRawType().getTypeName();
        }
        throw new RuntimeException();
    }

    public static String getFieldTypeVariableName(Field field) {
        Condition<TypeVariable> condition = isFieldTypeVariableThenConvert(field);
        if (condition.getFlag()) {
            return condition.then().getName();
        }
        throw new RuntimeException();
    }

    public static boolean checkClassEQField(Class<?> clazz, Field field) {
        return clazz.getName().equals(getFieldParameterizedTypeName(field));
    }

    public static boolean checkTypeVariableEQField(String typeVariable, Field field) {
        return typeVariable.equals(getFieldTypeVariableName(field));
    }

    public static boolean isClassIsGeneric(Class<?> cl) {
        return cl.getTypeParameters().length > 0;
    }




}
