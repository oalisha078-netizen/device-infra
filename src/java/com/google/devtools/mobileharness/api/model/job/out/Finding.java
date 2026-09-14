/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.mobileharness.api.model.job.out;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.common.metrics.stability.model.MetadataProvider;
import com.google.devtools.common.metrics.stability.model.proto.ExceptionProto;
import com.google.devtools.common.metrics.stability.model.proto.ExceptionProto.ExceptionDetail;
import com.google.devtools.mobileharness.api.model.proto.Diagnostic;
import com.google.devtools.mobileharness.api.model.proto.Diagnostic.Finding.Severity;
import java.util.Map;
import java.util.Optional;

/**
 * A finding is a piece of information about an execution that is considered noteworthy.
 *
 * <p>TODO: This class doesn't integrate with {@code TouchableTiming}. We should either integrate
 * with it or remove TouchableTiming(preferred).
 */
public class Finding implements MetadataProvider {

  private final ExceptionDetail.Builder exceptionDetailBuilder;
  private final Severity severity;

  @VisibleForTesting
  Finding(Severity severity, ExceptionDetail detail) {
    this.exceptionDetailBuilder = detail.toBuilder();
    this.severity = severity;
  }

  @VisibleForTesting
  Finding(Diagnostic.Finding findingProto) {
    this.exceptionDetailBuilder = findingProto.getDetail().toBuilder();
    this.severity = findingProto.getSeverity();
  }

  public ExceptionDetail getDetail() {
    return exceptionDetailBuilder.build();
  }

  public Severity getSeverity() {
    return severity;
  }

  @Override
  public void addMetadata(String key, String value) {
    checkNotNull(key, "key");
    checkNotNull(value, "value");
    ExceptionProto.ExceptionSummary.Builder summaryBuilder =
        exceptionDetailBuilder.getSummaryBuilder();
    for (int i = 0; i < summaryBuilder.getMetadataCount(); i++) {
      if (summaryBuilder.getMetadata(i).getKey().equals(key)) {
        summaryBuilder.setMetadata(
            i,
            ExceptionProto.ExceptionSummary.MetadataEntry.newBuilder().setKey(key).setValue(value));
        return;
      }
    }
    summaryBuilder.addMetadata(
        ExceptionProto.ExceptionSummary.MetadataEntry.newBuilder().setKey(key).setValue(value));
  }

  @Override
  public void addMetadata(Map<String, String> metadata) {
    checkNotNull(metadata, "metadata");
    metadata.forEach(this::addMetadata);
  }

  @Override
  public ImmutableMap<String, String> getMetadata() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (ExceptionProto.ExceptionSummary.MetadataEntry entry :
        exceptionDetailBuilder.getSummary().getMetadataList()) {
      builder.put(entry.getKey(), entry.getValue());
    }
    return builder.buildKeepingLast();
  }

  @Override
  public Optional<String> getMetadata(String key) {
    return exceptionDetailBuilder.getSummary().getMetadataList().stream()
        .filter(entry -> entry.getKey().equals(key))
        .map(ExceptionProto.ExceptionSummary.MetadataEntry::getValue)
        .reduce((first, second) -> second);
  }

  public Diagnostic.Finding toProto() {
    return Diagnostic.Finding.newBuilder()
        .setSeverity(severity)
        .setDetail(exceptionDetailBuilder)
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Finding that)) {
      return false;
    }
    return toProto().equals(that.toProto());
  }

  @Override
  public int hashCode() {
    return toProto().hashCode();
  }

  @Override
  public String toString() {
    return toProto().toString();
  }
}
