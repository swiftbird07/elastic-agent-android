package de.swiftbird.elasticandroid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for handling the storage and retrieval of {@link LocationCompDocument} objects,
 * which represent network log events collected by the application.
 */
@Dao
public interface LocationCompBuffer {

    /**
     * Inserts a single location document into the database.
     *
     * @param document The location document to insert.
     */
    @Insert
    void insertDocument(LocationCompDocument document);

    /**
     * Retrieves all location documents from the database, ordered by their timestamp in ascending order.
     *
     * @return A list of all location documents.
     */
    @Query("SELECT * FROM LocationCompDocument ORDER BY timestamp ASC")
    List<LocationCompDocument> getAllDocuments();

    /**
     * Deletes all location documents from the database.
     */
    @Query("DELETE FROM LocationCompDocument")
    void deleteAllDocuments();

    /**
     * Retrieves the oldest location documents up to a specified limit.
     *
     * @param maxDocuments The maximum number of documents to retrieve.
     * @return A list of the oldest location documents, limited by maxDocuments.
     */
    @Query("SELECT * FROM LocationCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments")
    List<LocationCompDocument> getOldestDocuments(int maxDocuments);

    /**
     * Counts the total number of location documents in the database.
     *
     * @return The count of location documents.
     */
    @Query("SELECT COUNT(*) FROM LocationCompDocument")
    int getDocumentCount();

    /**
     * Deletes the oldest location documents from the database, up to a specified number.
     *
     * @param maxDocuments The maximum number of oldest documents to delete.
     */
    @Query("DELETE FROM LocationCompDocument WHERE id IN (SELECT id FROM LocationCompDocument ORDER BY timestamp ASC LIMIT :maxDocuments)")
    void deleteOldestDocuments(int maxDocuments);


}

