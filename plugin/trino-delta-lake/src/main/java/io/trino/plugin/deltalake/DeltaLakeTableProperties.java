/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.deltalake;

import com.google.common.collect.ImmutableList;
import io.trino.spi.TrinoException;
import io.trino.spi.session.PropertyMetadata;
import io.trino.spi.type.ArrayType;

import javax.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.trino.spi.StandardErrorCode.INVALID_TABLE_PROPERTY;
import static io.trino.spi.session.PropertyMetadata.longProperty;
import static io.trino.spi.session.PropertyMetadata.stringProperty;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

public class DeltaLakeTableProperties
{
    public static final String LOCATION_PROPERTY = "location";
    public static final String PARTITIONED_BY_PROPERTY = "partitioned_by";
    public static final String CHECKPOINT_INTERVAL_PROPERTY = "checkpoint_interval";
    // TODO: This property represents the subset of columns to be analyzed. This exists mainly because there is no way
    //       to pass the column names to ConnectorMetadata#getStatisticsCollectionMetadata; we should consider passing
    //       ConnectorTableHandle instead of ConnectorTableMetadata as an argument since it makes more information
    //       available (including the names of the columns to be analyzed)
    public static final String ANALYZE_COLUMNS_PROPERTY = "$trino.analyze_columns";

    private final List<PropertyMetadata<?>> tableProperties;

    @SuppressWarnings("unchecked")
    @Inject
    public DeltaLakeTableProperties()
    {
        tableProperties = ImmutableList.<PropertyMetadata<?>>builder()
                .add(stringProperty(
                        LOCATION_PROPERTY,
                        "File system location URI for external table",
                        null,
                        false))
                .add(new PropertyMetadata<>(
                        PARTITIONED_BY_PROPERTY,
                        "Partition columns",
                        new ArrayType(VARCHAR),
                        List.class,
                        ImmutableList.of(),
                        false,
                        value -> ImmutableList.copyOf(((Collection<String>) value).stream()
                                .map(name -> name.toLowerCase(ENGLISH))
                                .collect(Collectors.toList())),
                        value -> value))
                .add(longProperty(
                        CHECKPOINT_INTERVAL_PROPERTY,
                        "Checkpoint interval",
                        null,
                        false))
                .build();
    }

    public List<PropertyMetadata<?>> getTableProperties()
    {
        return tableProperties;
    }

    public static String getLocation(Map<String, Object> tableProperties)
    {
        return (String) tableProperties.get(LOCATION_PROPERTY);
    }

    public static List<String> getPartitionedBy(Map<String, Object> tableProperties)
    {
        List<String> partitionedBy = (List<String>) tableProperties.get(PARTITIONED_BY_PROPERTY);
        return partitionedBy == null ? ImmutableList.of() : ImmutableList.copyOf(partitionedBy);
    }

    public static Optional<Long> getCheckpointInterval(Map<String, Object> tableProperties)
    {
        Optional<Long> checkpointInterval = Optional.ofNullable((Long) tableProperties.get(CHECKPOINT_INTERVAL_PROPERTY));
        checkpointInterval.ifPresent(value -> {
            if (value <= 0) {
                throw new TrinoException(INVALID_TABLE_PROPERTY, format("%s must be greater than 0", CHECKPOINT_INTERVAL_PROPERTY));
            }
        });

        return checkpointInterval;
    }

    @SuppressWarnings("unchecked")
    public static Optional<Set<String>> getAnalyzeColumns(Map<String, Object> tableProperties)
    {
        return Optional.ofNullable((Set<String>) tableProperties.get(ANALYZE_COLUMNS_PROPERTY));
    }
}
