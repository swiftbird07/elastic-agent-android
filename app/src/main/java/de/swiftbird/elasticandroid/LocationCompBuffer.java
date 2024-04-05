package de.swiftbird.elasticandroid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationCompBuffer {
    @Insert
    void insertDocument(LocationCompDocument document);

    @Query("SELECT * FROM LocationCompDocument ORDER BY timestamp ASC")
    List<LocationCompDocument> getAllDocuments();

    @Query("DELETE FROM LocationCompDocument")
    void deleteAllDocuments();

    // Get X oldest documents
    @Query("SELECT * FROM LocationCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments")
    List<LocationCompDocument> getOldestDocuments(int maxDocuments);

    // Count the number of documents in the buffer, return 0 if no documents
    @Query("SELECT COUNT(*) FROM LocationCompDocument")
    int getDocumentCount();

    // Delete the oldest X documents

    @Query("DELETE FROM LocationCompDocument WHERE id IN (SELECT id FROM LocationCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments)")
    void deleteOldestDocuments(int maxDocuments);


}

