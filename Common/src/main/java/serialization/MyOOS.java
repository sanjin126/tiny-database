package serialization;

import util.TypeUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.function.Function;

/**
 * 生成的byte数组的大小 = 非static非transient的field类型size总和
 * 数组会存储一个int作为length
 * String会存储一个short作为length
 * 泛型会存储一个实际的类型
 *
 * 使用的类必须继承自serializable接口，且要求有public的无参构造方法
 * 暂不支持：Enum
 *
 * @see Serializable
 */
public class MyOOS extends ObjectOutputStream {

    private ByteArrayOutputStream out;
    private DataOutputStream dos;
    private Object instance;

    public MyOOS(ByteArrayOutputStream out) throws IOException {
        super();
        this.out = out;
        this.dos = new DataOutputStream(out);
    }

    @Override
    protected void writeObjectOverride(Object obj) throws IOException {
        super.writeObjectOverride(obj);
        instance = obj;
        writeObject0(obj);
    }

    private void writeNull(boolean processingArray) {
        if (processingArray) { //如果正在处理Array，那么就将处理Array空数组的行为转交给类来处理
            try {
                ArrayNullElement cast = (ArrayNullElement) instance;
                int elementSize = cast.getElementSize();
                byte[] bytes = new byte[elementSize];
                dos.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassCastException e) {
                throw new RuntimeException("请为"+instance.getClass().getName()+"实现"+ArrayNullElement.class.getName());
            }
        } else {
            throw new NullPointerException();
        }
    }

    private void writeObject0(Object obj) throws IOException {
        writeObject0(obj, false);
    }

    private void writeOrdinaryObject(Object obj) throws IOException {
        writeOrdinaryObject(obj, false);
    }


    private void writeSerialData(Object obj) throws IOException {
        writeSerialData(obj, false);
    }

    private void defaultWriteFields(Object obj) throws IOException {
        defaultWriteFields(obj, false);
    }

    private void writeObject0(Object obj, boolean processingArray) throws IOException {
        if (obj == null) {
            writeNull(processingArray);
            return;
        }
        Class<?> cl = obj.getClass();
        /**
         * 先对obj类型进行判断
         */
        if (obj instanceof String) {
            dos.writeUTF((String) obj); //TODO 并不会写入长度，需要自己来处理
        } else if ( cl.isArray() ) {
            handleArray(cl, obj);
        } else if (obj instanceof Enum) {
            throw new RuntimeException("暂时不能处理Enum");
        } else if (obj instanceof Serializable) {
            writeOrdinaryObject(obj, processingArray);
        } else {
            throw new NotSerializableException(cl.getName());
        }

    }

    private void writeOrdinaryObject(Object obj, boolean processingArray) throws IOException {

        if (obj instanceof Number) {
            if (obj instanceof Integer) {
                dos.writeInt((Integer) obj);
            } else if (obj instanceof Byte) {
                dos.writeByte((Byte) obj);
            } else if (obj instanceof Long) {
                dos.writeLong((Long) obj);
            } else if (obj instanceof Float) {
                dos.writeFloat((Float) obj);
            } else if (obj instanceof Double) {
                dos.writeDouble((Double) obj);
            } else if (obj instanceof Short) {
                dos.writeShort((Short) obj);
            }
        } else if (obj instanceof Character) {
            dos.writeChar((Character) obj);
        } else if (obj instanceof Boolean) {
            dos.writeBoolean((Boolean) obj);
        } else { //不属于primitive的包装类型，当作其他Object来依次处理其field
            writeSerialData(obj, processingArray);
        }
    }


    private void writeSerialData(Object obj, boolean processingArray) throws IOException {
        Class<?> superclass = obj.getClass().getSuperclass();
        if ( superclass != null && !superclass.getName().equals(Object.class.getName())) {
            defaultWriteFields(obj, processingArray, c -> c.getSuperclass().getDeclaredFields());
        }
        defaultWriteFields(obj, processingArray);
    }

    private interface FieldArraySupplier extends Function<Class, Field[]> {
    }

