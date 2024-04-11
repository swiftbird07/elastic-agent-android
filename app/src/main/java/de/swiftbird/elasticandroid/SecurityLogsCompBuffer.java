package de.swiftbird.elasticandroid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for handling the storage and retrieval of {@link SecurityLogsCompDocument} objects,
 * which represent security log events collected by the Android operating system.
 *
 * <p></p>Functionality is similar to LocationCompBuffer.java so we see the same methods there.</p>
 */
@Dao
public interface SecurityLogsCompBuffer {
    @Insert
    void insertDocument(SecurityLogsCompDocument document);

    @Query("SELECT * FROM SecurityLogsCompDocument ORDER BY timestamp ASC")
    List<SecurityLogsCompDocument> getAllDocuments();

    @Query("DELETE FROM SecurityLogsCompDocument")
    void deleteAllDocuments();

    // Get X oldest documents
    @Query("SELECT * FROM SecurityLogsCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments")
    List<SecurityLogsCompDocument> getOldestDocuments(int maxDocuments);

    // Count the number of documents in the buffer, return 0 if no documents
    @Query("SELECT COUNT(*) FROM SecurityLogsCompDocument")
    int getDocumentCount();

    // Delete the oldest X documents

    @Query("DELETE FROM SecurityLogsCompDocument WHERE id IN (SELECT id FROM SecurityLogsCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments)")
    void deleteOldestDocuments(int maxDocuments);


}

