package annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 后续可以使用AOP进行检查其值必须大于0，TODO
 * @ref <a>https://checkerframework.org/api/org/checkerframework/checker/signedness/qual/Unsigned.html</a>
 */
@Target({ ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface UnsignedInt {
}
