package de.cxp.ocs.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import de.cxp.ocs.client.SearchClient;
import de.cxp.ocs.model.params.SearchQuery;
import de.cxp.ocs.model.result.SearchResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SearchUIService {
	
	SearchClient client = new SearchClient("http://localhost:8534/");

	public SearchResult search(String query, Map<String,String> filters) {
		try {
			log.debug("Perform search with query {} and filters {}", query, filters);
			return client.search("quick-start", new SearchQuery().setQ(query), filters);
		}
		catch (Exception e) {
			log.error("Could not perfrom search, because of: ", e);
		}
		return new SearchResult();
	}

}
