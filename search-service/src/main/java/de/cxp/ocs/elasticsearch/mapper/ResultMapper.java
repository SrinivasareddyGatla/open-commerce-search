package de.cxp.ocs.elasticsearch.mapper;

import static com.google.common.base.Predicates.instanceOf;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;

import de.cxp.ocs.config.Field;
import de.cxp.ocs.config.FieldConfigIndex;
import de.cxp.ocs.config.FieldConstants;
import de.cxp.ocs.model.index.Document;
import de.cxp.ocs.model.index.Product;
import de.cxp.ocs.model.result.ResultHit;

public class ResultMapper {

	public static ResultHit mapSearchHit(SearchHit hit, Map<String, SortOrder> sortedFields) {
		return mapSearchHit(hit, sortedFields, VariantPickingStrategy.pickIfSingleHit);
	}

	/**
	 * Extract a result document from the search-hit. This includes some extra
	 * logic for variant handling:
	 * <ul>
	 * <li>If sortedFields are given, then documents that have several values at
	 * a sort field will get a "${fieldname}_prefix" field with the value
	 * '{from}' (for asc order) or '{to}' (for desc order). Also according to
	 * the sort order the according lowest or highest value is put into the
	 * according field.<br>
	 * Example: With this behavior it is possible to show "from 10€" (for
	 * asc price sorting) or "to 59€" (for desc price sorting) for products that
	 * have several variants with different prices.
	 * </li>
	 * <li>
	 * The result-data of a variant is put into the document if one of the
	 * following conditions met:<br>
	 * - the search hit only contains 1 variant<br>
	 * - the first variant has a better matching score than the second
	 * variant<br>
	 * - the 'preferVariantHits' flag is true
	 * </li>
	 * </ul>
	 * 
	 * @param hit
	 *        ES search hit
	 * @param sortedFields
	 *        map with field-to-sortOrder entries applicable on variant level
	 *        (no validation done here)
	 * @param variantPickingStrategy
	 *        function to pick variant in case of inner variant hits
	 * @return mapped result hit
	 */
	public static ResultHit mapSearchHit(SearchHit hit, Map<String, SortOrder> sortedFields, VariantPickingStrategy variantPickingStrategy) {
		SearchHits variantHits = hit.getInnerHits() == null ? null : hit.getInnerHits().get("variants");

		int allVariantHitCount = 0;
		if (variantPickingStrategy.isAllVariantHitCountRequired() && hit.getInnerHits() != null && hit.getInnerHits().get("_all") != null) {
			allVariantHitCount = (int) hit.getInnerHits().get("_all").getTotalHits().value;
		}

		SearchHit variantHit = null;
		if (variantHits != null && variantHits.getHits().length > 0) {
			variantHit = variantPickingStrategy.pick(variantHits, allVariantHitCount);
		}

		ResultHit resultHit = new ResultHit()
				.setDocument(getResultDocument(hit, variantHit))
				.setIndex(hit.getIndex())
				.setMatchedQueries(hit.getMatchedQueries());

		addSortFieldPrefix(hit, resultHit, sortedFields);
		return resultHit;
	}

	/**
	 * If we sort by a numeric value (e.g. price) and there are several
	 * different values at a product for that given field (e.g. multiple prices
	 * from the variants), then add a prefix "from" or "to" depending on sort
	 * order.
	 * 
	 * The goal is to show "from 10€" if sorted by price ascending and "to 59€"
	 * if sorted by price descending.
	 * 
	 * @param hit
	 *        ES search hit
	 * @param resultHit
	 *        according OCS mapped result hit
	 * @param sortedFields
	 *        map of sort fields
	 */
	@SuppressWarnings("unchecked")
	private static void addSortFieldPrefix(SearchHit hit, ResultHit resultHit, Map<String, SortOrder> sortedFields) {
		Map<String, Object> source = hit.getSourceAsMap();
		if (source == null) return;

		Object sortData = source.get(FieldConstants.SORT_DATA);
		if (sortData != null && sortData instanceof Map && sortedFields.size() > 0) {
			sortedFields.forEach((fieldName, order) -> {
				Map<String, String> fieldsToChange = new HashMap<String, String>();
				resultHit.document.getData().computeIfPresent(fieldName, (fn, v) -> {
					Object fieldSortData = ((Map<String, Object>) sortData).get(fn);
					if (fieldSortData != null && fieldSortData instanceof Collection
							&& ((Collection<?>) fieldSortData).size() > 1) {
						fieldsToChange.put(fn + "_prefix", SortOrder.ASC.equals(order) ? "{from}" : "{to}");
						// collection is already sorted asc/desc: first value is
						// the relevant one
						return ((Collection<?>) fieldSortData).iterator().next();
					}
					return v;
				});
				fieldsToChange.forEach((field, value) -> resultHit.document.set(field, value));
			});
		}
	}

	private static Document getResultDocument(SearchHit hit, SearchHit variantHit) {
		Map<String, Object> source = hit.getSourceAsMap();
		Document document = new Document(hit.getId());

		if (source != null) {
			putDataIntoResult(hit.getSourceAsMap(), document.getData(), FieldConstants.RESULT_DATA);
			if (variantHit != null) {
				putDataIntoResult(variantHit.getSourceAsMap(), document.getData(), FieldConstants.RESULT_DATA);
			}
		}

		return document;
	}

	@SuppressWarnings("unchecked")
	private static void putDataIntoResult(Map<String, Object> source, Map<String, Object> resultFields, String sourceDataField) {
		Object sourceData = source.get(sourceDataField);
		if (sourceData != null && sourceData instanceof Map) {
			resultFields.putAll((Map<String, Object>) sourceData);
		}
	}

	@SuppressWarnings("unchecked")
	public static Document mapToOriginalDocument(String id, Map<String, Object> source, FieldConfigIndex fieldConfig) {
		Document mapped;

		Object variantsData = source.get(FieldConstants.VARIANTS);
		if (variantsData != null && isMapList(variantsData, instanceOf(String.class), instanceOf(Object.class))) {
			mapped = new Product(id);
			List<Map<String, Object>> variantSources = (List<Map<String, Object>>) variantsData;
			Document[] variants = new Document[variantSources.size()];
			// TODO: care about proper handling of variant ID
			Optional<Field> variantIdField = fieldConfig.getField("id");
			for (int i = 0; i < variantSources.size(); i++) {
				Document mappedVariant = mapToOriginalDocument(id + "_" + i, variantSources.get(i), fieldConfig);
				variantIdField
						.map(idField -> mappedVariant.data.get(idField.getName()))
						.ifPresent(varId -> mappedVariant.setId(varId.toString()));
				variants[i] = mappedVariant;
			}
			((Product) mapped).setVariants(variants);
		}
		else {
			mapped = new Document(id);
		}

		putDataIntoResult(source, mapped.getData(), FieldConstants.RESULT_DATA);

		return mapped;
	}

	@SuppressWarnings("unchecked")
	private static boolean isMapList(Object data, Predicate<Object> keyPredicate, Predicate<Object> valuePredicate) {
		boolean isWantedMap = data instanceof List
				&& ((List<Object>) data).size() > 0
				&& ((List<Object>) data).get(0) instanceof Map
				&& ((List<Map<?, ?>>) data).get(0).size() > 0;
		if (isWantedMap && keyPredicate != null && valuePredicate != null) {
			Entry<?, ?> mapContent = ((List<Map<?, ?>>) data).get(0).entrySet().iterator().next();
			isWantedMap &= keyPredicate.test(mapContent.getKey()) && valuePredicate.test(mapContent.getValue());
		}
		return isWantedMap;
	}

}