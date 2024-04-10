import org.junit.jupiter.api.Test;

import java.util.Optional;

public class TestOptional {
    @Test
    public void testMap() {
        Optional<String> optional = Optional.of("hello");
        Optional<Optional<Integer>> nested = optional.map(s -> Optional.of(s.length()));
// nested 是一个 Optional<Optional<Integer>> 对象
        Optional<Integer> flatNested = optional.flatMap(s -> Optional.of(s.length()));
// flatNested 是一个包含值 5 的 Optional<Integer> 对象

    }
}
