package serialization;

import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.TypeUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

public class TestMyOOIS {

    public static Logger logger = Logger.getLogger(TestMyOOIS.class.getName());

    interface CheckFunc {
        public <T> void test(T expectValue, T res);
    }

    public static <T> void testOneType(Class<T> testClass, T expectValue, T instance, CheckFunc checkFunc) throws IOException {
        byte[] bytes;
        {
            try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MyOOS oos = new MyOOS(baos)) {
                oos.writeObjectOverride((expectValue));
                bytes = baos.toByteArray();
            }
            try ( MyOIS<T> ois = new MyOIS<>(new ByteArrayInputStream(bytes), testClass, instance);){
                T res = ois.readObjectOverride();
                checkFunc.test(expectValue, res);
                logger.info("测试结果为：expect："+expectValue+"  actual: "+res);
            }
        }
    }



    @Test
    public void testPrimitive() throws IOException {
        {
            testOneType(Long.class, Long.MAX_VALUE, null, Assertions::assertEquals);
        }
        {
            testOneType(Character.class, '你', null, Assertions::assertEquals);
        }
    }

    @Test
    public void testPrimitiveWrapperClass() throws IOException {
        testOneType(Integer.class, Integer.valueOf(Integer.MAX_VALUE), null, Assertions::assertEquals);
    }

    @Test
    public void testPrimitiveArray() throws IOException {
        int[] ints = new int[10];
        Random random = new Random(47);
        for (int i = 0; i < ints.length; i++) {
            ints[i] = random.nextInt();
        }
        Class aClass = ints.getClass(); //在使用array时，其class应使用rawtype
        testOneType(aClass, ints, null, new CheckFunc() {
            @Override
            public <T> void test(T expectValue, T res) {
                Arrays.equals((int[]) expectValue, (int[]) res);
            }
        });
    }

    /**
     * 暂时没有实现除了继承serialization.ArrayNullElement的数组的其他null元素的序列化存储
     * @throws IOException
     */
    @Test
    public void testObjectArray() throws IOException {
        Integer[] arr = new Integer[3];
        Class aClass = arr.getClass();
        testOneType(aClass, arr, arr, new CheckFunc() {
            @Override
            public <T> void test(T expectValue, T res) {
                Arrays.equals((Integer[]) expectValue, (Integer []) res);
            }
        });

    }


    /**
     * 测试泛型类型的数组，继承，泛型类中包含泛型类的情况
     * @throws IOException
     */
    @Test
    public void testCustomObject() throws IOException {
        Holder<Integer> holder = new Holder<Integer>();
        testOneType(Holder.class, holder, new Holder(), Assertions::assertEquals);
    }

    @Test
    public void testArrLengthEQ0() throws IOException {
        Integer[] arr = new Integer[0];
        Class aClass = arr.getClass();
        testOneType(aClass, arr, arr, new CheckFunc() {
            @Override
            public <T> void test(T expectValue, T res) {
                Arrays.equals((Integer[]) expectValue, (Integer[]) res);
            }
        });
    }

    @Test
    public void test() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        Holder<Integer> holder = new Holder<Integer>();
        oos.writeObject(holder);
        byte[] bytes = baos.toByteArray();
        System.out.println(new String(bytes));
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object o = ois.readObject();
        System.out.println(o);
    }

    @Test
    public void testClassName() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String name = Dog.class.getName();
        Class<?> aClass = Class.forName(name);
        System.out.println(aClass.getConstructor().newInstance());
    }

    @Test
    public void testExtendClass() throws NoSuchFieldException, IllegalAccessException {
        Dog dog = new Dog("haha");
        System.out.println(Dog.class.getSuperclass().getDeclaredField("animal").get(dog));

    }

}

@Data
class Animal {
    String animal = "animal";
}

class Dog extends Animal implements Serializable{

    String name = "dog";
    int age = 10;

    public Dog(){}

    public Dog(String superName) {
        super.animal = superName;
    }

    @Override
    public String toString() {
        return "Dog{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", animal='" + animal + '\'' +
                '}';
    }
}

class Cat<T> implements Serializable{
    T obj;

    public Cat(){}

    public Cat(T t) {
        obj = t;
    }

    @Override
    public String toString() {
        return "Cat{" +
                "obj=" + obj +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cat<?> cat)) return false;
        return Objects.equals(obj, cat.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
    }
}

class Generic<T> implements Serializable {
//    Class<T> clazz;
    T obj;

    public Generic() {
        obj = null;
    }

    public Generic(T obj) {
        this.obj = obj;
    }

    @Override
    public String toString() {
        return "Generic{" +
                "obj=" + obj +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Generic<?> generic)) return false;
        return Objects.equals(obj, generic.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
    }


}

@Data
class Holder<T> implements Serializable  {

//    String name = "haha";
//    Dog[] animals = new Dog[]{new Dog(), new Dog("overwrite")};
//    Generic[] g = new Generic[]{new Generic<Cat<Cat<String>>>(new Cat<>(new Cat<>("hello world"))),
//            new Generic<Cat<Cat<String>>>(new Cat<>(new Cat<>("hello Java")))};
//    boolean flag = true;
    Generic[] g = new Generic[]{new Generic<Cat<Integer>>(new Cat<>(1))};
    Generic[] gg = new Generic[0];




}
