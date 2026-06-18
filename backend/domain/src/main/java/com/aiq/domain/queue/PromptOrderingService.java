package com.aiq.domain.queue;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PromptOrderingService {

    private static final Comparator<Prompt> ORDERING = Comparator
        .comparingInt(Prompt::getPriority)
        .reversed()
        .thenComparingLong(Prompt::getPosition)
        .thenComparing(Prompt::getCreatedAt);

    public List<Prompt> order(Collection<Prompt> prompts) {
        return prompts.stream()
            .sorted(ORDERING)
            .toList();
    }
}
