package ljv;

class Person {
    private String name;
    private boolean isMale;
    private int age;

    public Person(String n, boolean m, int a) {
        setName(n);
        setMale(m);
        setAge(a);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isMale() {
        return isMale;
    }

    public void setMale(boolean isMale) {
        this.isMale = isMale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
