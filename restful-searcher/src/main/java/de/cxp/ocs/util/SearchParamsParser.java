package de.cxp.ocs.util;

import static de.cxp.ocs.util.SearchQueryBuilder.*;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import de.cxp.ocs.config.Field;
import de.cxp.ocs.config.FieldUsage;
import de.cxp.ocs.elasticsearch.query.filter.NumberResultFilter;
import de.cxp.ocs.elasticsearch.query.filter.PathResultFilter;
import de.cxp.ocs.elasticsearch.query.filter.TermResultFilter;
import de.cxp.ocs.model.result.SortOrder;
import de.cxp.ocs.model.result.Sorting;

public class SearchParamsParser {

	/**
	 * @throws IllegalArgumentException
	 *         if a parameter has an unexpected value
	 * @param params
	 * @return
	 */
	public static InternalSearchParams parseParams(Map<String, Object> params, final Map<String, Field> fields) {
		final InternalSearchParams searchParams = new InternalSearchParams();
		if (params == null) return searchParams;

		mapParameterIfPresent(params, "limit", value -> searchParams.limit = Integer.parseInt(value));
		mapParameterIfPresent(params, "offset", value -> searchParams.offset = Integer.parseInt(value));
		mapParameterIfPresent(params, "sort", value -> searchParams.sortings.addAll(getSortings(value, fields)));

		for (Entry<String, Object> p : params.entrySet()) {
			String paramName = p.getKey();
			String paramValue = String.valueOf(p.getValue());

			Field field = fields.get(paramName);

			// TODO: facets should be generated dynamically
			// => dynamic field config required
			// => add possibility to filter on such dynamic fields
			if (field != null && field.getUsage().contains(FieldUsage.Facet)) {
				switch (field.getType()) {
					case category:
						searchParams.filters.add(new PathResultFilter(field.getName(), Arrays.asList(split(paramValue, VALUE_DELIMITER))));
						break;
					case number:
						String[] paramValues = splitPreserveAllTokens(paramValue, VALUE_DELIMITER);
						if (paramValues.length != 2) throw new IllegalArgumentException("unexpected numeric filter value: " + paramValue);
						searchParams.filters.add(new NumberResultFilter(
								field.getName(),
								Util.tryToParseAsNumber(paramValues[0]).orElse(null),
								Util.tryToParseAsNumber(paramValues[0]).orElse(null)));
					default:
						searchParams.filters.add(new TermResultFilter(field.getName(), split(paramValue, VALUE_DELIMITER)));
				}
			}
		}

		return searchParams;
	}

	private static void mapParameterIfPresent(Map<String, Object> params, String key, Consumer<String> paramConsumer) {
		Object paramValue = params.get(key);
		if (paramValue != null && paramValue instanceof String) {
			paramConsumer.accept((String) paramValue);
		}
	}

	private static List<Sorting> getSortings(String paramValue, final Map<String, Field> fields) {
		String[] paramValueSplit = split(paramValue, VALUE_DELIMITER);
		List<Sorting> sortings = new ArrayList<>(paramValueSplit.length);
		for (String rawSortValue : paramValueSplit) {
			String fieldName = rawSortValue;
			SortOrder sortOrder = SortOrder.ASC;
			if (rawSortValue.startsWith(SORT_DESC_PREFIX)) {
				fieldName = rawSortValue.substring(1);
				sortOrder = SortOrder.DESC;
			}
			Field fieldConf = fields.get(fieldName);
			if (fieldConf != null && fieldConf.getUsage().contains(FieldUsage.Sort)) {
				sortings.add(new Sorting(fieldName, sortOrder, null));
			}
		}
		return sortings;
	}

}
