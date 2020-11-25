package de.cxp.ocs.elasticsearch.facets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import de.cxp.ocs.config.FacetConfiguration.FacetConfig;
import de.cxp.ocs.config.FieldConstants;
import de.cxp.ocs.elasticsearch.query.filter.InternalResultFilter;
import de.cxp.ocs.model.result.Facet;
import de.cxp.ocs.model.result.FacetEntry;
import de.cxp.ocs.model.result.HierarchialFacetEntry;
import de.cxp.ocs.util.InternalSearchParams;
import de.cxp.ocs.util.SearchQueryBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class CategoryFacetCreator implements FacetCreator {

	private static final String AGGREGATION_NAME_PREFIX = "_category";

	private final FacetConfig	categoryFacetConfig;
	private final String		aggregationName;
	private final String		categoryFieldName;

	private NestedFacetCountCorrector nestedFacetCorrector;

	@Setter
	private int maxFacetValues = 250;

	public CategoryFacetCreator(FacetConfig categoryFacetConfig) {
		this.categoryFacetConfig = categoryFacetConfig;
		categoryFieldName = categoryFacetConfig.getSourceField();
		aggregationName = AGGREGATION_NAME_PREFIX + "_" + categoryFieldName;
		nestedFacetCorrector = new NestedFacetCountCorrector(FieldConstants.PATH_FACET_DATA + ".value");
	}

	@Override
	public AbstractAggregationBuilder<?> buildAggregation(InternalSearchParams parameters) {
		// other than the TermFacetCreator, the CategoryFacetCreator does the
		// aggregation on a specific "field", this is why a filter is used here
		// this could be changed in case category-type fields would be used more
		// generic
		TermsAggregationBuilder valuesAgg = AggregationBuilders.terms("_values")
				.field(FieldConstants.PATH_FACET_DATA + ".value")
				.size(maxFacetValues)
				.subAggregation(AggregationBuilders.terms("_ids")
						.field(FieldConstants.PATH_FACET_DATA + ".id")
						.size(1));
		nestedFacetCorrector.correctValueAggBuilder(valuesAgg);

		return AggregationBuilders.nested(aggregationName, FieldConstants.PATH_FACET_DATA)
				.subAggregation(
						AggregationBuilders.filter("_field", QueryBuilders.termQuery(FieldConstants.PATH_FACET_DATA + ".name", categoryFieldName))
								.subAggregation(valuesAgg));
	}

	@Override
	public Collection<Facet> createFacets(List<InternalResultFilter> filters, Aggregations aggResult, SearchQueryBuilder linkBuilder) {
		// unwrapping these nested value aggregation
		Terms categoryAgg = ((Filter) ((Nested) aggResult.get(aggregationName)).getAggregations().get("_field")).getAggregations().get("_values");
		List<? extends Bucket> catBuckets = categoryAgg.getBuckets();
		if (catBuckets.size() == 0) return Collections.emptyList();

		Facet facet = FacetFactory.create(categoryFacetConfig, "hierarchical");

		Map<String, HierarchialFacetEntry> entries = new LinkedHashMap<>(catBuckets.size());
		long absDocCount = 0;

		for (Bucket categoryBucket : catBuckets) {
			String categoryPath = categoryBucket.getKeyAsString();

			String[] categories = StringUtils.split(categoryPath, '/');
			// TODO: in case a category is filtered, it might be a good idea to
			// only show the according path
			HierarchialFacetEntry lastLevelEntry = entries.computeIfAbsent(categories[0], c -> toFacetEntry(c, categoryPath, linkBuilder));
			for (int i = 1; i < categories.length; i++) {
				FacetEntry child = getChildByKey(lastLevelEntry, categories[i]);
				if (child != null) {
					lastLevelEntry = (HierarchialFacetEntry) child;
				}
				else {
					HierarchialFacetEntry newChild = toFacetEntry(categories[i], categoryPath, linkBuilder);
					lastLevelEntry.addChild(newChild);
					lastLevelEntry = newChild;
				}
			}
			long docCount = nestedFacetCorrector.getCorrectedDocumentCount(categoryBucket);
			absDocCount += docCount;
			lastLevelEntry.setDocCount(docCount);
			lastLevelEntry.setPath(categoryPath);
			
			Terms idsAgg = (Terms) categoryBucket.getAggregations().get("_ids");
			if (idsAgg != null && idsAgg.getBuckets().size() > 0) {
				lastLevelEntry.setId(idsAgg.getBuckets().get(0).getKeyAsString());
			}
		}
		facet.setAbsoluteFacetCoverage(absDocCount);
		entries.values().forEach(facet.getEntries()::add);
		return Arrays.asList(facet);
	}

	private FacetEntry getChildByKey(HierarchialFacetEntry entry, String childKey) {
		for (FacetEntry e : entry.children) {
			if (childKey.equals(e.getKey())) {
				return e;
			}
		}
		return null;
	}

	private HierarchialFacetEntry toFacetEntry(String value, String categoryPath, SearchQueryBuilder linkBuilder) {
		return new HierarchialFacetEntry(value, null, 0,
				linkBuilder.withFilterAsLink(categoryFacetConfig, categoryPath),
				linkBuilder.isFilterSelected(categoryFacetConfig, categoryPath));
	}

}
