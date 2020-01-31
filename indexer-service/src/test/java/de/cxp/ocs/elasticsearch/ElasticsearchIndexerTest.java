package de.cxp.ocs.elasticsearch;

import static de.cxp.ocs.config.FieldType.*;
import static de.cxp.ocs.config.FieldUsage.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.cxp.ocs.api.indexer.ImportSession;
import de.cxp.ocs.conf.IndexConfiguration;
import de.cxp.ocs.config.Field;
import de.cxp.ocs.model.index.BulkImportData;
import de.cxp.ocs.model.index.Category;
import de.cxp.ocs.model.index.Document;

public class ElasticsearchIndexerTest {

	ElasticsearchIndexClient mockedIndexClient = mock(ElasticsearchIndexClient.class);

	ElasticsearchIndexer underTest = new ElasticsearchIndexer(getIndexConf(), Collections.emptyList(), mockedIndexClient);

	@BeforeEach
	public void setupDefaultEsIndexClient() throws IOException {
		when(mockedIndexClient.getSettings(any())).thenReturn(Optional.empty());
		when(mockedIndexClient.indexRecords(any(), any())).thenReturn(Optional.empty());
	}

	@Test
	public void testStandardIndexProcess() throws Exception {
		ImportSession importSession = underTest.startImport("test", "de");
		assertEquals(importSession.finalIndexName, "test");
		assertTrue(importSession.temporaryIndexName.endsWith("de"));

		BulkImportData data = new BulkImportData();
		data.setSession(importSession);
		data.setDocuments(new Document[] {
				new Document().setId("1").set("title", "Test 1"),
				new Document().setId("2").set("title", "Test 2")
						.setCategories(Collections.singletonList(new Category[] { new Category("c1", "cat1"), new Category("c2", "cat2") }))
		});
		underTest.add(data);
		verify(mockedIndexClient).indexRecords(argThat(is(importSession.temporaryIndexName)), anyObject());

		underTest.done(importSession);
		verify(mockedIndexClient).updateAlias(importSession.finalIndexName, null, importSession.temporaryIndexName);
	}


	@Test
	public void testImportSessionNeverFinished() {
		when(mockedIndexClient.getAliases("ocs-*-test*")).thenReturn(Collections.singletonMap("ocs-1-test-de", Collections.emptySet()));
		assertThrows(IllegalStateException.class, () -> underTest.startImport("test", "de"));
	}

	private IndexConfiguration getIndexConf() {
		IndexConfiguration config = new IndexConfiguration();
		config.getFieldConfiguration()
				.addField(new Field("id").setType(id).setUsage(Result))
				.addField(new Field("title").setType(string).setUsage(Result, Search))
				.addField(new Field("cagories").setType(category).setUsage(Result, Search, Facet));
		return config;
	}

}