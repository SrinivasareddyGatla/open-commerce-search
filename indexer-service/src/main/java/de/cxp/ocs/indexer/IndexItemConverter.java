package de.cxp.ocs.indexer;

import java.util.Map.Entry;

import de.cxp.ocs.config.FieldConfiguration;
import de.cxp.ocs.indexer.model.DataItem;
import de.cxp.ocs.indexer.model.IndexableItem;
import de.cxp.ocs.indexer.model.MasterItem;
import de.cxp.ocs.indexer.model.VariantItem;
import de.cxp.ocs.model.index.Attribute;
import de.cxp.ocs.model.index.Document;
import de.cxp.ocs.model.index.Product;
import lombok.extern.slf4j.Slf4j;

/**
 * converts {@link Document} / {@link Product} objects into
 * {@link DataItem}
 */
@Slf4j
public class IndexItemConverter {

	private FieldConfigIndex fieldConfigIndex;

	/**
	 * Constructor of the converter that prepares the given field configurations
	 * for converting Documents into {@link IndexableItem}.
	 * 
	 * @param standardFields
	 * @param dynamicFields
	 */
	public IndexItemConverter(FieldConfiguration fieldConfiguration) {
		fieldConfigIndex = new FieldConfigIndex(fieldConfiguration);
	}

	/**
	 * Converts a Document coming in via the REST API into the Indexable Item
	 * for Elasticsearch.
	 * 
	 * @param doc
	 * @return
	 */
	public IndexableItem toIndexableItem(Document doc) {
		// TODO: validate document (e.g. require IDs etc.)
		IndexableItem indexableItem;
		if (doc instanceof Product) {
			indexableItem = toMasterVariantItem((Product) doc);
		}
		else {
			indexableItem = new IndexableItem(doc.getId());
		}

		extractSourceValues(doc, indexableItem);

		return indexableItem;
	}

	private MasterItem toMasterVariantItem(Product doc) {
		MasterItem targetMaster = new MasterItem(doc.getId());

		for (final Document variantDocument : doc.getVariants()) {
			final VariantItem targetVariant = new VariantItem(targetMaster);
			extractSourceValues(variantDocument, targetVariant);
			targetMaster.getVariants().add(targetVariant);
		}
		return targetMaster;
	}

	private void extractSourceValues(Document sourceDoc, final DataItem targetItem) {
		final boolean isVariant = (targetItem instanceof VariantItem);

		if (sourceDoc.getData() != null) {
			for (Entry<String, Object> dataField : sourceDoc.getData().entrySet()) {
				fieldConfigIndex.getMatchingField(dataField.getKey(), dataField.getValue())
						.map(field -> {
							if ((field.isVariantLevel() && !isVariant) || (field.isMasterLevel() && isVariant)) {
								return null;
							}
							else {
								return field;
							}
						})
						.ifPresent(field -> targetItem.setValue(field, dataField.getValue()));
			}
		}

		if (sourceDoc.getAttributes() != null) {
			for (Attribute attribute : sourceDoc.getAttributes()) {
				fieldConfigIndex.getMatchingField(attribute.getLabel(), attribute.getValue())
						.map(field -> {
							if ((field.isVariantLevel() && !isVariant) || (field.isMasterLevel() && isVariant)) {
								return null;
							}
							else {
								return field;
							}
						})
						.ifPresent(field -> targetItem.setValue(field, attribute));
			}
		}

		fieldConfigIndex.getCategoryField().ifPresent(f -> targetItem.setValue(f, sourceDoc.getCategories()));
	}

}
