package org.atpfivt.ljv;

enum Gender {
    MALE,
    FEMALE,
    OTHER,
}

class Person {
    private String name;
    private Gender gender;
    private int age;

    public Person(String n, Gender g, int a) {
        setName(n);
        setGender(g);
        setAge(a);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
