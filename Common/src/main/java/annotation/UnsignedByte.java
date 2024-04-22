package annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 后续可以使用AOP进行检查其值必须大于0，TODO
 * @see UnsignedInt
 */
@Target({ ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface UnsignedByte {
}
