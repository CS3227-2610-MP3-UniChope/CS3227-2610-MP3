package unichope.platform.core.model.user;

import java.util.Objects;
import java.util.UUID;
import unichope.platform.core.model.Validation;

public record Student(UUID id, String name, String email, boolean isActive) implements User {
    public Student {
        Objects.requireNonNull(id, "id");
        name = Validation.text(name, "name");
        email = Validation.email(email);
    }

    @Override
    public Role role() { return Role.STUDENT; }

    @Override
    public Student withActive(boolean active) {
        return new Student(id, name, email, active);
    }
}
