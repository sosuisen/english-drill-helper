package com.sosuisha.domain.repository;

import java.util.List;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.AudioFolder;

/**
 * The registered folders of drill audio files, kept in the database of the
 * app in the order of registration.
 */
public interface AudioFolderRepository {
    /**
     * Saves the registered folder. Only one folder is registered for now, so
     * the folder replaces the one registered before.
     *
     * @param folder folder to save
     * @throws RepositoryException if the database cannot be written
     */
    void save(AudioFolder folder) throws RepositoryException;

    /**
     * Returns the registered folders in the order of registration.
     *
     * @return the folders, empty if none is registered
     * @throws RepositoryException if the database cannot be read
     */
    List<AudioFolder> findAll() throws RepositoryException;
}
