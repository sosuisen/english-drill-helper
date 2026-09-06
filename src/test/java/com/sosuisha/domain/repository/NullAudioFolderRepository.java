package com.sosuisha.domain.repository;

import java.util.List;

import com.sosuisha.domain.model.AudioFolder;

/**
 * Repository of audio folders that saves nothing and finds nothing. For tests
 * that need an {@link AudioFolderRepository} but do not care about the
 * folders, and as a base of test spies that record one call.
 */
public class NullAudioFolderRepository implements AudioFolderRepository {
    @Override
    public void save(AudioFolder folder) {
        // does nothing
    }

    @Override
    public List<AudioFolder> findAll() {
        return List.of();
    }
}
