package com.aiq.domain.queue;

import java.util.Collection;
import java.util.Optional;

public class NextPromptSelector {

    private final PromptOrderingService orderingService = new PromptOrderingService();

    public Optional<Prompt> selectNext(Collection<Prompt> prompts) {
        return orderingService.order(prompts).stream()
            .filter(Prompt::isQueued)
            .findFirst();
    }
}
