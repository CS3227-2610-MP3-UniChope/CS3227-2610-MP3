package unichope.platform.ai;

import java.util.Objects;
import java.time.Duration;
import java.util.UUID;

/**
 * Provisional server-side generation contract. Callers authorize and validate before
 * generation. Production callers must use the admitted gateway wrapper.
 * Generation returns untrusted text and never persists a note.
 */
public interface AiGateway {
    Result generate(Request request);

    /** Validates task output before an admitted call is recorded as successful. */
    default Result generate(Request request, ResultValidator validator) {
        Result result = generate(request);
        validator.validate(result);
        return result;
    }

    @FunctionalInterface
    interface ResultValidator {
        void validate(Result result);
    }

    /** Actor/task metadata is trusted backend context, never provider prompt data. */
    record Request(UUID actorId, String task, String instructions, String input, int maxOutputTokens) {
        public Request {
            Objects.requireNonNull(actorId);
            Objects.requireNonNull(task);
            Objects.requireNonNull(instructions);
            Objects.requireNonNull(input);
            if (task.isBlank() || instructions.isBlank() || input.isBlank() || maxOutputTokens <= 0) {
                throw new IllegalArgumentException("AI request requires context, input and a positive output budget.");
            }
        }

        @Override
        public String toString() {
            return "AiGateway.Request[redacted]";
        }
    }

    /** Raw text still requires role-specific validation and human review. */
    record Result(String text, ProviderMode providerMode) {
        public Result {
            Objects.requireNonNull(text);
            Objects.requireNonNull(providerMode);
        }

        @Override
        public String toString() {
            return "AiGateway.Result[providerMode=" + providerMode + ", text=redacted]";
        }
    }

    enum ProviderMode { FAKE, SOC }

    enum FailureCode { BUSY, RATE_LIMITED, UNAVAILABLE, TIMEOUT, INVALID_OUTPUT }

    /** Infrastructure failures carry no upstream body, prompt or exception cause. */
    final class Failure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final FailureCode code;
        private final Duration retryAfter;

        public Failure(FailureCode code) {
            this(code, null);
        }

        public Failure(FailureCode code, Duration retryAfter) {
            super(safeMessage(Objects.requireNonNull(code)));
            if (retryAfter != null && (retryAfter.isZero() || retryAfter.isNegative())) {
                throw new IllegalArgumentException("Retry delay must be positive.");
            }
            if (retryAfter != null
                    && code != FailureCode.BUSY
                    && code != FailureCode.RATE_LIMITED) {
                throw new IllegalArgumentException("Retry delay only applies to admission failures.");
            }
            this.code = code;
            this.retryAfter = retryAfter;
        }

        private static String safeMessage(FailureCode code) {
            return switch (code) {
                case BUSY -> "AI generation is already in progress.";
                case RATE_LIMITED -> "AI generation limit reached.";
                case UNAVAILABLE -> "AI is currently unavailable.";
                case TIMEOUT -> "AI generation timed out.";
                case INVALID_OUTPUT -> "AI returned an invalid response.";
            };
        }

        public FailureCode code() {
            return code;
        }

        public java.util.Optional<Duration> retryAfter() {
            return java.util.Optional.ofNullable(retryAfter);
        }
    }
}
