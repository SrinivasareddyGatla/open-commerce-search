package mindshift.search.connector.ocs.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.cxp.ocs.client.models.Facet;
import de.cxp.ocs.client.models.FacetEntry;
import de.cxp.ocs.client.models.ResultHit;
import de.cxp.ocs.client.models.SearchResult;
import de.cxp.ocs.client.models.SearchResultSlice;
import mindshift.search.connector.api.v2.models.ResultItem;
import mindshift.search.connector.api.v2.models.SearchRequest;
import mindshift.search.connector.api.v2.models.TextFacet;
import mindshift.search.connector.api.v2.models.TextFacetValue;

public class SearchResultMapperTest {

	@Test
	public void testDefaultResultMapping() throws JsonParseException, JsonMappingException, IOException {
		InputStream resourceAsStream = SearchResultMapperTest.class.getClassLoader().getResourceAsStream("ocs-search-response.json");
		assumeTrue(resourceAsStream != null);
		
		ObjectMapper jsonMapper = new ObjectMapper();
		SearchResult ocsResult = jsonMapper.readValue(resourceAsStream, SearchResult.class);
		assertNotNull(ocsResult);
		
		SearchRequest request = getSearchRequest(ocsResult);
		mindshift.search.connector.api.v2.models.SearchResult mindResult = new SearchResultMapper(ocsResult, request).toMindshiftResult();
		
		assertEquals("ocs", mindResult.getEngine());
		
		SearchResultSlice resultSlice = ocsResult.getSlices().get(0);
		assertEquals(resultSlice.getMatchCount(), mindResult.getNumFound());
		
		int i = 0;
		for(ResultHit ocsHit : resultSlice.getHits()) {
			ResultItem mindHit = mindResult.getItems().get(i++);
			
			assertEquals(ocsHit.getDocument().getId(), mindHit.getCode());
			assertEquals(ocsHit.getDocument().getData().get("title"), mindHit.getName());
		}
		
		i = 0;
		for (Facet ocsFacet : resultSlice.getFacets()) {
			mindshift.search.connector.api.v2.models.Facet mindFacet = mindResult.getFacets().get(i++);
			
			assertEquals(ocsFacet.getMeta().get("label"), mindFacet.getName());
			
			int k = 0;
			for(FacetEntry ocsFacetEntry : ocsFacet.getEntries()) {
				TextFacetValue mindFacetEntry = ((TextFacet)mindFacet).getValues().get(k++);
				
				assertEquals(ocsFacetEntry.getDocCount(), mindFacetEntry.getCount());
				assertEquals(ocsFacetEntry.getKey(), mindFacetEntry.getName());
			}
		}
	}

	private SearchRequest getSearchRequest(SearchResult ocsResult) {
		String[] ocsRawParams = ocsResult.getInputURI().getQuery().split("\\&");
		Map<String, String> params = new HashMap<>(ocsRawParams.length);
		for (String param : ocsRawParams) {
			String[] paramSplit = param.split("=", 2);
			params.put(paramSplit[0], paramSplit[1]);
		}
		SearchRequest request = new SearchRequest();
		request.setQ(params.remove("q"));
		Optional.ofNullable(params.remove("limit")).map(Integer::parseInt).ifPresent(request::setFetchsize);
		Optional.ofNullable(params.remove("offest")).map(Long::parseLong).ifPresent(request::setOffset);
		Optional.ofNullable(params.remove("sort")).ifPresent(request::setSort);
		params.remove("withFacets");
		params.forEach((k, v) -> request.putFiltersItem(k, v)); // ^^ 
		return request;
	}
}