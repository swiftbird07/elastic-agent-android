package de.swiftbird.elasticandroid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SelfLogCompBuffer {
    @Insert
    void insertDocument(SelfLogCompDocument document);

    @Query("SELECT * FROM SelfLogCompDocument ORDER BY timestamp ASC")
    List<SelfLogCompDocument> getAllDocuments();

    @Query("DELETE FROM SelfLogCompDocument")
    void deleteAllDocuments();

   // Get X oldest documents
    @Query("SELECT * FROM SelfLogCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments")
    List<SelfLogCompDocument> getOldestDocuments(int maxDocuments);

    // Count the number of documents in the buffer, return 0 if no documents
    @Query("SELECT COUNT(*) FROM SelfLogCompDocument")
    int getDocumentCount();

    // Delete the oldest X documents

    @Query("DELETE FROM SelfLogCompDocument WHERE id IN (SELECT id FROM SelfLogCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments)")
    void deleteOldestDocuments(int maxDocuments);


}

