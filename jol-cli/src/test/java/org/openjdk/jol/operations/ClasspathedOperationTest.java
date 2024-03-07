package org.openjdk.jol.operations;

import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests for {@link ClasspathedOperation}. */
public final class ClasspathedOperationTest {

    @Test
    public void testTryInstantiate() throws Exception {

        final ClasspathedOperation classpathedOperation = new ClasspathedOperation() {
            @Override
            public String label() {
                return "test";
            }

            @Override
            public String description() {
                return "test";
            }

            @Override
            protected void runWith(final Class<?> factoryClass, final Class<?> klass) throws Exception {
              throw new UnsupportedOperationException();
            }
        };

        // Falls back to constructor if factory fails:
        assertNotNull(classpathedOperation.tryInstantiate(null, Object.class));
        assertNotNull(classpathedOperation.tryInstantiate(Object.class, Object.class));
        assertNotNull(classpathedOperation.tryInstantiate(TestFactory.class, Object.class));

        try {
            classpathedOperation.tryInstantiate(null, RequiresFactory.class);
            fail("Instantiated " + RequiresFactory.class.getSimpleName() + " without factory.");
        } catch (final Exception e) {
           // Expected.
        }

        try {
            classpathedOperation.tryInstantiate(InvalidFactory.class, RequiresFactory.class);
            fail("Instantiated " + RequiresFactory.class.getSimpleName() + " without valid factory.");
        } catch (final Exception e) {
            // Expected.
        }

        // Factory is used.
        Object o = classpathedOperation.tryInstantiate(TestFactory.class, RequiresFactory.class);
        assertNotNull(o);
        assertTrue(o instanceof RequiresFactory);
    }

    public static final class RequiresFactory {
       public RequiresFactory(final Object object) {
           Objects.requireNonNull(object);
       }
    }

    public static final class TestFactory {
        public static <T> T newInstance(final Class<T> klass) {
            if (RequiresFactory.class.equals(klass)) {
                return klass.cast(new RequiresFactory(new Object()));
            }

            return null;
        }
    }

    public static final class InvalidFactory {

        public static <T> T newInstance(final Class<T> klass) {
            // Always creates objects, but lies and claims they are of type T.
            return (T) new Object();
        }
    }
}
