import java.lang.reflect.Field;

class ParentClass {
    public String parentField = "parentField";
    protected String protectedField = "protectedField";
    private String privateField = "privateField";


}

class ChildClass extends ParentClass {
    public String childField = "childField";
}

public class TestReflect {
    public static void main(String[] args) {
        ChildClass child = new ChildClass();
        Class currentClass = child.getClass();

        while(currentClass != null){
            System.out.println("Class: " + currentClass.getName());

            for(Field field : currentClass.getDeclaredFields()) {
                String fieldName = field.getName();
                System.out.println("Field: " + fieldName);

                try {
                    field.setAccessible(true);
                    Object fieldVal = field.get(child);
                    System.out.println("Value: " + fieldVal);
                } catch (IllegalAccessException e) {
                    System.out.println("Cannot access value of field: "+ fieldName);
                }

                System.out.println("---");
            }

            currentClass = currentClass.getSuperclass();
        }
    }
}
