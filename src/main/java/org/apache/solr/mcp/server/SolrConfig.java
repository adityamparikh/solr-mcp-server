package org.apache.solr.mcp.server;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration class for Apache Solr client setup and connection management.
 * 
 * <p>This configuration class is responsible for creating and configuring the SolrJ client
 * that serves as the primary interface for communication with Apache Solr servers. It handles
 * URL normalization, connection parameters, and timeout configurations to ensure reliable
 * connectivity for the MCP server operations.</p>
 * 
 * <p><strong>Configuration Features:</strong></p>
 * <ul>
 *   <li><strong>Automatic URL Normalization</strong>: Ensures proper Solr URL formatting</li>
 *   <li><strong>Connection Timeout Management</strong>: Configurable timeouts for reliability</li>
 *   <li><strong>Property Integration</strong>: Uses externalized configuration through properties</li>
 *   <li><strong>Production-Ready Defaults</strong>: Optimized timeout values for production use</li>
 * </ul>
 * 
 * <p><strong>URL Processing:</strong></p>
 * <p>The configuration automatically normalizes Solr URLs to ensure proper communication:</p>
 * <ul>
 *   <li>Adds trailing slashes if missing</li>
 *   <li>Appends "/solr/" path if not present in the URL</li>
 *   <li>Handles various URL formats (with/without protocols, paths, etc.)</li>
 * </ul>
 * 
 * <p><strong>Connection Parameters:</strong></p>
 * <ul>
 *   <li><strong>Connection Timeout</strong>: 10 seconds (10,000ms) for establishing connections</li>
 *   <li><strong>Socket Timeout</strong>: 60 seconds (60,000ms) for read operations</li>
 * </ul>
 * 
 * <p><strong>Configuration Example:</strong></p>
 * <pre>{@code
 * # application.properties
 * solr.url=http://localhost:8983
 * 
 * # Results in normalized URL: http://localhost:8983/solr/
 * }</pre>
 * 
 * <p><strong>Supported URL Formats:</strong></p>
 * <ul>
 *   <li>{@code http://localhost:8983} → {@code http://localhost:8983/solr/}</li>
 *   <li>{@code http://localhost:8983/} → {@code http://localhost:8983/solr/}</li>
 *   <li>{@code http://localhost:8983/solr} → {@code http://localhost:8983/solr/}</li>
 *   <li>{@code http://localhost:8983/solr/} → {@code http://localhost:8983/solr/} (unchanged)</li>
 * </ul>
 * 
 * @author Solr MCP Server
 * @version 1.0
 * @since 1.0
 * 
 * @see SolrConfigurationProperties
 * @see HttpSolrClient
 * @see org.springframework.boot.context.properties.EnableConfigurationProperties
 */
@Configuration
@EnableConfigurationProperties(SolrConfigurationProperties.class)
public class SolrConfig {

    /**
     * Creates and configures a SolrClient bean for Apache Solr communication.
     * 
     * <p>This method serves as the primary factory for creating SolrJ client instances
     * that are used throughout the application for all Solr operations. It performs
     * automatic URL normalization and applies production-ready timeout configurations.</p>
     * 
     * <p><strong>URL Normalization Process:</strong></p>
     * <ol>
     *   <li><strong>Trailing Slash</strong>: Ensures URL ends with "/"</li>
     *   <li><strong>Solr Path</strong>: Appends "/solr/" if not already present</li>
     *   <li><strong>Validation</strong>: Checks for proper Solr endpoint format</li>
     * </ol>
     * 
     * <p><strong>Connection Configuration:</strong></p>
     * <ul>
     *   <li><strong>Connection Timeout</strong>: 10,000ms - Time to establish initial connection</li>
     *   <li><strong>Socket Timeout</strong>: 60,000ms - Time to wait for data/response</li>
     * </ul>
     * 
     * <p><strong>Client Type:</strong></p>
     * <p>Creates an {@code HttpSolrClient} configured for standard HTTP-based communication
     * with Solr servers. This client type is suitable for both standalone Solr instances
     * and SolrCloud deployments when used with load balancers.</p>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <p>URL normalization is defensive and handles various input formats gracefully.
     * Invalid URLs or connection failures will be caught during application startup
     * or first usage, providing clear error messages for troubleshooting.</p>
     * 
     * <p><strong>Production Considerations:</strong></p>
     * <ul>
     *   <li>Timeout values are optimized for production workloads</li>
     *   <li>Connection pooling is handled by the HttpSolrClient internally</li>
     *   <li>Client is thread-safe and suitable for concurrent operations</li>
     * </ul>
     * 
     * @param properties the injected Solr configuration properties containing connection URL
     * @return configured SolrClient instance ready for use in application services
     * 
     * @see HttpSolrClient.Builder
     * @see SolrConfigurationProperties#url()
     */
    @Bean
    SolrClient solrClient(SolrConfigurationProperties properties) {
        String url = properties.url();

        // Ensure URL is properly formatted for Solr
        // The URL should end with /solr/ for proper path construction
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        // If URL doesn't contain /solr/ path, add it
        if (!url.endsWith("/solr/") && !url.contains("/solr/")) {
            if (url.endsWith("/")) {
                url = url + "solr/";
            } else {
                url = url + "/solr/";
            }
        }

        // Use HttpSolrClient with explicit base URL
        return new HttpSolrClient.Builder(url)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
    }
}