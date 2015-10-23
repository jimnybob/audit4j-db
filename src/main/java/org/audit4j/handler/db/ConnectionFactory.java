/*
 * Copyright (c) 2014-2015 Janith Bandara, This source is a part of
 * Audit4j - An open source auditing framework.
 * http://audit4j.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.audit4j.handler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.audit4j.core.exception.InitializationException;
import org.audit4j.core.util.Log;

import com.zaxxer.hikari.HikariDataSource;

/**
 * A factory for creating Connection objects.
 * 
 * @author <a href="mailto:janith3000@gmail.com">Janith Bandara</a>
 */
final class ConnectionFactory {

    /** The Constant SINGLE_CONNECTION. */
    static final String SINGLE_CONNECTION = "single";

    /** The Constant POOLED_CONNECTION. */
    static final String POOLED_CONNECTION = "pooled";

    /** The db_driver. */
    private String driver = "org.hsqldb.jdbcDriver";

    private final String dataSourceClass = "org.hsqldb.jdbc.JDBCDataSource";

    /** The db_url. */
    private String url = "jdbc:hsqldb:hsql://localhost/audit4j";

    /** The jndi data source. */
    private String jndiDataSource;

    /** The db_user. */
    private String user = "audit4juser";

    /** The db_password. */
    private String password = "audit4jpassword";

    /** The connection type. */
    private ConnectionType connectionType;

    private HikariDataSource ds;

    /**
     * Instantiates a new connection factory.
     */
    private ConnectionFactory() {
    }

    /** The instance. */
    private static ConnectionFactory instance;

    /** The data source. */
    private DataSource dataSource;

    /**
     * Gets the connection.
     * 
     * @return the connection
     */
    Connection getConnection() {
        if (dataSource != null) {
            return getDataSourceConnection();
        } else {
            return getSingleConnection();
        }
    }

    /**
     * Gets the connection.
     * 
     * @return the connection
     */
    Connection getSingleConnection() {
        Connection connection = null;
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new InitializationException("Could not initialize the connection", e);
        } catch (ClassNotFoundException e) {
            throw new InitializationException("Could not find the driver class", e);
        }
        return connection;
    }

    /**
     * Gets the data source connection.
     * 
     * @return the data source connection
     * @since 2.4.0
     */
    Connection getDataSourceConnection() {
        Connection connection = null;
        try {
            connection = ds.getConnection();
        } catch (SQLException ex) {
            throw new InitializationException("Could not obtain the db connection: Cannot get connection", ex);
        }
        return connection;
    }

    /**
     * Inits the.
     */
    void init() {
        if (dataSource == null) {
            if (connectionType.equals(ConnectionType.POOLED)) {
                ds = new HikariDataSource();

                ds.setMaximumPoolSize(100);
                ds.setDataSourceClassName(dataSourceClass);
                ds.addDataSourceProperty("url", url);
                ds.addDataSourceProperty("user", user);
                ds.addDataSourceProperty("password", password);
                ds.addDataSourceProperty("maximumPoolSize", 10);

                // ds.addDataSourceProperty("maxLifetime", 3000);
                // ds.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
                // ds.addDataSourceProperty("useServerPrepStmts", true);
                // dataSource = ds;

            } else if (connectionType.equals(ConnectionType.JNDI)) {
                if (null == jndiDataSource) {
                    throw new InitializationException("Could not obtain the db connection: jndi data source is null");
                }
                String DATASOURCE_CONTEXT = jndiDataSource;
                try {
                    Context initialContext = new InitialContext();
                    dataSource = (DataSource) initialContext.lookup(DATASOURCE_CONTEXT);
                    if (dataSource == null) {
                        Log.error("Failed to lookup datasource.");
                        throw new InitializationException(
                                "Could not obtain the db connection: Failed to lookup datasource: " + jndiDataSource);
                    }
                } catch (NamingException ex) {
                    throw new InitializationException("Could not obtain the db connection: jndi lookup failed", ex);
                }
            }
        }
    }

    /**
     * Stop.
     */
    void stop() {
        if (ds != null) {
            ds.close();
        }
    }

    /**
     * Sets the driver.
     * 
     * @param driver
     *            the new driver
     */
    void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Sets the url.
     * 
     * @param url
     *            the new url
     */
    void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the user.
     * 
     * @param user
     *            the new user
     */
    void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the password.
     * 
     * @param password
     *            the new password
     */
    void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the jndi data source.
     * 
     * @param jndiDataSource
     *            the new jndi data source
     */
    public void setJndiDataSource(String jndiDataSource) {
        this.jndiDataSource = jndiDataSource;
    }

    /**
     * Sets the connection type.
     * 
     * @param connectionType
     *            the new connection type
     */
    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    /**
     * Sets the data source.
     * 
     * @param dataSource
     *            the new data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets the single instance of ConnectionFactory.
     * 
     * @return single instance of ConnectionFactory
     */
    static ConnectionFactory getInstance() {
        synchronized (ConnectionFactory.class) {
            if (instance == null) {
                instance = new ConnectionFactory();
            }
        }
        return instance;
    }
}