    private void defaultWriteFields(Object obj, boolean processingArray, FieldArraySupplier strategyFunc) throws IOException {
        Class<?> cl = obj.getClass();

        for (Field field : strategyFunc.apply(cl)) {
            /**
             * revise:
             * 1. 排除内部类的情况
             * 2. 排除时transient
             * 3. 如果一个属性是泛型，就需要特殊处理
             */
            if (field.isSynthetic()) {
//                throw new RuntimeException("暂不支持内部类等");
                continue;
            }
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Class<?> type = field.getType();
                Object currentField = field.get(obj);
                if (field.get(obj) == null) {
                    throw new NullPointerException(type.getName()+" "+field.getName()+": is null");
                }
                if (type.isPrimitive()) { //不可能为null，所以不用担心 TODO 是否是多余的
                    if (type == Integer.TYPE) {
                        dos.writeInt(field.getInt(obj));
                    } else if (type == Short.TYPE) {
                        dos.writeShort(field.getShort(obj));
                    }
                }
                else if (type.isArray()) {
                    handleArray(type, field.get(obj));
                }
                else { //表示是Object类型
                    TypeUtils.Condition<TypeVariable> condition = TypeUtils.isFieldTypeVariableThenConvert(field);
                    if (condition.getFlag() && ! processingArray) { //如果是泛型类型，需要写入实际的类型，否则在read时会认为是Object，从而无法newInstance
                        dos.writeUTF(currentField.getClass().getName());
                    }
                    writeObject0(currentField, processingArray);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void defaultWriteFields(Object obj, boolean processingArray) throws IOException {
       defaultWriteFields(obj, processingArray, Class::getDeclaredFields);
    }

    private void handleArray(Class<?> fieldType, Object obj) throws IOException {
        if (fieldType.isArray()) {
            Class<?> ccl = fieldType.getComponentType();
            final Object array = obj;
            if (ccl.isPrimitive()) {
                if (ccl == Integer.TYPE) {
                    int[] ia = (int[]) array;
                    dos.writeInt(ia.length);
                    for (int i : ia) {
                        dos.writeInt(i);
                    }
                } else if (ccl == Byte.TYPE) {
                    byte[] ba = (byte[]) array;
                    dos.writeInt(ba.length);
                    for (byte b : ba) {
                        dos.writeByte(b);
                    }
                } else if (ccl == Long.TYPE) {
                    long[] ja = (long[]) array;
                    dos.writeInt(ja.length);
                    for (long l : ja) {
                        dos.writeLong(l);
                    }
                } else if (ccl == Float.TYPE) {
                    float[] fa = (float[]) array;
                    dos.writeInt(fa.length);
                    for (float v : fa) {
                        dos.writeFloat(v);
                    }
                } else if (ccl == Double.TYPE) {
                    double[] da = (double[]) array;
                    dos.writeInt(da.length);
                    for (double v : da) {
                        dos.writeDouble(v);
                    }
                } else if (ccl == Short.TYPE) {
                    short[] sa = (short[]) array;
                    dos.writeInt(sa.length);
                    for (short i : sa) {
                        dos.writeShort(i);
                    }
                } else if (ccl == Character.TYPE) {
                    char[] ca = (char[]) array;
                    dos.writeInt(ca.length);
                    for (char c : ca) {
                        dos.writeChar(c);
                    }
                } else if (ccl == Boolean.TYPE) {
                    boolean[] za = (boolean[]) array;
                    dos.writeInt(za.length);
                    for (boolean b : za) {
                        dos.writeBoolean(b);
                    }
                } else {
                    throw new InternalError();
                }
            } else {
                /**
                 * 最终序列化：
                 * [length] [GenericType] [GenericType] [Object] ...
                 */
                Object[] objs = (Object[]) array;
                // 对于泛型类型的特殊处理
                final int len = objs.length;
                dos.writeInt(len);
                if (len > 0) {
                    Object element= findNonNullElement(objs);

                    writeAllGenericField(ccl, element);

                    for (int i = 0; i < len; i++) {
                        writeObject0(objs[i], true);
                    }

                }

            }
        } else {
            throw new RuntimeException(fieldType.getName()+" 不是array类型");
        }
    }

    private Object findNonNullElement(Object[] objs) {
        for (int i = 0; i < objs.length; i++) {
            if (Objects.nonNull(objs[i])) return objs[i];
        }
        return null;
    }


    /**
     * 对于Component的field进行处理，而不会处理Component本身
     * 包括field的field的field
     * @param ccl field的实际类型
     * @param fieldOwner
     * @throws IOException
     */
    private void writeAllGenericField(Class<?> ccl, Object fieldOwner) throws IOException {
        if (TypeUtils.isClassIsGeneric(ccl)) {
            for (Field field : ccl.getDeclaredFields()) {
                /**
                 如果field是一个泛型，就写入实际的类型;
                 注意：只需要处理如 "T field;"
                 而：Generic<T> field ; 和 Generic<T>[] arr; 则无需处理，因为在之后的递归过程会处理
                 */
                TypeUtils.Condition<TypeVariable> condition = TypeUtils.isFieldTypeVariableThenConvert(field);
                if (condition.getFlag()) {
                    try {
                        /**
                         * 如果数组的元素全部为空，则写入java.lang.Object;
                         */
                        Object actualField = new Object();
                        if (fieldOwner != null) {
                            actualField = field.get(fieldOwner);
                        }
                        dos.writeUTF(actualField.getClass().getName());

                        writeAllGenericField(actualField.getClass(), actualField); //如果是Object，则进入之后也不会做什么

                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }


    /**
     * 继承改变行为，避免空指针异常，因为父类中会对bos进行操作，但是在子类中并没有对其进行赋值，为null
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        out.flush();
        dos.close();
    }
}
