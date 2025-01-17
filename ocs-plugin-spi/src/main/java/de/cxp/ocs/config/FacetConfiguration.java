package de.cxp.ocs.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter // write setters with java-doc!
@NoArgsConstructor
public class FacetConfiguration {

	private FacetConfig defaultFacetConfiguration = new FacetConfig();

	private List<FacetConfig> facets = new ArrayList<>();

	private int maxFacets = 5;

	/**
	 * A list of fine grained facet configurations. Each facet configuration
	 * controls the return value of one specific facet.
	 * Facets without configuration will be configured by default values.
	 * 
	 * @param facets
	 *        set full facets list
	 * @return self
	 */
	public FacetConfiguration setFacets(@NonNull List<FacetConfig> facets) {
		this.facets = facets;
		return this;
	}

	/**
	 * Limit the amount of all facets returned for a result. Facets that have
	 * the property 'excludeFromFacetLimit' enabled, won't be considered for
	 * that limit.
	 * 
	 * @param maxFacets
	 *        set facet limit
	 * @return self
	 */
	public FacetConfiguration setMaxFacets(int maxFacets) {
		this.maxFacets = maxFacets;
		return this;
	}

	public FacetConfiguration setDefaultFacetConfiguration(de.cxp.ocs.config.FacetConfiguration.FacetConfig defaultFacetConfiguration) {
		this.defaultFacetConfiguration = defaultFacetConfiguration;
		return this;
	}

	@Getter // write setters with java-doc!
	@NoArgsConstructor
	@RequiredArgsConstructor
	public static class FacetConfig {

		@NonNull
		private String label;

		@NonNull
		private String sourceField;

		private String type;

		private Map<String, Object> metaData = new HashMap<>();

		private int optimalValueCount = 5;

		private boolean showUnselectedOptions = false;

		private boolean isMultiSelect = false;

		private int order = 1000;

		private ValueOrder valueOrder = ValueOrder.COUNT;

		private boolean excludeFromFacetLimit = false;

		private boolean preferVariantOnFilter = false;

		/**
		 * Label of that facet
		 * 
		 * @param label
		 *        label to set
		 * @return self
		 */
		public FacetConfig setLabel(String label) {
			this.label = label;
			return this;
		}

		/**
		 * Required: Set name of data field that is configured with these
		 * config.
		 * 
		 * @param sourceField
		 *        set field name this facet relates to
		 * @return self
		 */
		public FacetConfig setSourceField(String sourceField) {
			this.sourceField = sourceField;
			return this;
		}

		/**
		 * <p>
		 * Optional type that relates to the available FacetCreators.
		 * If not set, it uses the default type of the related field.
		 * </p>
		 * From some field-types different facet types can be generated:
		 * <ul>
		 * <li>numeric fields generate "interval" facets per default, but can be
		 * set to "range"</li>
		 * <li>TODO: custom facet creators can support their own facet
		 * types</li>
		 * </ul>
		 * <p>
		 * If set to 'ignore' the facet creation is avoided, even if that facet
		 * is indexed.
		 * </p>
		 * 
		 * @param type
		 *        type of facet
		 * @return self
		 */
		public FacetConfig setType(String type) {
			this.type = type;
			return this;
		}

		/**
		 * Optional map that is returned with that facet. Can be used for
		 * additional data you need with the facet for visualizing.
		 * 
		 * @param metaData
		 *        arbitrary data map
		 * @return self
		 */
		public FacetConfig setMetaData(Map<String, Object> metaData) {
			this.metaData = metaData;
			return this;
		}

		/**
		 * Primary used for numeric facets to build according number of value
		 * ranges.
		 * Can also be used for advanced displaying term facets.
		 * 
		 * @param optimalValueCount
		 *        this is a number
		 * @return self
		 */
		public FacetConfig setOptimalValueCount(int optimalValueCount) {
			this.optimalValueCount = optimalValueCount;
			return this;
		}

		/**
		 * Set to true if all options should be shown after filtering on one of
		 * the options of the same facet.
		 * 
		 * @param showUnselectedOptions
		 *        set true to activate
		 * @return self
		 */
		public FacetConfig setShowUnselectedOptions(boolean showUnselectedOptions) {
			this.showUnselectedOptions = showUnselectedOptions;
			return this;
		}

		/**
		 * Set to true if it should be possible to select several different
		 * values of the same facet.
		 * 
		 * @param isMultiSelect
		 *        set true to activate
		 * @return self
		 */
		public FacetConfig setMultiSelect(boolean isMultiSelect) {
			this.isMultiSelect = isMultiSelect;
			return this;
		}

		public FacetConfig setIsMultiSelect(boolean isMultiSelect) {
			this.isMultiSelect = isMultiSelect;
			return this;
		}

		/**
		 * Optional index, to put the facets in a consistent order.
		 * 
		 * @param order
		 *        numeric value between 0 and 127
		 * @return self
		 */
		public FacetConfig setOrder(int order) {
			this.order = order;
			return this;
		}

		/**
		 * If set to true, this facet will always be shown and not removed
		 * because of facet limit.
		 * 
		 * @param excludeFromFacetLimit
		 *        set true to activate
		 * @return self
		 */
		public FacetConfig setExcludeFromFacetLimit(boolean excludeFromFacetLimit) {
			this.excludeFromFacetLimit = excludeFromFacetLimit;
			return this;
		}

		/**
		 * <p>
		 * Set to true, if variant documents should be preferred in the
		 * result in case a filter of that facet/field is used. This can only
		 * be used for facets/fields, that exist on variant level, otherwise it
		 * is ignored.
		 * </p>
		 * <p>
		 * If several facets have this flag activated, one of them must be
		 * filtered to prefer a variant. E.g. if you have different variants per
		 * "color" and "material", and you set this flag for both facets,
		 * variants will be shown if there is either a color or a material
		 * filter.
		 * </p>
		 * 
		 * @param preferVariantOnFilter
		 *        default is false. set to true to activate.
		 * @return self
		 */
		public FacetConfig setPreferVariantOnFilter(boolean preferVariantOnFilter) {
			this.preferVariantOnFilter = preferVariantOnFilter;
			return this;
		}

		/**
		 * <p>
		 * Set the order of the facet values. Defaults to COUNT which means, the
		 * value with the highest result coverage will be listed first.
		 * </p>
		 * 
		 * <p>
		 * This setting is only used for term-facets and category-facets.
		 * </p>
		 * 
		 * @param valueOrder
		 *        order of the values for that facet
		 * @return self
		 */
		public FacetConfig setValueOrder(ValueOrder valueOrder) {
			this.valueOrder = valueOrder;
			return this;
		}

		public static enum ValueOrder {
			COUNT, ALPHANUM_ASC, ALPHANUM_DESC;
		}
	}
}
