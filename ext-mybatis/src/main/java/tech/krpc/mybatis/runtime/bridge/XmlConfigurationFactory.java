package tech.krpc.mybatis.runtime.bridge;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.jboss.logging.Logger;

public class XmlConfigurationFactory implements ConfigurationFactory {

    private static final Logger LOG = Logger.getLogger(XmlConfigurationFactory.class);
    private static volatile boolean warnedCloseConnectionOverride = false;

    private String mybatisConfigFile;

    public XmlConfigurationFactory() {

    }

    public XmlConfigurationFactory(String mybatisConfigFile) {
        this.mybatisConfigFile = mybatisConfigFile;
    }

    @Override
    public Configuration createConfiguration() {
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(mybatisConfigFile);

            XMLConfigBuilder builder = new XMLConfigBuilder(reader);
            //just use the type full name
            //builder.getConfiguration().getTypeAliasRegistry().registerAlias("QUARKUS", QuarkusDataSourceFactory.class);
            Configuration configuration = builder.parse();
            enforceCloseConnectionForQuarkusDataSource(configuration);
            return configuration;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Safe-by-construction guard for the {@code MANAGED + QuarkusDataSource} topology.
     *
     * <p>MyBatis {@code MANAGED} transactions with {@code closeConnection=false} NEVER close the
     * physical JDBC connection — {@link org.apache.ibatis.transaction.managed.ManagedTransaction#close()}
     * is a no-op. That contract assumes an <em>external container</em> reclaims the connection at
     * request/transaction end. Under Quarkus + Agroal ({@link QuarkusDataSource} → {@code DataSources.fromName}),
     * NO such reclaim exists for a NON-transactional read: Agroal only returns a connection on an
     * explicit {@code close()} or on JTA transaction completion. A non-tx mapper read has neither, so
     * every such read leaks one pooled connection until the pool is exhausted (acquisition timeout).
     * This is version-independent (reproduced identically on krpc 1.0.1 and 1.1.0); the connection
     * lifecycle never belonged to krpc.
     *
     * <p>Therefore, for this specific bridge topology we FORCE {@code closeConnection=true} so MyBatis
     * self-returns the pooled connection after each auto-session — the only correct behaviour here.
     * A consumer's explicit {@code closeConnection=false} is overridden: under Quarkus+Agroal there is
     * no legal way to rely on the "container closes it" semantics (there is no such container hook),
     * so the override is a fix, not a betrayal — but we warn once so it is visible. The override is
     * scoped strictly to {@link QuarkusDataSource}; any other DataSource (where the consumer may own
     * the lifecycle) is left untouched.
     *
     * <p>Transaction-safety is preserved: with an active JTA transaction, MyBatis's per-call close only
     * releases the Agroal handle/wrapper and does NOT end the enlisted transaction, so commit/rollback
     * atomicity holds (verified by the tx-safety regression test).
     */
    static void enforceCloseConnectionForQuarkusDataSource(Configuration configuration) {
        Environment env = configuration.getEnvironment();
        if (env == null) {
            return;
        }
        if (!(env.getDataSource() instanceof QuarkusDataSource)) {
            return; // only our Quarkus/Agroal bridge — leave other DataSources' lifecycle alone
        }
        if (!(env.getTransactionFactory() instanceof ManagedTransactionFactory factory)) {
            return; // JDBC/other tx factories close their own connections; nothing to fix
        }
        if (!readsWithConnectionClosed(factory)) {
            if (!warnedCloseConnectionOverride) {
                warnedCloseConnectionOverride = true;
                LOG.warn("[ext-mybatis] MANAGED transactionManager with closeConnection=false under "
                        + "QuarkusDataSource leaks one pooled connection per non-transactional read "
                        + "(Agroal has no container-reclaim hook for it). Forcing closeConnection=true "
                        + "so MyBatis returns the connection. This is transaction-safe. Remove the "
                        + "closeConnection=false property from your mybatis-config to silence this.");
            }
            // Flip in-place on the held factory instance (Environment/factory are shared with the
            // SqlSessionFactory built from this Configuration). Default is already true, so this only
            // affects configs that explicitly set false.
            Properties props = new Properties();
            props.setProperty("closeConnection", "true");
            factory.setProperties(props);
        }
    }

    /** Reads the private {@code closeConnection} flag; treats an unreadable field as already-safe. */
    private static boolean readsWithConnectionClosed(ManagedTransactionFactory factory) {
        try {
            Field f = ManagedTransactionFactory.class.getDeclaredField("closeConnection");
            f.setAccessible(true);
            return f.getBoolean(factory);
        } catch (ReflectiveOperationException e) {
            // Field name changed across MyBatis versions — force-set true unconditionally is still safe.
            return false;
        }
    }

    public String getMybatisConfigFile() {
        return mybatisConfigFile;
    }

    public void setMybatisConfigFile(String mybatisConfigFile) {
        this.mybatisConfigFile = mybatisConfigFile;
    }

    @Override
    public String toString() {
        return "XmlConfigurationFactory{" +
                "mybatisConfigFile='" + mybatisConfigFile + '\'' +
                '}';
    }
}