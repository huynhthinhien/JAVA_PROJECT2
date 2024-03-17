package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class ParallelCrawler extends RecursiveTask<Boolean> {
    private String url;
    private Instant deadlineTime;
    private int maxDepth;
    private ConcurrentMap<String, Integer> counts;
    private Set<String> visitedUrls;
    private Clock clock;
    private List<Pattern> ignoredUrls;
    private PageParserFactory parserFactory;

    public ParallelCrawler(String url, Instant deadlineTime, int maxDepth, ConcurrentMap<String, Integer> counts, Set<String> visitedUrls, Clock clock, List<Pattern> ignoredUrls, PageParserFactory parserFactory) {
        this.url = url;
        this.deadlineTime = deadlineTime;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    protected Boolean compute() {
        boolean shouldIgnore = ignoredUrls.stream()
                .noneMatch(pattern -> pattern.matcher(url).matches());
        // check timeout
        if (maxDepth == 0 || clock.instant().isAfter(deadlineTime) || !shouldIgnore || visitedUrls.contains(url)) {
            return false;
        }

        if(!visitedUrls.add(url)) {
            return false;
        }
//        visitedUrls.add(url);

        PageParser.Result rs = parserFactory.get(url).parse();

        rs.getWordCounts().forEach((key, value) -> counts.merge(key, value, Integer::sum));

        List<ParallelCrawler> subtasks = new ArrayList<>();
        rs.getLinks().forEach(link -> {
            subtasks.add(new ParallelCrawler(link, deadlineTime, maxDepth - 1, counts, visitedUrls, clock, ignoredUrls, parserFactory));
        });

        invokeAll(subtasks);

        return true;
    }

}
