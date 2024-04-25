package serialization;

import util.TypeUtils;

import java.io.*;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class MyOIS<T> extends ObjectInputStream {

    private ByteArrayInputStream in;
    private Class<T> clazz;
    private DataInputStream dis;
    private T instance;
    private static Logger logger = Logger.getLogger(MyOIS.class.getName());
    /**
     * <GenericName, ClassName>
     */
    private Map<String, String> generics2ClassName = null;

    /**
     * @param in
     * @param clazz 需要read出的类型
     * @throws IOException
     * @throws SecurityException
     */
    protected MyOIS(ByteArrayInputStream in, Class<T> clazz, T instance) throws IOException, SecurityException {
        super();
        this.in = in;
        this.clazz = clazz;
        this.dis = new DataInputStream(in);
        this.instance = instance;
    }

    @Override
    protected T readObjectOverride() throws IOException {
        return readObject0(this.clazz, this.instance);
    }

    private <Q> Q readObject0(Class<Q> cl, Q ins) throws IOException {
        if (cl == String.class) {
            return (Q) handleStringRead();
        } else if (cl.isArray()) {
            int arrLength = dis.readInt();
            return handleArrayRead(cl, arrLength);
        } else if (cl.isEnum()) {
            throw new RuntimeException("暂时不能处理Enum");
        } else if (cl.isPrimitive()) {
            return readOrdinaryObject(cl, ins);
        } else if (Serializable.class.isAssignableFrom(cl)) {
            return readOrdinaryObject(cl, ins);
        } else {
            throw new NotSerializableException(cl.getName());
        }
    }

    /**
     * Read representation of a "ordinary" (i.e., not a String, Class, ObjectStreamClass, array, or enum constant) serializable object to the stream.
     */
    private <Q> Q readOrdinaryObject(Class<Q> cl, Q ins) throws IOException {
        if (Number.class.isAssignableFrom(cl)) {
            if (cl == Integer.class) {
                return cl.cast(dis.readInt());
//                return dis.readInt();
            } else if (cl == Byte.class) {
                return cl.cast(dis.readByte());
//                return dis.readByte();
            } else if (cl == Long.class) {
                return cl.cast(dis.readLong());
//                return dis.readLong();
            } else if (cl == Float.class) {
                return cl.cast(dis.readFloat());
//                return dis.readFloat();
            } else if (cl == Double.class) {
                return cl.cast(dis.readDouble());
//                return dis.readDouble();
            } else if (cl == Short.class) {
                return cl.cast(dis.readShort());
//                return dis.readShort();
            }
            throw new RuntimeException("未知错误");
        } else if (cl == Character.class) {
            return cl.cast(dis.readChar());
//            return dis.readChar();
        } else if (cl == Boolean.class) {
            return cl.cast(dis.readBoolean());
//            return dis.readBoolean();
        } else { //不属于primitive的包装类型，当作其他Object来依次处理其field
            return readSerialData(cl, ins);
        }
    }

    /**
     * 向instance中填充field
     *
     * @param cl
     * @return
     * @see MyOIS#instance
     */
    private <Q> Q readSerialData(Class<Q> cl, Q ins) throws IOException { //TODO 暂时无法处理父类
        Class<?> superclass = cl.getSuperclass();
        if ( superclass != null && !superclass.getName().equals(Object.class.getName())) {
            defaultReadFields(cl, ins, (c)->c.getSuperclass().getDeclaredFields());
        }
        return defaultReadFields(cl, ins);
    }

    private interface StrategyFunc {
        Field[] getArray(Class c);
    }
    private int depGlobal = -1;

    private <Q> Q defaultReadFields(Class<Q> cl, Q ins, StrategyFunc strategyFunc) throws IOException {
        for (Field field : strategyFunc.getArray(cl)) { //进行field的填充
            if (field.isSynthetic()) {
//                throw new RuntimeException("暂不支持内部类等");
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
//                Class<?> fieldType = field.getType(); //TODO 删除泛型后就不报错
                Class fieldType = field.getType();

                if (fieldType.isPrimitive()) { //不可能为null，所以不用担心
                    if (fieldType == Integer.TYPE) {
                        field.setInt(ins, dis.readInt());
                    } else if (fieldType == Short.TYPE) {
                        field.setShort(ins, dis.readShort());
                    }
                } else if (fieldType.isArray()) {
                    depGlobal = 0; //当处理array时，就开启depth来记录
                    int arrLength = dis.readInt();
                    Object arr =  handleArrayRead(fieldType, arrLength);
                    depGlobal = -1;
                    Object dest =  field.get(ins);
                    System.arraycopy(arr, 0, dest, 0, arrLength); //TODO 应该是复制 而不是 field.set？
                } else { //表示是Object类型
                    Object o;
                    try {
                        TypeUtils.Condition<TypeVariable> condition = TypeUtils.isFieldTypeVariableThenConvert(field);
                        if (condition.getFlag()) {
                            String className;
                            if (! isProcessingArray()) {
                                className = dis.readUTF();
                            } else { //表示正在读取array的过程中
                                String genericName = condition.then().getName();
                                if (generics2ClassName.containsKey(cl.getName()+genericName+depGlobal)) {
                                    className = generics2ClassName.get(cl.getName()+genericName+depGlobal);
                                } else {
                                    throw new RuntimeException(cl.getName()+" "+fieldType.getName()+" 未找到泛型信息");
                                }
                            }
                            Class originalFieldType = Class.forName(className);
                            fieldType = originalFieldType;
                        }
                        if (isProcessingArray()) {
                            depGlobal ++;
                        }
                        if (isProcessingArray() && fieldType == Object.class) {
                            //说明数组元素全部为空
                            o = null;
                        } else {
                            o = readObject0(fieldType, fieldType.getConstructor().newInstance());
                        }
                        if (isProcessingArray()) {
                            depGlobal --;
                        }
                    } catch (InvocationTargetException | InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        /**
                         * 使用null进行一个尝试，如果不行再失败，因为例如Integer这样的内置类型不能访问无参的构造函数，
                         * 但是在此方法前可以成功创建且不依赖instance的存在与否
                         */
                        if (fieldType.isPrimitive() || Number.class.isAssignableFrom(fieldType) || Boolean.class.isAssignableFrom(fieldType) || Character.class.isAssignableFrom(fieldType)) {
                            o = readObject0(fieldType, null);
                        } else {
                            logger.severe(fieldType.getName() + ": 缺少无参构造函数或者不是public的");
                            throw new RuntimeException(e);
                        }
                        if (isProcessingArray()) {
                            depGlobal --;
                        }

                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    field.set(ins, o);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return ins;
    }

    private boolean isProcessingArray() {
        return Objects.nonNull(generics2ClassName);
    }

    private <Q> Q defaultReadFields(Class<Q> cl, Q ins) throws IOException {
        return defaultReadFields(cl, ins, c -> c.getDeclaredFields());
    }

    private <Q> Q handleArrayRead(Class<Q> type, int arrLength) throws IOException { //TODO 处理数组的长度

        Class<?> componentType = type.getComponentType();
        if (arrLength <= 0) {
            return (Q) Array.newInstance(componentType, 0);
        }
        Object array = Array.newInstance(componentType, arrLength);

        if (type.isArray()) {
            Class ccl = componentType; //ccl一定不是Object
            if (ccl.isPrimitive()) {
                if (ccl == Integer.TYPE) {
                    int[] ia = (int[]) array;
                    for (int i = 0; i < ia.length; i++) {
                        ia[i] = dis.readInt();
                    }
                } else if (ccl == Byte.TYPE) {
                    byte[] ba = (byte[]) array;
                    for (int i = 0; i < ba.length; i++) {
                        ba[i] = dis.readByte();
                    }
                } else if (ccl == Long.TYPE) {
                    long[] ja = (long[]) array;
                    for (int i = 0; i < ja.length; i++) {
                        ja[i] = dis.readLong();
                    }
                } else if (ccl == Float.TYPE) {
                    float[] fa = (float[]) array;
                    for (int i = 0; i < fa.length; i++) {
                        fa[i] = dis.readFloat();
                    }
                } else if (ccl == Double.TYPE) {
                    double[] da = (double[]) array;
                    for (int i = 0; i < da.length; i++) {
                        da[i] = dis.readDouble();
                    }
                } else if (ccl == Short.TYPE) {
                    short[] sa = (short[]) array;

                    for (int i = 0; i < sa.length; i++) {
                        sa[i] = dis.readShort();
                    }
                } else if (ccl == Character.TYPE) {
                    char[] ca = (char[]) array;

                    for (int i = 0; i < ca.length; i++) {
                        ca[i] = dis.readChar();
                    }
                } else if (ccl == Boolean.TYPE) {
                    boolean[] za = (boolean[]) array;
                    for (int i = 0; i < za.length; i++) {
                        za[i] = dis.readBoolean();
                    }
                } else {
                    throw new InternalError();
                }
            }  else {
                Object[] objs = (Object[]) array;

                Map<String, String> fieldGeneric = new HashMap();

                int depth = 0;
                if ( ! readAllGenericField(ccl, fieldGeneric, depth) ) { //此时数组中全为空
                    Arrays.fill(objs, null);
                    return (Q) objs;
                }

                generics2ClassName = fieldGeneric; //仅在Array中的Object read时 提供，其余时size为0

                for (int i = 0; i < objs.length; i++) {
                    try {
                        objs[i] = readObject0(ccl, ccl.getConstructor().newInstance());
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        if (ccl.isPrimitive() || ccl.isInstance(Number.class) || ccl.isInstance(Boolean.class) || ccl.isInstance(Character.class)) {
                            objs[i] = readObject0(ccl, null); //使用null进行一个尝试
                        } else {
                            logger.severe(ccl.getName() + ": 缺少无参构造函数或者不是public的");
                            throw new RuntimeException(e);
                        }
                    }
                }

                generics2ClassName = null; //使用完毕后就null
            }
        } else {
            throw new RuntimeException(type.getName() + " 不是array类型");
        }

        return (Q) array;
    }

    /**
     * @param ccl          应该为field的实际所属类型Class，所以使用Class.forName获取；如果使用field.getType得到的就是
     * @param fieldGeneric
     * @param depth
     * @throws IOException
     * @return 返回false 表示数组中元素全为空，返回进行处理
     */
    private boolean readAllGenericField(Class ccl, Map<String, String> fieldGeneric, int depth) throws IOException {
        if (TypeUtils.isClassIsGeneric(ccl)) {
            for (Field field : ccl.getDeclaredFields()) {
                TypeUtils.Condition<TypeVariable> condition = TypeUtils.isFieldTypeVariableThenConvert(field);
                if (condition.getFlag()) {
                    String className = dis.readUTF();
                    if (className.equals(Object.class.getName())) {
                        return false;
                    }
                    fieldGeneric.put(ccl.getName()+condition.then().getName()+depth, className);
                    try {
                        readAllGenericField(Class.forName(className), fieldGeneric,depth+1);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }

    /**
     * 使用readUTF则无需关注长度实现，DataOutputStream中的writeUTF已经写入了16bit长度的length
     *
     * @return
     * @throws IOException
     */
    private String handleStringRead() throws IOException { //TODO 处理String的长度
        return dis.readUTF();
    }

    @Override
    public void close() throws IOException {
        dis.close();
    }
}
