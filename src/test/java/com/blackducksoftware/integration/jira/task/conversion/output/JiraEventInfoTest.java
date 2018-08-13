/**
 * Black Duck JIRA Plugin
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
package com.blackducksoftware.integration.jira.task.conversion.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.jira.common.exception.EventDataBuilderException;
import com.blackducksoftware.integration.jira.config.model.ProjectFieldCopyMapping;
import com.blackducksoftware.integration.jira.task.conversion.output.eventdata.EventCategory;
import com.blackducksoftware.integration.jira.task.conversion.output.eventdata.EventData;
import com.blackducksoftware.integration.jira.task.conversion.output.eventdata.EventDataBuilder;
import com.synopsys.integration.hub.api.generated.enumeration.NotificationType;
import com.synopsys.integration.hub.notification.content.detail.NotificationContentDetail;

public class JiraEventInfoTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testValidVulnerabilityEvent() throws EventDataBuilderException, URISyntaxException {
        final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings = new HashSet<>();
        final IssuePropertiesGenerator issuePropertiesGenerator = createIssuePropertyGenerator(NotificationType.VULNERABILITY);

        final EventDataBuilder eventDataBuilder = createEventDataBuilder(EventCategory.VULNERABILITY, jiraFieldCopyMappings, issuePropertiesGenerator);

        final EventData eventData = eventDataBuilder.build();

        checkCommonValues(jiraFieldCopyMappings, issuePropertiesGenerator, eventData);
        assertEquals(null, eventData.getBlackDuckRuleName());
        assertEquals(null, eventData.getBlackDuckRuleUrl());
    }

    @Test
    public void testInValidVulnerabilityEvent() throws EventDataBuilderException, URISyntaxException {
        final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings = new HashSet<>();
        final IssuePropertiesGenerator issuePropertiesGenerator = createIssuePropertyGenerator(NotificationType.VULNERABILITY);

        final EventDataBuilder eventDataBuilder = createEventDataBuilder(EventCategory.VULNERABILITY, jiraFieldCopyMappings, issuePropertiesGenerator);
        eventDataBuilder.setBlackDuckProjectVersionUrl(null);

        try {
            eventDataBuilder.build();
            fail("Expected exception");
        } catch (final EventDataBuilderException e) {
            // expected
        }
    }

    @Test
    public void testValidPolicyEvent() throws EventDataBuilderException, URISyntaxException {
        final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings = new HashSet<>();
        final IssuePropertiesGenerator issuePropertiesGenerator = createIssuePropertyGenerator(NotificationType.POLICY_OVERRIDE);

        final EventDataBuilder eventDataBuilder = createEventDataBuilder(EventCategory.POLICY, jiraFieldCopyMappings, issuePropertiesGenerator);
        eventDataBuilder.setBlackDuckRuleName("blackDuckRuleName");
        eventDataBuilder.setBlackDuckRuleUrl("blackDuckRuleUrl");

        final EventData eventData = eventDataBuilder.build();

        checkCommonValues(jiraFieldCopyMappings, issuePropertiesGenerator, eventData);
        assertEquals("blackDuckRuleName", eventData.getBlackDuckRuleName());
        assertEquals("blackDuckRuleUrl", eventData.getBlackDuckRuleUrl());
    }

    @Test
    public void testInValidPolicyEventMissingRuleUrl() throws EventDataBuilderException, URISyntaxException {
        final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings = new HashSet<>();
        final IssuePropertiesGenerator issuePropertiesGenerator = createIssuePropertyGenerator(NotificationType.POLICY_OVERRIDE);

        final EventDataBuilder eventDataBuilder = createEventDataBuilder(EventCategory.POLICY, jiraFieldCopyMappings, issuePropertiesGenerator);
        eventDataBuilder.setBlackDuckRuleName("blackDuckRuleName");

        try {
            eventDataBuilder.build();
            fail("Expected exception");
        } catch (final EventDataBuilderException e) {
            // expected
        }
    }

    @Test
    public void testInValidPolicyEventMissingRuleName() throws EventDataBuilderException, URISyntaxException {
        final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings = new HashSet<>();
        final IssuePropertiesGenerator issuePropertiesGenerator = createIssuePropertyGenerator(NotificationType.POLICY_OVERRIDE);

        final EventDataBuilder eventDataBuilder = createEventDataBuilder(EventCategory.POLICY, jiraFieldCopyMappings, issuePropertiesGenerator);
        eventDataBuilder.setBlackDuckRuleUrl("blackDuckRuleUrl");

        try {
            eventDataBuilder.build();
            fail("Expected exception");
        } catch (final EventDataBuilderException e) {
            // expected
        }
    }

    private void checkCommonValues(final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings, final IssuePropertiesGenerator issuePropertiesGenerator,
            final EventData eventData) {
        assertEquals(BlackDuckEventAction.ADD_COMMENT, eventData.getAction());
        assertEquals("blackDuckComponentName", eventData.getBlackDuckComponentName());
        assertEquals("blackDuckComponentUrl", eventData.getBlackDuckComponentUrl());
        assertEquals("blackDuckComponentVersion", eventData.getBlackDuckComponentVersion());
        assertEquals("blackDuckComponentVersionUrl", eventData.getBlackDuckComponentVersionUrl());

        assertEquals("blackDuckComponentUsage", eventData.getBlackDuckComponentUsage());
        assertEquals("blackDuckComponentOrigin", eventData.getBlackDuckComponentOrigin());
        assertEquals("blackDuckComponentOriginId", eventData.getBlackDuckComponentOriginId());

        assertEquals("blackDuckProjectVersionUrl", eventData.getBlackDuckProjectVersionUrl());
        assertEquals("blackDuckProjectVersionNickname", eventData.getBlackDuckProjectVersionNickname());
        assertEquals("blackDuckLicenseNames", eventData.getBlackDuckLicenseNames());

        assertEquals("blackDuckProjectName", eventData.getBlackDuckProjectName());
        assertEquals("blackDuckProjectVersion", eventData.getBlackDuckProjectVersion());

        assertEquals(jiraFieldCopyMappings, eventData.getJiraFieldCopyMappings());
        assertEquals("jiraIssueAssigneeUserId", eventData.getJiraIssueAssigneeUserId());
        assertEquals("jiraIssueComment", eventData.getJiraIssueComment());
        assertEquals("jiraIssueCommentForExistingIssue", eventData.getJiraIssueCommentForExistingIssue());
        assertEquals("jiraIssueCommentForExistingIssue", eventData.getJiraIssueCommentForExistingIssue());
        assertEquals("jiraIssueCommentInLieuOfStateChange", eventData.getJiraIssueCommentInLieuOfStateChange());
        assertEquals("jiraIssueDescription", eventData.getJiraIssueDescription());
        assertEquals(issuePropertiesGenerator, eventData.getJiraIssuePropertiesGenerator());
        assertEquals("jiraIssueReOpenComment", eventData.getJiraIssueReOpenComment());
        assertEquals("jiraIssueResolveComment", eventData.getJiraIssueResolveComment());
        assertEquals("jiraIssueSummary", eventData.getJiraIssueSummary());
        assertEquals("jiraIssueTypeId", eventData.getJiraIssueTypeId());
        assertEquals(Long.valueOf(123L), eventData.getJiraProjectId());
        assertEquals("jiraProjectName", eventData.getJiraProjectName());
        assertEquals("jiraAdminUserKey", eventData.getJiraAdminUserKey());
        assertEquals("jiraAdminUserName", eventData.getJiraAdminUsername());
        assertEquals("jiraIssueCreatorUserKey", eventData.getJiraIssueCreatorUserKey());
        assertEquals("jiraIssueCreatorUserName", eventData.getJiraIssueCreatorUsername());
    }

    private EventDataBuilder createEventDataBuilder(final EventCategory eventCategory, final Set<ProjectFieldCopyMapping> jiraFieldCopyMappings,
            final IssuePropertiesGenerator issuePropertiesGenerator) {
        final EventDataBuilder eventDataBuilder = new EventDataBuilder(eventCategory);
        eventDataBuilder.setAction(BlackDuckEventAction.ADD_COMMENT)
                .setBlackDuckComponentName("blackDuckComponentName")
                .setBlackDuckComponentUrl("blackDuckComponentUrl")
                .setBlackDuckComponentVersion("blackDuckComponentVersion")
                .setBlackDuckComponentVersionUrl("blackDuckComponentVersionUrl")
                .setBlackDuckComponentUsage("blackDuckComponentUsage")
                .setBlackDuckComponentOrigin("blackDuckComponentOrigin")
                .setBlackDuckComponentOriginId("blackDuckComponentOriginId")
                .setBlackDuckProjectName("blackDuckProjectName")
                .setBlackDuckProjectVersion("blackDuckProjectVersion")
                .setBlackDuckProjectVersionUrl("blackDuckProjectVersionUrl")
                .setBlackDuckProjectVersionNickname("blackDuckProjectVersionNickname")
                .setBlackDuckLicenseNames("blackDuckLicenseNames")
                .setJiraFieldCopyMappings(jiraFieldCopyMappings)
                .setJiraIssueAssigneeUserId("jiraIssueAssigneeUserId")
                .setJiraIssueComment("jiraIssueComment")
                .setJiraIssueCommentForExistingIssue("jiraIssueCommentForExistingIssue")
                .setJiraIssueCommentForExistingIssue("jiraIssueCommentForExistingIssue")
                .setJiraIssueCommentInLieuOfStateChange("jiraIssueCommentInLieuOfStateChange")
                .setJiraIssueDescription("jiraIssueDescription")
                .setJiraIssuePropertiesGenerator(issuePropertiesGenerator)
                .setJiraIssueReOpenComment("jiraIssueReOpenComment")
                .setJiraIssueResolveComment("jiraIssueResolveComment")
                .setJiraIssueSummary("jiraIssueSummary")
                .setJiraIssueTypeId("jiraIssueTypeId")
                .setJiraProjectId(123L)
                .setJiraProjectName("jiraProjectName")
                .setJiraAdminUserKey("jiraAdminUserKey")
                .setJiraAdminUserName("jiraAdminUserName")
                .setJiraIssueCreatorUserKey("jiraIssueCreatorUserKey")
                .setJiraIssueCreatorUserName("jiraIssueCreatorUserName");
        ;
        return eventDataBuilder;
    }

    private IssuePropertiesGenerator createIssuePropertyGenerator(final NotificationType notificationType) throws URISyntaxException {
        final Optional<String> projectName = Optional.of("projectName");
        final Optional<String> projectVersionName = Optional.of("projectVersionName");
        final Optional<String> projectVersionUri = Optional.of("projectVersionUri");

        final Optional<String> componentName = Optional.of("compName");
        final Optional<String> componentUri = Optional.of("componentUri");
        final Optional<String> componentVersionName = Optional.of("versionName");
        final Optional<String> componentVersionUri = Optional.of("compVerName");
        final Optional<String> componentVersionOriginId = Optional.of("compVerOriginId");
        final Optional<String> componentVersionOriginName = Optional.of("compVerOriginName");

        final NotificationContentDetail detail = NotificationContentDetail.createDetail(notificationType.name(), projectName, projectVersionName, projectVersionUri, componentName, componentUri, componentVersionName, componentVersionUri,
                Optional.empty(), Optional.empty(), componentVersionOriginName, Optional.empty(), componentVersionOriginId);
        final IssuePropertiesGenerator issuePropertiesGenerator = new IssuePropertiesGenerator(detail, Optional.empty());
        return issuePropertiesGenerator;
    }
}
