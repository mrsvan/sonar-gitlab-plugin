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

import static org.sonar.api.batch.rule.Severity.BLOCKER;
import static org.sonar.api.batch.rule.Severity.CRITICAL;
import static org.sonar.api.batch.rule.Severity.INFO;
import static org.sonar.api.batch.rule.Severity.MAJOR;
import static org.sonar.api.batch.rule.Severity.MINOR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

public class GlobalReport {
  private final int maxGlobalIssues;
  private final MarkDownUtils markDownUtils;
  private int[] newIssuesBySeverity = new int[Severity.values().length];
  private Map<Severity, List<String>> notReportedOnDiffMap = new HashMap<>();
  private int notReportedIssueCount = 0;

  public GlobalReport(int maxGlobalIssues, MarkDownUtils markDownUtils) {
    super();

    this.maxGlobalIssues = maxGlobalIssues;
    this.markDownUtils = markDownUtils;
  }

  private void increment(Severity severity) {
    newIssuesBySeverity[severity.ordinal()]++;
  }

  public String formatForMarkdown() {
    StringBuilder sb = new StringBuilder();
    printNewIssuesMarkdown(sb);
    if (hasNewIssue()) {
      sb.append("\nWatch the comments in this conversation to review them.");
    }
    if (notReportedIssueCount > 0) {
      sb.append(
          "\nNote: the following issues could not be reported as comments because they are located on lines that are not displayed in this commit:\n");

      int notReportedDisplayedIssueCount = 0;
      int i = 0;
      for (Severity severity : Severity.values()) {
        List<String> ss = notReportedOnDiffMap.get(severity);
        if (ss != null && !ss.isEmpty()) {
          for (String s : ss) {
            if (i < maxGlobalIssues) {
              sb.append(s).append("\n");
            } else {
              notReportedDisplayedIssueCount++;
            }
            i++;
          }
        }
      }

      if (notReportedDisplayedIssueCount > 0) {
        sb.append("* ... ").append(notReportedDisplayedIssueCount).append(" more\n");
      }
    }
    return sb.toString();
  }

  public String getStatusDescription() {
    StringBuilder sb = new StringBuilder();
    printNewIssuesInline(sb);
    return sb.toString();
  }

  public String getStatus() {
    return (newIssues(BLOCKER) > 0 || newIssues(CRITICAL) > 0) ? "failed" : "success";
  }

  private int newIssues(Severity s) {
    return newIssuesBySeverity[s.ordinal()];
  }

  private void printNewIssuesMarkdown(StringBuilder sb) {
    sb.append("SonarQube analysis reported ");
    int newIssues = newIssues(BLOCKER) + newIssues(CRITICAL) + newIssues(MAJOR) + newIssues(MINOR) + newIssues(INFO);
    if (newIssues > 0) {
      sb.append(newIssues).append(" issue" + (newIssues > 1 ? "s" : "")).append(":\n");
      for (Severity severity : Severity.values()) {
        printNewIssuesForMarkdown(sb, severity);
      }
    } else {
      sb.append("no issues.");
    }
  }

  private void printNewIssuesInline(StringBuilder sb) {
    sb.append("SonarQube reported ");
    int newIssues = newIssues(BLOCKER) + newIssues(CRITICAL) + newIssues(MAJOR) + newIssues(MINOR) + newIssues(INFO);
    if (newIssues > 0) {
      sb.append(newIssues).append(" issue" + (newIssues > 1 ? "s" : "")).append(",");
      int newCriticalOrBlockerIssues = newIssues(BLOCKER) + newIssues(CRITICAL);
      if (newCriticalOrBlockerIssues > 0) {
        printNewIssuesInline(sb, CRITICAL);
        printNewIssuesInline(sb, BLOCKER);
      } else {
        sb.append(" no critical nor blocker");
      }
    } else {
      sb.append("no issues");
    }
  }

  private void printNewIssuesInline(StringBuilder sb, Severity severity) {
    int issueCount = newIssues(severity);
    if (issueCount > 0) {
      if (sb.charAt(sb.length() - 1) == ',') {
        sb.append(" with ");
      } else {
        sb.append(" and ");
      }
      sb.append(issueCount).append(" ").append(getTextForSeverity(severity));
    }
  }

  private void printNewIssuesForMarkdown(StringBuilder sb, Severity severity) {
    int issueCount = newIssues(severity);
    if (issueCount > 0) {
      sb.append("* ").append(MarkDownUtils.getEmojiForSeverity(severity)).append(" ").append(issueCount).append(" ")
          .append(getTextForSeverity(severity)).append("\n");
    }
  }

  public void process(PostJobIssue issue, @Nullable String gitLabUrl, boolean reportedOnDiff) {
    increment(issue.severity());
    if (!reportedOnDiff) {
      notReportedIssueCount++;

      List<String> notReportedOnDiffs = notReportedOnDiffMap.get(issue.severity());
      if (notReportedOnDiffs == null) {
        notReportedOnDiffs = new ArrayList<>();
        notReportedOnDiffMap.put(issue.severity(), notReportedOnDiffs);
      }

      notReportedOnDiffs.add(new StringBuilder().append("* ").append(markDownUtils.globalIssue(issue.severity(),
          issue.message(), issue.ruleKey().toString(), gitLabUrl, issue.componentKey())).toString());
    }
  }

  public boolean hasNewIssue() {
    return newIssues(BLOCKER) + newIssues(CRITICAL) + newIssues(MAJOR) + newIssues(MINOR) + newIssues(INFO) > 0;
  }

  public static String getTextForSeverity(Severity severity) {
    String text;
    switch (severity) {
    case BLOCKER:
      text = "blocker";
      break;
    case CRITICAL:
      text = "critical";
      break;
    case MAJOR:
      text = "major";
      break;
    case MINOR:
      text = "minor";
      break;
    case INFO:
      text = "info";
      break;
    default:
      text = "undefined";
      break;
    }
    return text;
  }
}
