package unichope.student;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test-only simulated authentication for Student HTTP tests (stu-c1 C1-T1).
 *
 * <p>The shared actor seam {@code unichope.platform.auth.CurrentActorProvider} (TECH §2) is
 * located by name instead of a compile-time reference. The test source set therefore
 * compiles before the seam exists, and a missing seam shows as a behavioral failure
 * rather than a compile failure (workflow NOTE §2). The provider resolves the
 * session-authenticated UUID principal on every call. It is never a singleton actor and
 * never lives in main sources.
 */
public final class StudentTestActors {
    public static final String PROVIDER_TYPE = "unichope.platform.auth.CurrentActorProvider";
    public static final String PROVIDER_BEAN = "studentCurrentActorProvider";

    private StudentTestActors() {}

    /** The C1-T1 actor seam type, or empty before it is implemented. */
    public static Optional<Class<?>> providerType() {
        try {
            return Optional.of(Class.forName(PROVIDER_TYPE, false, StudentTestActors.class.getClassLoader()));
        } catch (ClassNotFoundException missing) {
            return Optional.empty();
        }
    }

    /** UUID principal of the current request's authenticated security context, if any. */
    public static Optional<UUID> currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID id
                ? Optional.of(id) : Optional.empty();
    }

    /** A test-only implementation of the seam, or empty before the seam exists. */
    public static Optional<Object> sessionProvider() {
        return providerType().map(type -> Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> invoke(proxy, method, args)));
    }

    /** Registers the test-only provider when the seam exists; returns whether it did. */
    public static boolean registerSessionProvider(ConfigurableListableBeanFactory factory) {
        Optional<Object> provider = sessionProvider();
        provider.ifPresent(instance -> factory.registerSingleton(PROVIDER_BEAN, instance));
        return provider.isPresent();
    }

    private static Object invoke(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "currentActorId" -> currentPrincipal();
            case "toString" -> "test-only session actor provider";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }
}
