Sonar GitLab Plugin
===================

Forked from https://github.com/SonarSource/sonar-github

# Goal

Add to each **commit** inline comments with details of rule violation and a global commentary summarizing the issues introduced by this **commit**.

Comment commits:
![Comment commits](doc/comment_commits.jpg)

Comment line:
![Comment line](doc/comment_line.jpg)

Add build line:
![Add buids](doc/builds.jpg)

# Usage

This plugin uses Sonar Plugin API version 6.2 so **minimum SonarQube version is 6.2**.

- Download last version https://github.com/stour/sonar-gitlab-plugin/releases/download/1.8.0/sonar-gitlab-plugin-1.8.0.jar
- Copy file in extensions directory `SONARQUBE_HOME/extensions/plugins`
- Restart SonarQube

# Configuration

| Variable | Comment | Type |
| -------- | ----------- | ---- |
| sonar.gitlab.url | GitLab url | Global administration, Variable |
| sonar.gitlab.max_global_issues | Maximum number of issues to be displayed in the global comment |  Global administration, Variable |
| sonar.gitlab.user_token | Token of the user who can make reports on the project |  Global administration, Variable |
| sonar.gitlab.project_id | Id of the GitLab project to be scanned | Project administration, Variable |
| sonar.gitlab.commit_sha | SHA of the commit that triggers the scan | Variable |
| sonar.gitlab.ref_name | The name of branch or tag | Variable |

- Global administration : Global **Settings** in SonarQube available at http://{your-sonar-server}:9000/settings/?category=gitlab
- Project administration : Project **Settings** in SonarQube available at https://{your-sonar-server}:9000/project/settings/?category=gitlab&id={your-project-id}
- Variable : In an environment variable or in the `pom.xml` or from the Maven command with` -D`

# Command line example

``` shell
mvn -B -V verify sonar:sonar -Dsonar.host.url=$SONAR_URL -Dsonar.analysis.mode=preview -Dsonar.issuesReport.console.enable=true -Dsonar.gitlab.project_id=$CI_PROJECT_ID -Dsonar.gitlab.commit_sha=$CI_BUILD_REF -Dsonar.gitlab.ref_name=$CI_BUILD_REF_NAME
```
