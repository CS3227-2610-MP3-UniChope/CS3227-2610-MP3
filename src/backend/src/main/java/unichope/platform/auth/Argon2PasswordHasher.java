package unichope.platform.auth;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public final class Argon2PasswordHasher implements PasswordHasher {
  private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

  public String hash(String password) {
    return encoder.encode(password);
  }

  public boolean matches(String password, String encodedHash) {
    try {
      return encoder.matches(password, encodedHash);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
