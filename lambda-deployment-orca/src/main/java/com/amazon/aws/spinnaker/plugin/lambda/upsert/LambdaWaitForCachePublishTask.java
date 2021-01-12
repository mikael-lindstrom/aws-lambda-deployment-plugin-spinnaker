/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.amazon.aws.spinnaker.plugin.lambda.upsert;

import com.amazon.aws.spinnaker.plugin.lambda.LambdaStageBaseTask;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaCloudDriverUtils;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaDefinition;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaStageConstants;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.clouddriver.config.CloudDriverConfigurationProperties;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Map;

@Component
public class LambdaWaitForCachePublishTask implements LambdaStageBaseTask {
    private static Logger logger = LoggerFactory.getLogger(LambdaWaitForCachePublishTask.class);

    @Autowired
    CloudDriverConfigurationProperties props;

    @Autowired
    private LambdaCloudDriverUtils utils;

    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        logger.debug("Executing LambdaWaitForCachePublishTask...");
        return waitForCacheUpdate(stage);
    }

    private TaskResult waitForCacheUpdate(@NotNull StageExecution stage) {
        if (stage.getContext().containsKey(LambdaStageConstants.publishVersionUrlKey)) {
            String publishUrl = (String) stage.getContext().get(LambdaStageConstants.publishVersionUrlKey);
            String version = utils.getPublishedVersion(publishUrl);
            for (int i = 0; i < 10; i++) {
                LambdaDefinition lf = utils.findLambda(stage);
                if (lf != null) {
                    Map<String, String> revisions = lf.getRevisions();
                    if (revisions.containsValue(version)) {
                        return taskComplete(stage);
                    }
                }
                utils.await(10000);
            }
        }
        return this.formErrorTaskResult(stage, "Failed to update cache after PublishVersionTask");
    }
}
