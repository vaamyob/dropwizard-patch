/*
 * Copyright 2014 Tariq Bugrara
 *
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

package io.progix.dropwizard.patch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import io.progix.jackson.JsonPatchOperation;
import io.progix.jackson.exceptions.JsonPatchTestFailedException;

import java.util.List;

public class DefaultJsonPatch<T> extends ContextualJsonPatch<T> {

    @JsonIgnore
    private final ObjectMapper mapper;
    @JsonIgnore
    private final Class<? extends T> typeClass;

    /**
     * Constructs an instance using a list of {@link JsonPatchOperation}
     *
     * @param operations A list of {@link JsonPatchOperation}
     * @param typeClass  Class is required to convert a JsonNode back into context class
     */
    public DefaultJsonPatch(List<JsonPatchOperation> operations, Class<? extends T> typeClass) {
        super(operations);

        this.typeClass = typeClass;
        this.mapper = Jackson.newObjectMapper();
    }

    @Override
    public T apply(T context) throws JsonPatchTestFailedException {
        T copiedContext = PatchUtil.copy(context);

        for (JsonPatchOperation instruction : operations) {

            switch (instruction.getOperation()) {
                case ADD:
                    if (addOperation == null) {
                        JsonNode node = mapper.convertValue(copiedContext, JsonNode.class);
                        node = io.progix.jackson.JsonPatch.add(instruction.getPath(), instruction.getValue(), node);
                        copiedContext = mapper.convertValue(node, typeClass);
                    } else {

                        addOperation.add(copiedContext, new JsonPath(instruction.getPath()), new JsonPatchValue
                                (instruction.getValue()));
                    }
                    break;
                case COPY:
                    if (copyOperation == null) {
                        JsonNode node = mapper.convertValue(copiedContext, JsonNode.class);
                        node = io.progix.jackson.JsonPatch.copy(instruction.getPath(), instruction.getFrom(), node);
                        copiedContext = mapper.convertValue(node, typeClass);
                    } else {

                        copyOperation
                                .copy(copiedContext, new JsonPath(instruction.getFrom()), new JsonPath(instruction
                                        .getPath()));
                    }
                    break;
                case MOVE:
                    if (moveOperation == null) {
                        JsonNode node = mapper.convertValue(copiedContext, JsonNode.class);
                        node = io.progix.jackson.JsonPatch.move(instruction.getPath(), instruction.getFrom(), node);
                        copiedContext = mapper.convertValue(node, typeClass);
                    } else {

                        moveOperation
                                .move(copiedContext, new JsonPath(instruction.getFrom()), new JsonPath(instruction
                                        .getPath()));
                    }
                    break;
                case REMOVE:
                    if (removeOperation == null) {
                        JsonNode node = mapper.convertValue(copiedContext, JsonNode.class);
                        node = io.progix.jackson.JsonPatch.remove(instruction.getPath(), node);
                        copiedContext = mapper.convertValue(node, typeClass);
                    } else {

                        removeOperation.remove(copiedContext, new JsonPath(instruction.getPath()));
                    }
                    break;
                case REPLACE:
                    if (replaceOperation == null) {
                        JsonNode node = mapper.convertValue(copiedContext, JsonNode.class);
                        node = io.progix.jackson.JsonPatch.replace(instruction.getPath(), instruction.getValue(), node);
                        copiedContext = mapper.convertValue(node, typeClass);
                    } else {

                        replaceOperation.replace(copiedContext, new JsonPath(instruction.getPath()), new
                                JsonPatchValue(instruction.getValue()));
                    }
                    break;
                case TEST:
                    if (testOperation == null) {
                        JsonNode node = mapper.convertValue(copiedContext, JsonNode.class);
                        io.progix.jackson.JsonPatch.test(instruction.getPath(), instruction.getValue(), node);
                    } else {

                        boolean success = testOperation
                                .test(copiedContext, new JsonPath(instruction.getPath()), new JsonPatchValue
                                        (instruction.getValue()));
                        if (!success) {
                            throw new JsonPatchTestFailedException(instruction.getPath(), instruction.getValue(), "A test failed.");
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        return copiedContext;
    }
}
