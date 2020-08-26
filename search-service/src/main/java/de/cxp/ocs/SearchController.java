package de.cxp.ocs;

import static de.cxp.ocs.util.SearchParamsParser.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.cxp.ocs.api.searcher.SearchService;
import de.cxp.ocs.config.ApplicationProperties;
import de.cxp.ocs.config.IndexConfiguration;
import de.cxp.ocs.config.SearchConfiguration;
import de.cxp.ocs.config.TenantSearchConfiguration;
import de.cxp.ocs.elasticsearch.ElasticSearchBuilder;
import de.cxp.ocs.elasticsearch.Searcher;
import de.cxp.ocs.model.params.SearchQuery;
import de.cxp.ocs.model.result.SearchResult;
import de.cxp.ocs.util.InternalSearchParams;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@RefreshScope
@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping(path = "/search-api/v1")
@Slf4j
public class SearchController implements SearchService {

	@Autowired
	@NonNull
	private ElasticSearchBuilder esBuilder;

	@Autowired
	@NonNull
	private ApplicationProperties properties;

	@Autowired
	private MeterRegistry registry;

	private final Map<String, SearchConfiguration> searchConfigs = new HashMap<>();

	private final Cache<String, Searcher> searchClientCache = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.maximumSize(10)
			.build();

	@GetMapping("/search/{tenant}")
	@Override
	public SearchResult search(@PathVariable("tenant") String tenant, SearchQuery searchQuery, @RequestParam Map<String, String> filters) throws Exception {
		SearchConfiguration searchConfig = searchConfigs.computeIfAbsent(tenant, this::getConfigForTenant);
		log.debug("Using index {} for tenant {}", searchConfig.getIndexName(), tenant);

		final InternalSearchParams parameters = new InternalSearchParams();
		parameters.userQuery = searchQuery.q;
		parameters.limit = searchQuery.limit;
		parameters.offset = searchQuery.offset;
		if (searchQuery.sort != null) {
			parameters.sortings = parseSortings(searchQuery.sort, searchConfig.getFieldConfiguration().getFields());
		}
		parameters.filters = parseFilters(filters, searchConfig.getFieldConfiguration().getFields());

		final Searcher searcher = searchClientCache.get(tenant, () -> new Searcher(esBuilder.getRestHLClient(), searchConfig, registry));
		SearchResult result = searcher.find(parameters);

		return result;
	}

	@GetMapping("/tenants")
	@Override
	public String[] getTenants() {
		Set<String> tenants = new HashSet<>();
		try {
			esBuilder.getRestHLClient().indices()
					.getAlias(new GetAliasesRequest(), RequestOptions.DEFAULT)
					.getAliases()
					.entrySet()
					.stream()
					.filter(aliasEntry -> !aliasEntry.getKey().startsWith(".") && !aliasEntry.getValue().isEmpty())
					.map(aliasEntry -> aliasEntry.getValue().iterator().next().alias())
					.forEach(tenants::add);
		}
		catch (IOException e) {
			log.warn("could not retrieve ES indices", e);
		}
		return properties.getTenantConfig().keySet().toArray(new String[0]);
	}

	private SearchConfiguration getConfigForTenant(String tenant) {
		SearchConfiguration mergedConfig = new SearchConfiguration();

		TenantSearchConfiguration defaultConfig = properties.getDefaultTenantConfig();
		TenantSearchConfiguration specificConfig = properties.getTenantConfig().get(tenant);

		if (specificConfig != null && specificConfig.getIndexName() != null) {
			mergedConfig.setIndexName(specificConfig.getIndexName());
		}
		else if (defaultConfig.getIndexName() != null) {
			mergedConfig.setIndexName(defaultConfig.getIndexName());
		}
		else {
			mergedConfig.setIndexName(tenant);
		}

		IndexConfiguration indexConfig = getIndexConfiguration(mergedConfig.getIndexName());
		mergedConfig.setFieldConfiguration(indexConfig.getFieldConfiguration());

		if (specificConfig != null && !specificConfig.getFacetConfiguration().getFacets().isEmpty()) {
			// only set specific facet config, if a specific config exists and
			// is not empty
			mergedConfig.setFacetConfiguration(specificConfig.getFacetConfiguration());
		}
		else if (specificConfig == null || !specificConfig.isDisableFacets()) {
			// only set default facet config, if specific config does not exist
			// or if disableFacets is not activated/true
			mergedConfig.setFacetConfiguration(defaultConfig.getFacetConfiguration());
		}

		if (specificConfig != null && !specificConfig.getQueryConfiguration().isEmpty()) {
			mergedConfig.getQueryConfigs().putAll(specificConfig.getQueryConfiguration());
		}
		else if (specificConfig == null || !specificConfig.isDisableQueryConfig()) {
			mergedConfig.getQueryConfigs().putAll(defaultConfig.getQueryConfiguration());
		}

		if (specificConfig != null && !specificConfig.getScoringConfiguration().getScoreFunctions().isEmpty()) {
			mergedConfig.setScoring(specificConfig.getScoringConfiguration());
		}
		else if (specificConfig == null || !specificConfig.isDisableScorings()) {
			mergedConfig.setScoring(defaultConfig.getScoringConfiguration());
		}

		return mergedConfig;
	}

	private IndexConfiguration getIndexConfiguration(String indexName) {
		return properties.getIndexConfig().getOrDefault(indexName, properties.getDefaultIndexConfig());
	}

	@ExceptionHandler({ ExecutionException.class, IOException.class, RuntimeException.class, ClassNotFoundException.class })
	public ResponseEntity<String> handleInternalErrors(Exception e) {
		final String errorId = UUID.randomUUID().toString();
		log.error("Internal Server Error " + errorId, e);
		return new ResponseEntity<>("Something went wrong. Error reference: " + errorId,
				HttpStatus.INTERNAL_SERVER_ERROR);
	}

}