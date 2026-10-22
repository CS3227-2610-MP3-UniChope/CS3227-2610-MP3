package unichope.admin;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.ai.AiModel;
import unichope.platform.ai.AiModelCatalog;
import unichope.platform.ai.AiModelSettings;
import unichope.platform.web.ApiFailure;

/** Admin view and change of the SoCLaaS model used for every AI request. */
@RestController
@RequestMapping("/api/admin/ai/model")
@Profile("admin-api")
public final class AdminAiModelController {
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();
  private final AdminActorProvider actors;
  private final AiModelSettings settings;

  public AdminAiModelController(AdminActorProvider actors, AiModelSettings settings) {
    this.actors = actors;
    this.settings = settings;
  }

  public record ModelOption(
      String id,
      int contextLength,
      long inputCostMicrosPerMillionTokens,
      long outputCostMicrosPerMillionTokens,
      String recommendedFor) {
    static ModelOption of(AiModel model) {
      return new ModelOption(
          model.id(),
          model.contextLength(),
          model.inputMicrosPerMillionTokens(),
          model.outputMicrosPerMillionTokens(),
          model.recommendedFor());
    }
  }

  public record ModelView(
      String model, String defaultModel, Instant updatedAt, List<ModelOption> models) {
    static ModelView of(AiModelSettings.Setting setting) {
      return new ModelView(
          setting.model().id(),
          AiModelCatalog.DEFAULT_ID,
          setting.updatedAt(),
          AiModelCatalog.models().stream().map(ModelOption::of).toList());
    }
  }

  @GetMapping
  public ModelView current(HttpServletRequest r) {
    if (!r.getParameterMap().isEmpty()) throw invalid();
    actors.currentAdmin();
    return ModelView.of(settings.current());
  }

  @PutMapping
  public ModelView select(HttpServletRequest r) throws IOException {
    if (!r.getParameterMap().isEmpty()) throw invalid();
    var admin = actors.currentAdmin();
    JsonNode body = body(r);
    if (!body.get("model").isString()) throw invalid();
    return ModelView.of(settings.select(body.get("model").stringValue(), admin.actorId()));
  }

  private static JsonNode body(HttpServletRequest r) throws IOException {
    try {
      var type = MediaType.parseMediaType(Objects.requireNonNullElse(r.getContentType(), ""));
      if (!type.getType().equals("application") || !type.getSubtype().equals("json"))
        throw new ApiFailure(415, "UNSUPPORTED_MEDIA_TYPE", "JSON content type is required");
    } catch (org.springframework.http.InvalidMediaTypeException e) {
      throw new ApiFailure(415, "UNSUPPORTED_MEDIA_TYPE", "JSON content type is required");
    }
    byte[] bytes = r.getInputStream().readNBytes(65537);
    if (bytes.length > 65536)
      throw new ApiFailure(413, "PAYLOAD_TOO_LARGE", "Request body is too large");
    try {
      JsonNode node = JSON.readTree(bytes);
      if (node == null || !node.isObject() || !Set.copyOf(node.propertyNames()).equals(Set.of("model")))
        throw invalid();
      return node;
    } catch (tools.jackson.core.JacksonException e) {
      throw invalid();
    }
  }

  private static ApiFailure invalid() {
    return new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed");
  }
}
