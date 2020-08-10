package de.cxp.ocs.model.result;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
		discriminatorProperty = "type",
		discriminatorMapping = {
				@DiscriminatorMapping(value = "hierarchical", schema = HierarchialFacetEntry.class),
				@DiscriminatorMapping(value = "simple", schema = FacetEntry.class)
		})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacetEntry {

	public final String type = "simple";

	/**
	 * Associated filter value.
	 */
	public String key;

	/**
	 * Estimated amount of documents that will be returned, if this facet entry
	 * is picked as filter.
	 */
	@Schema(description = "Estimated amount of documents that will be returned, if this facet entry is picked as filter.")
	public long docCount;

	/**
	 * URL conform query parameters, that has to be used to
	 * filter the result.
	 */
	@Schema(format = "URI")
	public String link;

}