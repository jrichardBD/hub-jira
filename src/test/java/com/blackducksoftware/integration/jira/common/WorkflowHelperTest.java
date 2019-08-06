/**
 * Black Duck JIRA Plugin
 *
 * Copyright (C) 2019 Synopsys, Inc.
 * https://www.synopsys.com/
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
package com.blackducksoftware.integration.jira.common;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.WorkflowSchemeManager;
import com.blackducksoftware.integration.jira.mocks.ProjectManagerMock;
import com.blackducksoftware.integration.jira.mocks.issue.IssueTypeMock;
import com.blackducksoftware.integration.jira.mocks.workflow.JiraWorkflowMock;
import com.blackducksoftware.integration.jira.mocks.workflow.WorkflowManagerMock;
import com.blackducksoftware.integration.jira.mocks.workflow.WorkflowSchemeManagerMock;

public class WorkflowHelperTest {

    @Test
    public void matchesBlackDuckWorkflowNameTest() {
        assertTrue(WorkflowHelper.matchesBlackDuckWorkflowName(BlackDuckJiraConstants.BLACKDUCK_JIRA_WORKFLOW));
        assertTrue(WorkflowHelper.matchesBlackDuckWorkflowName(BlackDuckJiraConstants.BLACKDUCK_JIRA_WORKFLOW + "test"));
        assertTrue(WorkflowHelper.matchesBlackDuckWorkflowName(BlackDuckJiraConstants.BLACKDUCK_JIRA_WORKFLOW + " this should still match"));
        assertFalse(WorkflowHelper.matchesBlackDuckWorkflowName("BDS Hub PlugIn"));
        assertFalse(WorkflowHelper.matchesBlackDuckWorkflowName("Some other workflow"));
    }

    @Test
    public void usesBlackDuckWorkflowTest() {
        final WorkflowManager workflowManager = new WorkflowManagerMock();
        final WorkflowSchemeManager workflowSchemeManager = new WorkflowSchemeManagerMock();
        final ProjectManager projectManager = new ProjectManagerMock();

        WorkflowHelper workflowHelper = new WorkflowHelper(workflowManager, workflowSchemeManager, projectManager);

        assertFalse(workflowHelper.usesBlackDuckWorkflow(null, null));

        final Map<String, String> issueTypeIdToWorkflowName = new HashMap<>();
        final IssueTypeMock issueType = new IssueTypeMock();
        issueType.setId("Test ID");
        issueType.setName("Test Issue Type");

        assertFalse(workflowHelper.usesBlackDuckWorkflow(issueTypeIdToWorkflowName, issueType));

        final IssueTypeMock otherIssueType = new IssueTypeMock();
        otherIssueType.setId("Other Test ID");
        otherIssueType.setName("Other Test Issue Type");

        issueTypeIdToWorkflowName.put(otherIssueType.getId(), BlackDuckJiraConstants.BLACKDUCK_JIRA_WORKFLOW);

        assertFalse(workflowHelper.usesBlackDuckWorkflow(issueTypeIdToWorkflowName, issueType));

        issueTypeIdToWorkflowName.put(issueType.getId(), "Other workflow");

        assertFalse(workflowHelper.usesBlackDuckWorkflow(issueTypeIdToWorkflowName, issueType));

        issueTypeIdToWorkflowName.put(issueType.getId(), BlackDuckJiraConstants.BLACKDUCK_JIRA_WORKFLOW);

        assertTrue(workflowHelper.usesBlackDuckWorkflow(issueTypeIdToWorkflowName, issueType));
    }

    @Test
    public void getIssueTypeByNameTest() {
        final WorkflowManager workflowManager = new WorkflowManagerMock();
        final WorkflowSchemeManager workflowSchemeManager = new WorkflowSchemeManagerMock();
        final ProjectManager projectManager = new ProjectManagerMock();

        WorkflowHelper workflowHelper = new WorkflowHelper(workflowManager, workflowSchemeManager, projectManager);

        assertFalse(workflowHelper.getIssueTypeByName(Collections.EMPTY_LIST, "").isPresent());

        List<IssueType> issueTypes = new ArrayList<>();
        IssueTypeMock issueTypeMock = new IssueTypeMock();
        issueTypeMock.setName("Test Issue Type");
        issueTypes.add(issueTypeMock);
        assertFalse(workflowHelper.getIssueTypeByName(issueTypes, "Other Issue Type").isPresent());

        assertTrue(workflowHelper.getIssueTypeByName(issueTypes, "Test Issue Type").isPresent());
    }

    @Test
    public void doesBlackDuckDataExistYetTest() {
        final WorkflowManager workflowManager = new WorkflowManagerMock();
        final WorkflowSchemeManager workflowSchemeManager = new WorkflowSchemeManagerMock();
        final ProjectManager projectManager = new ProjectManagerMock();

        WorkflowHelper workflowHelper = new WorkflowHelper(workflowManager, workflowSchemeManager, projectManager);

        assertFalse(workflowHelper.doesBlackDuckDataExistYet(null, null, null, null));

        assertTrue(workflowHelper.doesBlackDuckDataExistYet(new JiraWorkflowMock(), null, null, null));
        assertTrue(workflowHelper.doesBlackDuckDataExistYet(null, new IssueTypeMock(), null, null));
        assertTrue(workflowHelper.doesBlackDuckDataExistYet(null, null, new IssueTypeMock(), null));
        assertTrue(workflowHelper.doesBlackDuckDataExistYet(null, null, null, new IssueTypeMock()));

        assertTrue(workflowHelper.doesBlackDuckDataExistYet(new JiraWorkflowMock(), new IssueTypeMock(), new IssueTypeMock(), new IssueTypeMock()));

    }
}
