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

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.utils.MessageException;

/**
 * Trigger load of commit metadata at the very beginning of SQ analysis. Also
 * set "in progress" status on the commit.
 */
public class CommitProjectBuilder extends ProjectBuilder {

    private final GitLabPluginConfiguration gitLabPluginConfiguration;
    private final CommitFacade commitFacade;
    private final AnalysisMode mode;

    public CommitProjectBuilder(GitLabPluginConfiguration gitLabPluginConfiguration, CommitFacade commitFacade, AnalysisMode mode) {
        this.gitLabPluginConfiguration = gitLabPluginConfiguration;
        this.commitFacade = commitFacade;
        this.mode = mode;
    }

    @Override
    public void build(Context context) {
        if (!gitLabPluginConfiguration.isEnabled()) {
            return;
        }

        checkMode();
        commitFacade.init(context.projectReactor().getRoot().getBaseDir());
        commitFacade.createOrUpdateSonarQubeStatus("pending", "SonarQube analysis in progress");
    }

    private void checkMode() {
        if (!mode.isIssues() && !mode.isPreview()) {
            throw MessageException.of("The GitLab plugin is only intended to be used in preview or issues mode. Please set '"
                + CoreProperties.ANALYSIS_MODE + "'.");
        }
    }
}
