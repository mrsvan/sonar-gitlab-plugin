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

import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

@Properties({
    @Property(
      key = GitLabPlugin.GITLAB_URL,
      defaultValue = "https://gitlab.com",
      name = "GitLab URL",
      description = "URL to access GitLab."),
    @Property(
      key = GitLabPlugin.GITLAB_IGNORE_CERT,
      defaultValue = "false",
      name = "GitLab Ignore Certificate",
      description = "Ignore Certificate for access GitLab.",
      type = PropertyType.BOOLEAN),
    @Property(
      key = GitLabPlugin.GITLAB_MAX_GLOBAL_ISSUES,
      defaultValue = "10",
      name = "GitLab Max Global Issues",
      description = "Max issues to show in global comment.",
      type = PropertyType.INTEGER),
    @Property(
      key = GitLabPlugin.GITLAB_USER_TOKEN,
      name = "GitLab User Token",
      description = "GitLab user token.",
      type = PropertyType.PASSWORD),
    @Property(
      key = GitLabPlugin.GITLAB_PROJECT_ID,
      name = "GitLab Project id",
      description = "The unique id of the GitLab project.",
      project = true,
      global = false),
    @Property(
      key = GitLabPlugin.GITLAB_COMMIT_SHA,
      name = "GitLab Commit SHA",
      description = "The commit revision for which project is built.",
      global = false),
    @Property(
      key = GitLabPlugin.GITLAB_REF_NAME,
      name = "GitLab Ref Name",
      description = "The commit revision for which project is built.",
      global = false),
    @Property(
      key = GitLabPlugin.GITLAB_IGNORE_FILE,
      defaultValue = "true",
      name = "GitLab Ignore file",
      description = "Ignore issues on files no modified by the commit.",
      type = PropertyType.BOOLEAN)
  })
public class GitLabPlugin implements Plugin {

    public static final String GITLAB_URL = "sonar.gitlab.url";
    public static final String GITLAB_IGNORE_CERT = "sonar.gitlab.ignore_certificate";
    public static final String GITLAB_MAX_GLOBAL_ISSUES = "sonar.gitlab.max_global_issues";
    public static final String GITLAB_USER_TOKEN = "sonar.gitlab.user_token";
    public static final String GITLAB_PROJECT_ID = "sonar.gitlab.project_id";
    public static final String GITLAB_COMMIT_SHA = "sonar.gitlab.commit_sha";
    public static final String GITLAB_REF_NAME = "sonar.gitlab.ref_name";
    public static final String GITLAB_IGNORE_FILE = "sonar.gitlab.ignore_file";
    public static final String GITLAB_GLOBAL_TEMPLATE = "sonar.gitlab.global_template";
    public static final String GITLAB_INLINE_TEMPLATE = "sonar.gitlab.inline_template";

    @Override
    public void define(Context context) {
        context.addExtensions(
		    CommitIssuePostJob.class,
		    GitLabPluginConfiguration.class,
		    CommitProjectBuilder.class,
		    CommitFacade.class,
            MarkDownUtils.class);
    }
}
