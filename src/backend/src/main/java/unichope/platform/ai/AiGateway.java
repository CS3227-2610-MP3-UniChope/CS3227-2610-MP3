package unichope.platform.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * Provisional server-side generation contract. Callers authorize and validate before
 * generation; production integration must add durable admission and deadlines.
 * Generation returns untrusted text and never persists a note.
 */
public interface AiGateway {
    Result generate(Request request);

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

    enum FailureCode { UNAVAILABLE, TIMEOUT, INVALID_OUTPUT }

    /** Infrastructure failures carry no upstream body, prompt or exception cause. */
    final class Failure extends RuntimeException {
        private final FailureCode code;

        public Failure(FailureCode code) {
            super(switch (Objects.requireNonNull(code)) {
                case UNAVAILABLE -> "AI is currently unavailable.";
                case TIMEOUT -> "AI generation timed out.";
                case INVALID_OUTPUT -> "AI returned an invalid response.";
            });
            this.code = code;
        }

        public FailureCode code() {
            return code;
        }
    }
}
