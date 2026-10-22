package unichope.platform.core.model.user;

import java.util.Objects;
import java.util.UUID;
import unichope.platform.core.model.Validation;

public record Admin(UUID id, String name, String email, boolean isActive) implements User {
    public Admin {
        Objects.requireNonNull(id, "id");
        name = Validation.text(name, "name");
        email = Validation.email(email);
    }

    @Override
    public Role role() { return Role.ADMIN; }

    @Override
    public Admin withActive(boolean active) {
        return new Admin(id, name, email, active);
    }
}
