package unichope.admin;

import unichope.platform.core.AdminContext;

/** Supplies current authenticated, active Admin identity; never browser input. */
@FunctionalInterface
public interface AdminActorProvider {
  AdminContext currentAdmin();
}
