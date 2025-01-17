package de.cxp.ocs.suggest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import de.cxp.ocs.api.SuggestService;
import de.cxp.ocs.model.suggest.Suggestion;
import de.cxp.ocs.smartsuggest.QuerySuggestManager;
import de.cxp.ocs.smartsuggest.querysuggester.QuerySuggester;
import de.cxp.ocs.smartsuggest.spi.CommonPayloadFields;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SuggestServiceImpl implements SuggestService {

	private final QuerySuggestManager querySuggestManager;

	private final LoadingCache<String, Boolean> deletionCache;

	public SuggestServiceImpl(QuerySuggestManager suggestManager, SuggestProperties properties) {
		querySuggestManager = suggestManager;
		deletionCache = CacheBuilder.newBuilder()
				.expireAfterAccess(properties.getSuggesterMaxIdleMinutes(), TimeUnit.MINUTES)
				.removalListener(new RemovalListener<String, Boolean>() {

					@Override
					public void onRemoval(RemovalNotification<String, Boolean> notification) {
						log.info("shutting down suggester for index {}", notification.getKey());
						querySuggestManager.destroyQuerySuggester(notification.getKey());
					}
				})
				.build(new CacheLoader<String, Boolean>() {

					@Override
					public Boolean load(String key) throws Exception {
						return Boolean.TRUE;
					}
				});
	}

	@Override
	public List<Suggestion> suggest(String indexName, String userQuery, Integer limit, String filter) throws Exception {
		deletionCache.getUnchecked(indexName);
		QuerySuggester qm = querySuggestManager.getQuerySuggester(indexName, false);

		Set<String> tagsFilter;
		if (filter != null && !filter.isEmpty()) {
			tagsFilter = new HashSet<>(Arrays.asList(filter.split(",")));
		}
		else {
			tagsFilter = Collections.emptySet();
		}

		return qm.suggest(userQuery, limit, tagsFilter)
				.stream()
				.map(this::mapToSuggestionModel)
				.collect(Collectors.toList());
	}

	/**
	 * map internal "Suggestion"
	 * (de.cxp.ocs.smartsuggest.querysuggester.Suggestion) to external
	 * "Suggestion" (de.cxp.ocs.model.suggest.Suggestion.Suggestion)
	 * 
	 * @param suggestion
	 * @return
	 */
	private Suggestion mapToSuggestionModel(de.cxp.ocs.smartsuggest.querysuggester.Suggestion suggestion) {
		Suggestion mappedSuggestion = new Suggestion(suggestion.getLabel());

		if (suggestion.getPayload() != null) {
			suggestion.getPayload().remove(CommonPayloadFields.PAYLOAD_LABEL_KEY);
			String type = suggestion.getPayload().remove(CommonPayloadFields.PAYLOAD_TYPE_KEY);
			if (type != null) {
				mappedSuggestion.setType(type);
			}
			suggestion.getPayload().putIfAbsent(CommonPayloadFields.PAYLOAD_WEIGHT_KEY, String.valueOf(suggestion.getWeight()));
			mappedSuggestion.setPayload(suggestion.getPayload());
		}
		else {
			mappedSuggestion.setPayload(Collections.singletonMap(CommonPayloadFields.PAYLOAD_WEIGHT_KEY, String.valueOf(suggestion.getWeight())));
		}

		return mappedSuggestion;
	}

}
