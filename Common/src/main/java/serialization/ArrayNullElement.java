package serialization;

/**
 * 用于自定义序列化类中，如果Array出现null元素，此时write时，通过该getElementSize()可以获知写入多少个字节的全0来表示空元素
 * 此种方式是一张妥协；如果使用bitmap的话效果会更好，并且无需继承此接口，同时null元素也无需真正的写入
 * @see MyOOS
 */
public interface ArrayNullElement {
    /**
     *
     * @return 数组元素的实际大小
     * @see MyOOS#writeNull(boolean)
     */
    int getElementSize();
}
