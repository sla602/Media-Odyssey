package com.mo.mediaodyssey.scheduler;

import com.mo.mediaodyssey.entity.Media;
import com.mo.mediaodyssey.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrendingScheduler {

    private final MediaRepository mediaRepository;

    @Autowired
    public TrendingScheduler(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    /**
     * Runs every Sunday at midnight (cron: 0 0 0 * * SUN).
     * Copies the current likes count into likesLastWeek for every media item.
     * This snapshot is then used by getTrendingGrowth() to calculate
     * the weekly growth % shown in the Fast-Rising section.
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    public void snapshotWeeklyLikes() {
        List<Media> allMedia = mediaRepository.findAll();
        for (Media m : allMedia) {
            m.setLikesLastWeek(m.getLikes());
        }
        mediaRepository.saveAll(allMedia);
    }
}