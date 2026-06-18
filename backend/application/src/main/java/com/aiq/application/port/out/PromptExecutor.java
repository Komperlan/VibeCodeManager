package com.aiq.application.port.out;

import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.domain.execution.ExecutionResult;

public interface PromptExecutor {

    String buildCommand(PromptExecutionRequest request);

    ExecutionResult execute(PromptExecutionRequest request);
}
