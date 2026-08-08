package moaon.backend.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Spring Boot의 ES 자동구성(ElasticsearchRestClientAutoConfiguration, ElasticsearchClientAutoConfiguration)은
 * ElasticsearchClient/ElasticsearchOperations를 하나만 만들어 connect/socket timeout도 전역으로 하나만 적용된다.
 * 조회(query) 경로는 ES 장애 시 API가 물리지 않도록 짧게 실패해야 하고, 벌크/백그라운드 색인 경로는
 * 오래 걸리는 게 정상이라 timeout을 분리한다. 자동구성은 BackendApplication에서 제외했다.
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchClientConfig {

    public static final String BULK_CLIENT = "bulkElasticsearchClient";
    public static final String BULK_OPERATIONS = "bulkElasticsearchOperations";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    // 커넥션 풀에서 커넥션을 빌리는 대기시간. 기본값(무제한)으로 두면 풀이 소진됐을 때
    // 스레드가 예외/로그 없이 무한 대기하게 되므로 반드시 명시한다.
    private static final Duration CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration QUERY_SOCKET_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration BULK_SOCKET_TIMEOUT = Duration.ofSeconds(120);

    private final ElasticsearchProperties properties;

    public ElasticsearchClientConfig(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        return buildClient(QUERY_SOCKET_TIMEOUT);
    }

    @Bean(BULK_CLIENT)
    public ElasticsearchClient bulkElasticsearchClient() {
        return buildClient(BULK_SOCKET_TIMEOUT);
    }

    @Bean
    @Primary
    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchConverter converter) {
        return new ElasticsearchTemplate(elasticsearchClient(), converter);
    }

    @Bean(BULK_OPERATIONS)
    public ElasticsearchOperations bulkElasticsearchOperations(ElasticsearchConverter converter) {
        return new ElasticsearchTemplate(bulkElasticsearchClient(), converter);
    }

    private ElasticsearchClient buildClient(Duration socketTimeout) {
        HttpHost[] hosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClient restClient = RestClient.builder(hosts)
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    if (StringUtils.hasText(properties.getUsername())) {
                        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    return httpClientBuilder;
                })
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout((int) CONNECT_TIMEOUT.toMillis())
                        .setConnectionRequestTimeout((int) CONNECTION_REQUEST_TIMEOUT.toMillis())
                        .setSocketTimeout((int) socketTimeout.toMillis()))
                .build();

        JsonpMapper mapper = new JacksonJsonpMapper();
        RestClientTransport transport = new RestClientTransport(restClient, mapper);
        return new ElasticsearchClient(transport);
    }
}
