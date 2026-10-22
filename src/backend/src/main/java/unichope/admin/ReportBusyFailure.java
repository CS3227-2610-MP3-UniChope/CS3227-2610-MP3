package unichope.admin;

/** A report is still outstanding or holds an uncertain provider lease. */
public final class ReportBusyFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ReportBusyFailure() {
    super("An activity report is already outstanding");
  }
}
