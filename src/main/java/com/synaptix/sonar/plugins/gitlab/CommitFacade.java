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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.scan.filesystem.PathResolver;

import com.synaptix.gitlab.api.GitLabAPI;
import com.synaptix.gitlab.api.Paged;
import com.synaptix.gitlab.api.models.commits.GitLabCommitDiff;
import com.synaptix.gitlab.api.models.projects.GitLabProject;

/**
 * Facade for all WS interaction with GitLab.
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class CommitFacade {

    static final String COMMIT_CONTEXT = "sonarqube";

    private final GitLabPluginConfiguration config;
    private File gitBaseDir;
    private GitLabAPI gitLabAPI;
    private GitLabProject gitLabProject;
    private Map<String, Set<Integer>> patchPositionMappingByFile;

    public CommitFacade(GitLabPluginConfiguration config) {
        this.config = config;
    }

    private static Map<String, Set<Integer>> mapPatchPositionsToLines(List<GitLabCommitDiff> diffs) throws IOException {
        Map<String, Set<Integer>> patchPositionMappingByFile = new HashMap<>();
        for (GitLabCommitDiff file : diffs) {
            Set<Integer> patchLocationMapping = new HashSet<>();
            patchPositionMappingByFile.put(file.getNewPath(), patchLocationMapping);
            String patch = file.getDiff();
            if (patch == null) {
                continue;
            }
            processPatch(patchLocationMapping, patch);
        }
        return patchPositionMappingByFile;
    }

    private static void processPatch(Set<Integer> patchLocationMapping, String patch) throws IOException {
        int currentLine = -1;
        for (String line : IOUtils.readLines(new StringReader(patch))) {
            if (line.startsWith("@@")) {
                // http://en.wikipedia.org/wiki/Diff_utility#Unified_format
                Matcher matcher = Pattern.compile("@@\\p{Space}-[0-9]+(?:,[0-9]+)?\\p{Space}\\+([0-9]+)(?:,[0-9]+)?\\p{Space}@@.*").matcher(line);
                if (!matcher.matches()) {
                    throw new IllegalStateException("Unable to parse patch line " + line + "\nFull patch: \n" + patch);
                }
                currentLine = Integer.parseInt(matcher.group(1));
            } else if (line.startsWith("-")) {
                // Skip removed lines
            } else if (line.startsWith("+") || line.startsWith(" ")) {
                // Count added and unmodified lines
                patchLocationMapping.add(currentLine);
                currentLine++;
            } else if (line.startsWith("\\")) {
                // I'm only aware of \ No newline at end of file
                // Ignore
            }
        }
    }

    public void init(File projectBaseDir) {
        if (findGitBaseDir(projectBaseDir) == null) {
            throw new IllegalStateException("Unable to find Git root directory. Is " + projectBaseDir + " part of a Git repository?");
        }
        gitLabAPI = GitLabAPI.connect(config.url(), config.userToken()).setIgnoreCertificateErrors(config.ignoreCertificate());
        try {
            gitLabProject = getGitLabProject();

            Paged<GitLabCommitDiff> paged = gitLabAPI.getGitLabAPICommits().getCommitDiffs(gitLabProject.getId(), config.commitSHA(), null);
            List<GitLabCommitDiff> commitDiffs = new ArrayList<>();
            do {
                if (paged.getResults() != null) {
                    commitDiffs.addAll(paged.getResults());
                }
            } while ((paged = paged.nextPage()) != null);
            patchPositionMappingByFile = mapPatchPositionsToLines(commitDiffs);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to perform GitLab WS operation", e);
        }
    }

    private File findGitBaseDir(@Nullable File baseDir) {
        if (baseDir == null) {
            return null;
        }
        if (new File(baseDir, ".git").exists()) {
            gitBaseDir = baseDir;
            return baseDir;
        }
        return findGitBaseDir(baseDir.getParentFile());
    }

    private GitLabProject getGitLabProject() throws IOException {
        String projectId = config.projectId();
        if (projectId == null) {
            throw new IllegalStateException("Unable found project for null project name. Set Configuration sonar.gitlab.project_id");
        }
        Paged<GitLabProject> paged = gitLabAPI.getGitLabAPIProjects().getProjectAlls(null, null, null, null, null, null);
        if (paged == null) {
            throw new IllegalStateException("Unable found project for " + projectId + " Verify Configuration sonar.gitlab.project_id or sonar.gitlab.user_token access project");
        }
        List<GitLabProject> projects = new ArrayList<>();
        do {
            if (paged.getResults() != null) {
                projects.addAll(paged.getResults());
            }
        } while ((paged = paged.nextPage()) != null);

        List<GitLabProject> res = new ArrayList<>();
        for (GitLabProject project : projects) {
            if (projectId.equals(project.getId().toString()) || projectId.equals(project.getPathWithNamespace()) || projectId.equals(project.getHttpUrl())
                || projectId.equals(project.getSshUrl()) || projectId.equals(project.getWebUrl()) || projectId.equals(project.getNameWithNamespace())) {
                res.add(project);
            }
        }
        if (res.isEmpty()) {
            throw new IllegalStateException("Unable found project for " + projectId + " Verify Configuration sonar.gitlab.project_id or sonar.gitlab.user_token access project");
        }
        if (res.size() > 1) {
            throw new IllegalStateException("Multiple found projects for " + projectId);
        }
        return res.get(0);
    }

    public void createOrUpdateSonarQubeStatus(String status, String statusDescription) {
        try {
            gitLabAPI.getGitLabAPICommits().postCommitStatus(gitLabProject.getId(), config.commitSHA(), status, config.refName(), COMMIT_CONTEXT, null, statusDescription);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to update commit status", e);
        }
    }

    public boolean hasFile(InputFile inputFile) {
        return patchPositionMappingByFile.containsKey(getPath(inputFile));
    }

    public boolean hasFileLine(InputFile inputFile, int line) {
        return hasFile(inputFile) && patchPositionMappingByFile.get(getPath(inputFile)).contains(line);
    }

    public String getGitLabUrl(@Nullable InputComponent inputComponent, @Nullable Integer issueLine) {
	if (inputComponent instanceof InputPath) {
            String path = getPath((InputPath) inputComponent);
            return gitLabProject.getWebUrl() + "/blob/" + config.commitSHA() + "/" + path + (issueLine != null ? ("#L" + issueLine) : "");
        }
        return null;
    }

    public void createOrUpdateReviewComment(InputFile inputFile, Integer line, String body) {
        String fullpath = getPath(inputFile);
        try {
            gitLabAPI.getGitLabAPICommits().postCommitComments(gitLabProject.getId(), config.commitSHA(), body, fullpath, line, "new");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create or update review comment in file " + fullpath + " at line " + line, e);
        }
    }

    private String getPath(InputPath inputPath) {
        return new PathResolver().relativePath(gitBaseDir, inputPath.file());
    }

    public void addGlobalComment(String comment) {
        try {
            gitLabAPI.getGitLabAPICommits().postCommitComments(gitLabProject.getId(), config.commitSHA(), comment, null, null, null);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to comment the commit", e);
        }
    }
}
