package org.openjdk.jol.info;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClassDataTest {

  private final ClassData classData = ClassData.parseClass(Class3.class);

  private static final String PREFIX = "org.openjdk.jol.info.ClassDataTest.Class";

  static class Class1 {
    int f1;
    int f2;
    int f3;
  }

  static class Class2 extends Class1 {
    int f1;
    int f2;
    int f3;
  }

  static class Class3 extends Class2 {
    int f1;
    int f2;
    int f3;
    Object f4;
  }

  @Test
  public void fields() {
    List<FieldData> fields = classData.fields();
    assertEquals(10, fields.size());
  }

  @Test
  public void ownFields() {
    List<FieldData> ownFields = classData.ownFields();
    String klass3 = Class3.class.getCanonicalName();

    assertEquals(4, ownFields.size());
    assertEquals("[" + PREFIX + "3.f1: int, " +  PREFIX + "3.f2: int, " + PREFIX + "3.f3: int, " + PREFIX + "3.f4: java.lang.Object]", ownFields.toString());
  }

  @Test
  public void oopsCount() {
    int oopsCount = classData.oopsCount();
    assertEquals(1, oopsCount);
  }

  @Test
  public void fieldsFor1() {
    String klass = Class1.class.getCanonicalName();
    List<FieldData> classFields = classData.fieldsFor(klass);
    assertEquals(3, classFields.size());
    assertEquals(klass, classFields.get(0).hostClass());
    assertEquals(klass, classFields.get(1).hostClass());
    assertEquals(klass, classFields.get(2).hostClass());
    assertEquals("[" + PREFIX + "1.f1: int, " + PREFIX + "1.f2: int, " + PREFIX + "1.f3: int]", classFields.toString());
  }

  @Test
  public void fieldsFor2() {
    String klass = Class2.class.getCanonicalName();
    List<FieldData> classFields = classData.fieldsFor(klass);
    assertEquals(3, classFields.size());
    assertEquals(klass, classFields.get(0).hostClass());
    assertEquals(klass, classFields.get(1).hostClass());
    assertEquals(klass, classFields.get(2).hostClass());
    assertEquals("[" + PREFIX + "2.f1: int, " + PREFIX + "2.f2: int, " + PREFIX + "2.f3: int]", classFields.toString());
  }

  @Test
  public void fieldsFor3() {
    String klass = Class3.class.getCanonicalName();
    List<FieldData> classFields = classData.fieldsFor(klass);
    assertEquals(4, classFields.size());
    assertEquals(klass, classFields.get(0).hostClass());
    assertEquals(klass, classFields.get(1).hostClass());
    assertEquals(klass, classFields.get(2).hostClass());
    assertEquals(klass, classFields.get(3).hostClass());
    assertEquals("[" + PREFIX + "3.f1: int, " +  PREFIX + "3.f2: int, " + PREFIX + "3.f3: int, " + PREFIX + "3.f4: java.lang.Object]", classFields.toString());
  }

  @Test
  public void fieldsForNonExisting() {
    List<FieldData> classFields = classData.fieldsFor("Non existent class");
    assertTrue(classFields.isEmpty());
  }
}
