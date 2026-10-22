package unichope.platform.ai;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.web.ApiFailure;

/** Single-row model setting. A missing row or a model no longer in the catalog reads as default. */
public final class PostgresAiModelSettings implements AiModelSettings {
  private final JdbcClient jdbc;
  private final Clock clock;

  public PostgresAiModelSettings(DataSource source, Clock clock) {
    this.jdbc = JdbcClient.create(source);
    this.clock = clock;
  }

  @Override
  public Setting current() {
    try {
      return jdbc.sql("SELECT model, updated_at FROM ai_settings WHERE singleton")
          .query(PostgresAiModelSettings::map)
          .optional()
          .orElseGet(() -> new Setting(AiModelCatalog.defaultModel(), null));
    } catch (DataAccessException e) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
  }

  @Override
  public Setting select(String modelId, UUID adminId) {
    AiModel model =
        AiModelCatalog.find(modelId)
            .orElseThrow(() -> new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed"));
    OffsetDateTime now = clock.instant().atOffset(ZoneOffset.UTC);
    try {
      jdbc.sql(
              "INSERT INTO ai_settings (singleton, model, updated_at, updated_by)"
                  + " VALUES (true, :model, :at, :by) ON CONFLICT (singleton) DO UPDATE"
                  + " SET model=EXCLUDED.model, updated_at=EXCLUDED.updated_at, updated_by=EXCLUDED.updated_by")
          .param("model", model.id())
          .param("at", now)
          .param("by", adminId)
          .update();
    } catch (DataAccessException e) {
      throw new ApiFailure(503, "TEMPORARILY_UNAVAILABLE", "The operation is temporarily unavailable");
    }
    return new Setting(model, now.toInstant());
  }

  private static Setting map(ResultSet row, int index) throws SQLException {
    AiModel model = AiModelCatalog.find(row.getString("model")).orElse(AiModelCatalog.defaultModel());
    OffsetDateTime at = row.getObject("updated_at", OffsetDateTime.class);
    return new Setting(model, at == null ? null : at.toInstant());
  }
}
