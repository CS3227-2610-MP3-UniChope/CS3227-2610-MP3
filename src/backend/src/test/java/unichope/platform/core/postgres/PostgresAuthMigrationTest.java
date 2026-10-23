package unichope.platform.core.postgres;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Connection;
import java.util.Properties;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
@Tag("postgres")
class PostgresAuthMigrationTest {
 @Test void v7MarksExistingTutorsButLeavesStudentsUnchanged() throws Exception {
  String url=System.getProperty("core.test.jdbcUrl"); assertNotNull(url,"Explicit disposable core.test.jdbcUrl is required");
  var source=new DriverManagerDataSource(url,System.getProperty("core.test.user",""),System.getProperty("core.test.password",""));
  String schema="migration_test_"+UUID.randomUUID().toString().replace("-","");
  try(Connection c=source.getConnection();var s=c.createStatement()) {s.execute("CREATE SCHEMA "+schema);}
  Properties properties=new Properties();properties.setProperty("currentSchema",schema);source.setConnectionProperties(properties);
  try {
   Flyway.configure().dataSource(source).defaultSchema(schema).locations("classpath:db/core-review","classpath:db/auth-review").target(MigrationVersion.fromVersion("2")).load().migrate();
   var db=JdbcClient.create(source);
   for(String role:new String[]{"TUTOR","STUDENT"}) db.sql("INSERT INTO users(id,name,email,role,active,password_hash) VALUES(:id,'Synthetic',:email,:role,true,'synthetic-H1')")
       .param("id",UUID.randomUUID()).param("email",role.toLowerCase()+"@local").param("role",role).update();
   Flyway.configure().dataSource(source).defaultSchema(schema).locations("classpath:db/core-review","classpath:db/auth-review").load().migrate();
   assertTrue(db.sql("SELECT must_change_password FROM users WHERE role='TUTOR'").query(Boolean.class).single());
   assertFalse(db.sql("SELECT must_change_password FROM users WHERE role='STUDENT'").query(Boolean.class).single());
  } finally {try(Connection c=source.getConnection();var s=c.createStatement()) {s.execute("DROP SCHEMA "+schema+" CASCADE");}}
 }
 @Test void credentiallessExistingSchemaFailsAtomically() throws Exception {
  String url=System.getProperty("core.test.jdbcUrl"); assertNotNull(url,"Explicit disposable core.test.jdbcUrl is required");
  var source=new DriverManagerDataSource(url,System.getProperty("core.test.user",""),System.getProperty("core.test.password",""));
  String schema="migration_test_"+UUID.randomUUID().toString().replace("-","");
  try(Connection c=source.getConnection();var s=c.createStatement()) {s.execute("CREATE SCHEMA "+schema);}
  Properties properties=new Properties();properties.setProperty("currentSchema",schema);source.setConnectionProperties(properties);
  try {
   Flyway.configure().dataSource(source).defaultSchema(schema).locations("classpath:db/core-review").load().migrate();
   var db=JdbcClient.create(source);
   db.sql("INSERT INTO users VALUES (:id,'Synthetic','old@local','STUDENT',true)").param("id",UUID.randomUUID()).update();
   assertThrows(RuntimeException.class,()->Flyway.configure().dataSource(source).defaultSchema(schema).locations("classpath:db/core-review","classpath:db/auth-review").load().migrate());
   assertEquals(5,db.sql("SELECT count(*) FROM information_schema.columns WHERE table_schema=:schema AND table_name='users'").param("schema",schema).query(Integer.class).single());
   assertEquals(1,db.sql("SELECT count(*) FROM users").query(Integer.class).single());
   assertEquals(0,db.sql("SELECT count(*) FROM flyway_schema_history WHERE version='2'").query(Integer.class).single());
  } finally {try(Connection c=source.getConnection();var s=c.createStatement()) {s.execute("DROP SCHEMA "+schema+" CASCADE");}}
 }
 @Test void emptySchemaConstraintsAndDefaults() throws Exception {
  String url=System.getProperty("core.test.jdbcUrl"); assertNotNull(url,"Explicit disposable core.test.jdbcUrl is required");
  var source=new DriverManagerDataSource(url,System.getProperty("core.test.user",""),System.getProperty("core.test.password",""));
  String schema="migration_test_"+UUID.randomUUID().toString().replace("-","");
  try(Connection c=source.getConnection();var s=c.createStatement()) {s.execute("CREATE SCHEMA "+schema);}
  Properties properties=new Properties();properties.setProperty("currentSchema",schema);source.setConnectionProperties(properties);
  try {
   Flyway.configure().dataSource(source).defaultSchema(schema).locations("classpath:db/core-review","classpath:db/auth-review").load().migrate();
   var db=JdbcClient.create(source);
   assertEquals("text",db.sql("SELECT data_type FROM information_schema.columns WHERE table_schema=:schema AND table_name='users' AND column_name='password_hash'").param("schema",schema).query(String.class).single());
   assertEquals("false",db.sql("SELECT column_default FROM information_schema.columns WHERE table_schema=:schema AND table_name='users' AND column_name='must_change_password'").param("schema",schema).query(String.class).single());
   assertTrue(db.sql("SELECT column_default IS NULL FROM information_schema.columns WHERE table_schema=:schema AND table_name='users' AND column_name='password_hash'").param("schema",schema).query(Boolean.class).single());
   for(String columns:new String[]{"NULL,0","' ',0","'synthetic-H1',-1","'synthetic-H1',NULL"}) assertThrows(RuntimeException.class,()->db.sql("INSERT INTO users (id,name,email,role,active,password_hash,session_generation) VALUES (:id,'Synthetic','invalid@local','STUDENT',true,"+columns+")").param("id",UUID.randomUUID()).update());
   assertThrows(RuntimeException.class,()->db.sql("INSERT INTO users (id,name,email,role,active) VALUES (:id,'Synthetic','missing@local','STUDENT',true)").param("id",UUID.randomUUID()).update());
   db.sql("INSERT INTO users (id,name,email,role,active,password_hash) VALUES (:id,'Synthetic','valid@local','STUDENT',true,'synthetic-H1')").param("id",UUID.randomUUID()).update();
   assertEquals(0,db.sql("SELECT session_generation FROM users").query(Long.class).single());
   assertFalse(db.sql("SELECT must_change_password FROM users").query(Boolean.class).single());
  } finally {try(Connection c=source.getConnection();var s=c.createStatement()) {s.execute("DROP SCHEMA "+schema+" CASCADE");}}
 }
}
