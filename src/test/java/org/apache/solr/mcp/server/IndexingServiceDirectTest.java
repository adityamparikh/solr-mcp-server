package org.apache.solr.mcp.server;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexingServiceDirectTest {

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrConfigurationProperties solrConfigurationProperties;

    @Mock
    private UpdateResponse updateResponse;

    private IndexingService indexingService;

    @BeforeEach
    void setUp() {
        indexingService = new IndexingService(solrClient, solrConfigurationProperties);
    }

    @Test
    void testBatchIndexingErrorHandling() throws Exception {
        // Create a list of test documents
        List<SolrInputDocument> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "test" + i);
            doc.addField("title", "Test Document " + i);
            documents.add(doc);
        }

        // Mock behavior: Batch add fails, but individual adds succeed
        when(solrClient.add(anyString(), anyList()))
                .thenThrow(new RuntimeException("Batch indexing failed"));

        // Individual document adds should succeed
        when(solrClient.add(anyString(), any(SolrInputDocument.class)))
                .thenReturn(updateResponse);

        // Call the method under test
        int successCount = indexingService.indexDocuments("test_collection", documents);

        // Verify the results
        assertEquals(10, successCount, "All documents should be successfully indexed individually");

        // Verify that batch add was attempted once
        verify(solrClient, times(1)).add(eq("test_collection"), anyList());

        // Verify that individual adds were attempted for each document
        verify(solrClient, times(10)).add(eq("test_collection"), any(SolrInputDocument.class));

        // Verify that commit was called
        verify(solrClient, times(1)).commit("test_collection");
    }

    @Test
    void testBatchIndexingPartialFailure() throws Exception {
        // Create a list of test documents
        List<SolrInputDocument> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "test" + i);
            doc.addField("title", "Test Document " + i);
            documents.add(doc);
        }

        // Mock behavior: Batch add fails
        when(solrClient.add(anyString(), anyList()))
                .thenThrow(new RuntimeException("Batch indexing failed"));

        // Even-numbered documents succeed, odd-numbered documents fail
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                when(solrClient.add(eq("test_collection"), eq(documents.get(i))))
                        .thenReturn(updateResponse);
            } else {
                when(solrClient.add(eq("test_collection"), eq(documents.get(i))))
                        .thenThrow(new RuntimeException("Document " + i + " indexing failed"));
            }
        }

        // Call the method under test
        int successCount = indexingService.indexDocuments("test_collection", documents);

        // Verify the results - only even-numbered documents should succeed
        assertEquals(5, successCount, "Only half of the documents should be successfully indexed");

        // Verify that batch add was attempted once
        verify(solrClient, times(1)).add(eq("test_collection"), anyList());

        // Verify that individual adds were attempted for each document
        verify(solrClient, times(10)).add(eq("test_collection"), any(SolrInputDocument.class));

        // Verify that commit was called
        verify(solrClient, times(1)).commit("test_collection");
    }

    @Test
    void testIndexDocumentsWithJsonString() throws Exception {
        // Test JSON string with multiple documents
        String json = """
                [
                  {
                    "id": "test001",
                    "title": "Test Document 1",
                    "content": "This is test content 1"
                  },
                  {
                    "id": "test002",
                    "title": "Test Document 2",
                    "content": "This is test content 2"
                  }
                ]
                """;

        // Create a spy on the indexingService to verify the createSchemalessDocuments method is called
        IndexingService indexingServiceSpy = spy(indexingService);

        // Create mock documents that would be returned by createSchemalessDocuments
        List<SolrInputDocument> mockDocuments = new ArrayList<>();
        SolrInputDocument doc1 = new SolrInputDocument();
        doc1.addField("id", "test001");
        doc1.addField("title", "Test Document 1");
        doc1.addField("content", "This is test content 1");

        SolrInputDocument doc2 = new SolrInputDocument();
        doc2.addField("id", "test002");
        doc2.addField("title", "Test Document 2");
        doc2.addField("content", "This is test content 2");

        mockDocuments.add(doc1);
        mockDocuments.add(doc2);

        // Mock the createSchemalessDocuments method to return our mock documents
        doReturn(mockDocuments).when(indexingServiceSpy).createSchemalessDocuments(json);

        // Mock the indexDocuments method that takes a collection and list of documents
        doReturn(2).when(indexingServiceSpy).indexDocuments(anyString(), anyList());

        // Call the method under test
        indexingServiceSpy.indexDocuments("test_collection", json);

        // Verify that createSchemalessDocuments was called with the JSON string
        verify(indexingServiceSpy, times(1)).createSchemalessDocuments(json);

        // Verify that indexDocuments was called with the collection name and the documents
        verify(indexingServiceSpy, times(1)).indexDocuments("test_collection", mockDocuments);
    }

    @Test
    void testIndexDocumentsWithJsonStringErrorHandling() throws Exception {
        // Test JSON string with invalid format
        String invalidJson = "{ This is not valid JSON }";

        // Create a spy on the indexingService
        IndexingService indexingServiceSpy = spy(indexingService);

        // Mock the createSchemalessDocuments method to throw an exception
        doThrow(new Exception("Invalid JSON")).when(indexingServiceSpy).createSchemalessDocuments(invalidJson);

        // Call the method under test and verify it throws an exception
        Exception exception = assertThrows(Exception.class, () -> {
            indexingServiceSpy.indexDocuments("test_collection", invalidJson);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Invalid JSON"));

        // Verify that createSchemalessDocuments was called
        verify(indexingServiceSpy, times(1)).createSchemalessDocuments(invalidJson);

        // Verify that indexDocuments with documents was not called
        verify(indexingServiceSpy, never()).indexDocuments(anyString(), anyList());
    }
}
