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
import java.util.Objects;

import javax.annotation.Nullable;

import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

public final class IssueComparator implements Comparator<PostJobIssue> {
	@Override
	public int compare(@Nullable PostJobIssue left, @Nullable PostJobIssue right) {
		// Most severe issues should be displayed first.
		if (left == right) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		if (Objects.equals(left.severity(), right.severity())) {
			// When severity is the same, sort by component key to at least
			// group issues from
			// the same file together.
			return compareComponentKeyAndLine(left, right);
		}
		return compareSeverity(left.severity(), right.severity());
	}

	private static int compareComponentKeyAndLine(PostJobIssue left, PostJobIssue right) {
		if (!left.componentKey().equals(right.componentKey())) {
			return left.componentKey().compareTo(right.componentKey());
		}
		return compareInt(left.line(), right.line());
	}

	private static int compareSeverity(Severity leftSeverity, Severity rightSeverity) {
		if (leftSeverity.ordinal() > rightSeverity.ordinal()) {
			// Display higher severity first. Relies on Severity.ALL to be
			// sorted by severity.
			return -1;
		} else {
			return 1;
		}
	}

	private static int compareInt(@Nullable Integer leftLine, @Nullable Integer rightLine) {
		if (Objects.equals(leftLine, rightLine)) {
			return 0;
		} else if (leftLine == null) {
			return -1;
		} else if (rightLine == null) {
			return 1;
		} else {
			return leftLine.compareTo(rightLine);
		}
	}
}
