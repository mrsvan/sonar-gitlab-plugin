/*
 * SonarQube :: GitLab Plugin
 * Copyright (C) 2016-2017 Talanlabs
 * gabriel.allaigre@talanlabs.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.synaptix.sonar.plugins.gitlab;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;

/**
 * Compute comments to be added on the commit.
 */
public class CommitIssuePostJob implements PostJob {
    private static final Comparator<PostJobIssue> ISSUE_COMPARATOR = new IssueComparator();

    private final GitLabPluginConfiguration gitLabPluginConfiguration;
    private final CommitFacade commitFacade;
    private final MarkDownUtils markDownUtils;

    public CommitIssuePostJob(GitLabPluginConfiguration gitLabPluginConfiguration, CommitFacade commitFacade, MarkDownUtils markDownUtils) {
        this.gitLabPluginConfiguration = gitLabPluginConfiguration;
        this.commitFacade = commitFacade;
        this.markDownUtils = markDownUtils;
    }

    @Override
    public void describe(PostJobDescriptor descriptor) {
	descriptor.name("GitLab Commit Issue Publisher").requireProperties(GitLabPlugin.GITLAB_COMMIT_SHA,
		GitLabPlugin.GITLAB_REF_NAME);
    }

    @Override
    public void execute(PostJobContext context) {
        GlobalReport report = new GlobalReport(gitLabPluginConfiguration.maxGlobalIssues(), markDownUtils);

        Map<InputFile, Map<Integer, StringBuilder>> commentsToBeAddedByLine = processIssues(report, context.issues());

        updateReviewComments(commentsToBeAddedByLine);

        if (report.hasNewIssue()) {
            commitFacade.addGlobalComment(report.formatForMarkdown());
        }

        commitFacade.createOrUpdateSonarQubeStatus(report.getStatus(), report.getStatusDescription());
    }

    private Map<InputFile, Map<Integer, StringBuilder>> processIssues(GlobalReport report, Iterable<PostJobIssue> issues) {
        Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine = new HashMap<>();
        StreamSupport.stream(issues.spliterator(), false)
                     .filter(PostJobIssue::isNew)
                     .filter(i -> {
                     InputComponent inputComponent = i.inputComponent();
                     return inputComponent == null || !inputComponent.isFile()
                                 || commitFacade.hasFile((InputFile) inputComponent)
                                 || !gitLabPluginConfiguration.ignoreFileNotInCommit();
                     })
                     .sorted(ISSUE_COMPARATOR)
                     .forEach(i -> processIssue(report, commentToBeAddedByFileAndByLine, i));

        return commentToBeAddedByFileAndByLine;
    }

    private void processIssue(GlobalReport report,
        Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine, PostJobIssue issue) {
        boolean reportedInline = false;
        InputComponent inputComponent = issue.inputComponent();
        if (inputComponent != null && inputComponent.isFile()) {
            reportedInline = tryReportInline(commentToBeAddedByFileAndByLine, issue, (InputFile) inputComponent);
        }
        report.process(issue, commitFacade.getGitLabUrl(inputComponent, issue.line()), reportedInline);
    }

    private boolean tryReportInline(Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine,
	    PostJobIssue issue, InputFile inputFile) {
        Integer issueLine = issue.line();
        if (issueLine != null) {
            int line = issueLine.intValue();
            if (commitFacade.hasFileLine(inputFile, line)) {
                String message = issue.message();
                String ruleKey = issue.ruleKey().toString();
                if (!commentToBeAddedByFileAndByLine.containsKey(inputFile)) {
                    commentToBeAddedByFileAndByLine.put(inputFile, new HashMap<Integer, StringBuilder>());
                }
                Map<Integer, StringBuilder> commentsByLine = commentToBeAddedByFileAndByLine.get(inputFile);
                if (!commentsByLine.containsKey(line)) {
                    commentsByLine.put(line, new StringBuilder());
                }
                commentsByLine.get(line)
                    .append(markDownUtils.inlineIssue(issue.severity(), message, ruleKey))
                    .append("\n");
                return true;
            }
        }
        return false;
    }

    private void updateReviewComments(Map<InputFile, Map<Integer, StringBuilder>> commentsToBeAddedByLine) {
        for (Map.Entry<InputFile, Map<Integer, StringBuilder>> entry : commentsToBeAddedByLine.entrySet()) {
            for (Map.Entry<Integer, StringBuilder> entryPerLine : entry.getValue().entrySet()) {
                String body = entryPerLine.getValue().toString();
                commitFacade.createOrUpdateReviewComment(entry.getKey(), entryPerLine.getKey(), body);
            }
        }
    }
}
