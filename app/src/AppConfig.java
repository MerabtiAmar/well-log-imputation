import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Configuration lue dans les variables d'environnement (valeurs par défaut pour un poste local).
 *
 *   ENAGEO_DB_URL       jdbc:mysql://localhost:3306/enageo
 *   ENAGEO_DB_USER      root
 *   ENAGEO_DB_PASSWORD  (vide)
 *   ENAGEO_PYTHON       python
 */
public final class AppConfig {
    private AppConfig() {
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                env("ENAGEO_DB_URL", "jdbc:mysql://localhost:3306/enageo"),
                env("ENAGEO_DB_USER", "root"),
                env("ENAGEO_DB_PASSWORD", ""));
    }

    public static String pythonExecutable() {
        return env("ENAGEO_PYTHON", "python");
    }
}
