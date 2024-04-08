package de.swiftbird.elasticandroid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for handling the storage and retrieval of {@link NetworkLogsCompDocument} objects,
 * which represent network log events collected by the Android operating system.
 *
 * Functionality is similar to LocationCompBuffer.java so we will not repeat the documentation here.
 */
@Dao
public interface NetworkLogsCompBuffer {
    @Insert
    void insertDocument(NetworkLogsCompDocument document);

    @Query("SELECT * FROM NetworkLogsCompDocument ORDER BY timestamp ASC")
    List<NetworkLogsCompDocument> getAllDocuments();

    @Query("DELETE FROM NetworkLogsCompDocument")
    void deleteAllDocuments();

    // Get X oldest documents
    @Query("SELECT * FROM NetworkLogsCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments")
    List<NetworkLogsCompDocument> getOldestDocuments(int maxDocuments);

    // Count the number of documents in the buffer, return 0 if no documents
    @Query("SELECT COUNT(*) FROM NetworkLogsCompDocument")
    int getDocumentCount();

    // Delete the oldest X documents

    @Query("DELETE FROM NetworkLogsCompDocument WHERE id IN (SELECT id FROM NetworkLogsCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments)")
    void deleteOldestDocuments(int maxDocuments);


}

