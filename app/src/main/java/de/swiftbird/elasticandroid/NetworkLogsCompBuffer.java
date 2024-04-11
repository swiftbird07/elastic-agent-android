package de.swiftbird.elasticandroid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for handling the storage and retrieval of {@link NetworkLogsCompDocument} objects,
 * which represent network log events collected by the Android operating system.
 *
 * <p>Functionality is similar to LocationCompBuffer.java so refer to that file for more details.</p>
 */
@Dao
public interface NetworkLogsCompBuffer {
    @Insert
    void insertDocument(NetworkLogsCompDocument document);

    @Query("SELECT * FROM NetworkLogsCompDocument ORDER BY timestamp ASC")
    List<NetworkLogsCompDocument> getAllDocuments();

    @Query("DELETE FROM NetworkLogsCompDocument")
    void deleteAllDocuments();

    @Query("SELECT * FROM NetworkLogsCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments")
    List<NetworkLogsCompDocument> getOldestDocuments(int maxDocuments);

    @Query("SELECT COUNT(*) FROM NetworkLogsCompDocument")
    int getDocumentCount();

    @Query("DELETE FROM NetworkLogsCompDocument WHERE id IN (SELECT id FROM NetworkLogsCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments)")
    void deleteOldestDocuments(int maxDocuments);
}

