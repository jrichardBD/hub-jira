/**
 * Hub JIRA Plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.jira.task.issue.handler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.AssignValidationResult;
import com.atlassian.jira.bc.issue.IssueService.CreateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.bc.issue.IssueService.UpdateValidationResult;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.blackducksoftware.integration.jira.common.HubJiraLogger;
import com.blackducksoftware.integration.jira.common.JiraUserContext;
import com.blackducksoftware.integration.jira.common.exception.JiraException;
import com.blackducksoftware.integration.jira.common.model.PluginField;
import com.blackducksoftware.integration.jira.task.issue.model.BlackDuckIssueFieldTemplate;
import com.blackducksoftware.integration.jira.task.issue.model.JiraIssueFieldTemplate;
import com.blackducksoftware.integration.jira.task.issue.model.JiraIssueWrapper;

public class IssueServiceWrapper {
    private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));

    private final IssueService jiraIssueService;
    private final IssueFieldCopyMappingHandler issueFieldHandler;
    private final JiraUserContext jiraUserContext;
    private final Map<PluginField, CustomField> customFieldsMap;

    public IssueServiceWrapper(final IssueService jiraIssueService, final IssueFieldCopyMappingHandler issueFieldHandler, final JiraUserContext jiraUserContext, final Map<PluginField, CustomField> customFieldsMap) {
        this.jiraIssueService = jiraIssueService;
        this.issueFieldHandler = issueFieldHandler;
        this.jiraUserContext = jiraUserContext;
        this.customFieldsMap = customFieldsMap;
    }

    public Issue getIssue(final Long issueId) throws JiraException {
        final IssueResult result = jiraIssueService.getIssue(jiraUserContext.getJiraIssueCreatorUser(), issueId);
        if (result.isValid()) {
            return result.getIssue();
        }
        throw new JiraException("getIssue", result.getErrorCollection());
    }

    public Issue createIssue(final JiraIssueWrapper jiraIssueWrapper) throws JiraException {
        logger.debug("Create issue: " + jiraIssueWrapper);
        final IssueInputParameters issueInputParameters = createPopulatedIssueInputParameters(jiraIssueWrapper);

        logger.debug("issueInputParameters.getAssigneeId(): " + issueInputParameters.getAssigneeId());
        logger.debug("issueInputParameters.applyDefaultValuesWhenParameterNotProvided(): " + issueInputParameters.applyDefaultValuesWhenParameterNotProvided());
        logger.debug("issueInputParameters.retainExistingValuesWhenParameterNotProvided(): " + issueInputParameters.retainExistingValuesWhenParameterNotProvided());

        // TODO set field copy mappings
        final Map<Long, String> blackDuckFieldMappings = jiraIssueWrapper.getBlackDuckIssueTemplate().getBlackDuckFieldMappings(customFieldsMap);
        final JiraIssueFieldTemplate jiraIssueFieldTemplate = jiraIssueWrapper.getJiraIssueFieldTemplate();
        final List<String> labels = issueFieldHandler.setFieldCopyMappings(issueInputParameters, jiraIssueWrapper.getProjectFieldCopyMappings(), blackDuckFieldMappings,
                jiraIssueFieldTemplate.getJiraProjectName(), jiraIssueFieldTemplate.getJiraProjectId());

        final CreateValidationResult validationResult = jiraIssueService.validateCreate(jiraUserContext.getJiraIssueCreatorUser(), issueInputParameters);
        if (validationResult.isValid()) {
            final IssueResult result = jiraIssueService.create(jiraUserContext.getJiraIssueCreatorUser(), validationResult);
            final ErrorCollection errors = result.getErrorCollection();
            if (!errors.hasAnyErrors()) {
                final MutableIssue jiraIssue = result.getIssue();
                fixIssueAssignment(jiraIssue, jiraIssueWrapper.getJiraIssueFieldTemplate().getAssigneeId());
                issueFieldHandler.addLabels(jiraIssue.getId(), labels);
                return jiraIssue;
            }
            throw new JiraException("createIssue", errors);
        }
        throw new JiraException("createIssue", validationResult.getErrorCollection());
    }

    public Issue updateIssue(final Issue existingIssue, final JiraIssueWrapper jiraIssueWrapper) throws JiraException {
        logger.debug("Update issue (" + existingIssue.getKey() + "): " + jiraIssueWrapper);
        final IssueInputParameters issueInputParameters = createPopulatedIssueInputParameters(jiraIssueWrapper);

        // TODO set field copy mappings

        final UpdateValidationResult validationResult = jiraIssueService.validateUpdate(jiraUserContext.getJiraIssueCreatorUser(), existingIssue.getId(), issueInputParameters);
        if (validationResult.isValid()) {
            final boolean sendMail = false;
            final IssueResult result = jiraIssueService.update(jiraUserContext.getJiraIssueCreatorUser(), validationResult, EventDispatchOption.ISSUE_UPDATED, sendMail);
            final ErrorCollection errors = result.getErrorCollection();
            if (!errors.hasAnyErrors()) {
                final Issue jiraIssue = result.getIssue();
                return jiraIssue;
            }
            throw new JiraException("updateIssue", errors);
        }
        throw new JiraException("updateIssue", validationResult.getErrorCollection());
    }

    public Issue transitionIssue(final Issue existingIssue, final int transitionActionId) throws JiraException {
        logger.debug("Transition issue (" + existingIssue.getKey() + "): " + transitionActionId);
        final IssueInputParameters issueInputParameters = jiraIssueService.newIssueInputParameters();
        issueInputParameters.setRetainExistingValuesWhenParameterNotProvided(true);

        logger.debug("Previous issue status: " + existingIssue.getStatus().getName());
        final TransitionValidationResult validationResult = jiraIssueService.validateTransition(jiraUserContext.getJiraIssueCreatorUser(), existingIssue.getId(), transitionActionId, issueInputParameters);
        if (validationResult.isValid()) {
            final IssueResult result = jiraIssueService.transition(jiraUserContext.getJiraIssueCreatorUser(), validationResult);
            final ErrorCollection errors = result.getErrorCollection();
            if (!errors.hasAnyErrors()) {
                final Issue jiraIssue = result.getIssue();
                logger.debug("New issue status: " + jiraIssue.getStatus().getName());
                return jiraIssue;
            }
            throw new JiraException("transitionIssue", errors);
        }
        throw new JiraException("transitionIssue", validationResult.getErrorCollection());
    }

    private void fixIssueAssignment(final MutableIssue mutableIssue, final String assigneeId) throws JiraException {
        if (mutableIssue.getAssignee() == null) {
            logger.debug("Created issue " + mutableIssue.getKey() + "; Assignee: null");
        } else {
            logger.debug("Created issue " + mutableIssue.getKey() + "; Assignee: " + mutableIssue.getAssignee().getName());
        }
        if (assigneeId == null && mutableIssue.getAssigneeId() != null) {
            logger.debug("Issue needs to be UNassigned");
            assignIssue(mutableIssue, assigneeId);
        } else if (assigneeId != null && !mutableIssue.getAssigneeId().equals(assigneeId)) {
            throw new JiraException("Issue assignment failed", "fixIssueAssignment");
        } else {
            logger.debug("Issue assignment is correct");
        }
    }

    private void assignIssue(final MutableIssue mutableIssue, final String assigneeId) throws JiraException {
        final ApplicationUser issueCreator = jiraUserContext.getJiraIssueCreatorUser();
        final AssignValidationResult assignValidationResult = jiraIssueService.validateAssign(jiraUserContext.getJiraIssueCreatorUser(), mutableIssue.getId(), assigneeId);
        final ErrorCollection errors = assignValidationResult.getErrorCollection();
        if (assignValidationResult.isValid() && !errors.hasAnyErrors()) {
            logger.debug("Assigning issue to user ID: " + assigneeId);
            jiraIssueService.assign(issueCreator, assignValidationResult);
        } else {
            final StringBuilder errorMessageBuilder = new StringBuilder("Unable to assign issue ");
            errorMessageBuilder.append(mutableIssue.getKey());
            errorMessageBuilder.append(": ");
            for (final String errorMsg : errors.getErrorMessages()) {
                errorMessageBuilder.append(errorMsg);
                errorMessageBuilder.append("; ");
            }
            throw new JiraException(errorMessageBuilder.toString(), "assignIssue");
        }
    }

    private IssueInputParameters createPopulatedIssueInputParameters(final JiraIssueWrapper jiraIssueWrapper) {
        final IssueInputParameters issueInputParameters = jiraIssueService.newIssueInputParameters();
        populateIssueInputParameters(issueInputParameters, jiraIssueWrapper.getJiraIssueFieldTemplate());
        populateIssueInputParameters(issueInputParameters, jiraIssueWrapper.getBlackDuckIssueTemplate());

        return issueInputParameters;
    }

    private void populateIssueInputParameters(final IssueInputParameters issueInputParameters, final JiraIssueFieldTemplate jiraIssueFieldTemplate) {
        issueInputParameters
                .setProjectId(jiraIssueFieldTemplate.getJiraProjectId())
                .setIssueTypeId(jiraIssueFieldTemplate.getJiraIssueTypeId())
                .setSummary(jiraIssueFieldTemplate.getSummary())
                .setReporterId(jiraIssueFieldTemplate.getIssueCreatorUsername())
                .setDescription(jiraIssueFieldTemplate.getIssueDescription())
                .setAssigneeId(jiraIssueFieldTemplate.getAssigneeId());

        issueInputParameters.setRetainExistingValuesWhenParameterNotProvided(jiraIssueFieldTemplate.shouldRetainExistingValuesWhenParameterNotProvided());
        issueInputParameters.setApplyDefaultValuesWhenParameterNotProvided(jiraIssueFieldTemplate.shouldApplyDefaultValuesWhenParameterNotProvided());
    }

    private void populateIssueInputParameters(final IssueInputParameters issueInputParameters, final BlackDuckIssueFieldTemplate blackDuckIssueFieldTemplate) {
        final Map<Long, String> blackDuckMap = blackDuckIssueFieldTemplate.getBlackDuckFieldMappings(customFieldsMap);
        for (final Entry<Long, String> blackDuckFieldEntry : blackDuckMap.entrySet()) {
            issueInputParameters.addCustomFieldValue(blackDuckFieldEntry.getKey(), blackDuckFieldEntry.getValue());
        }
    }

}
