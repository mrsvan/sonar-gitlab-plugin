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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.Nullable;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class MarkDownUtils {

  private final String ruleUrlPrefix;

  public MarkDownUtils(Settings settings) {
    // If server base URL was not configured in SQ server then is is better to take URL configured on batch side
    String baseUrl = settings.hasKey(CoreProperties.SERVER_BASE_URL) ? settings.getString(CoreProperties.SERVER_BASE_URL) : settings.getString("sonar.host.url");
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    ruleUrlPrefix = baseUrl;
  }

  public String inlineIssue(Severity severity, String message, String ruleKey) {
    String ruleLink = getRuleLink(ruleKey);
    StringBuilder sb = new StringBuilder();
    sb.append(getEmojiForSeverity(severity))
      .append(" ")
      .append(message)
      .append(" ")
      .append(ruleLink);
    return sb.toString();
  }

  public String globalIssue(Severity severity, String message, String ruleKey, @Nullable String url, String componentKey) {
    String ruleLink = getRuleLink(ruleKey);
    StringBuilder sb = new StringBuilder();
    sb.append(getEmojiForSeverity(severity)).append(" ");
    if (url != null) {
      sb.append("[").append(message).append("]").append("(").append(url).append(")");
    } else {
      sb.append(message).append(" ").append("(").append(componentKey).append(")");
    }
    sb.append(" ").append(ruleLink);
    return sb.toString();
  }

  String getRuleLink(String ruleKey) {
    return "[:blue_book:](" + ruleUrlPrefix + "coding_rules#rule_key=" + encodeForUrl(ruleKey) + ")";
  }

  static String encodeForUrl(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");

    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  public static String getEmojiForSeverity(Severity severity) {
    String emojiCode;
    switch (severity) {
      case BLOCKER:
        emojiCode = ":no_entry:";
        break;
      case CRITICAL:
        emojiCode = ":no_entry_sign:";
        break;
      case MAJOR:
        emojiCode = ":warning:";
        break;
      case MINOR:
        emojiCode = ":arrow_down_small:";
        break;
      case INFO:
        emojiCode = ":information_source:";
        break;
      default:
        emojiCode = ":grey_question:";
        break;
    }
    return emojiCode;
  }

}
