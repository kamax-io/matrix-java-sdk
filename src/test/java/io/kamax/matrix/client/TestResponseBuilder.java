/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2017 Arne Augenstein
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.matrix.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestResponseBuilder {
    private final int status;

    private Optional<String> contentType = Optional.empty();
    private Map<String, String> headers = new HashMap<>();
    private Optional<String> body = Optional.empty();
    private Optional<String> bodyFile = Optional.empty();

    public TestResponseBuilder(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public TestResponseBuilder setContentType(String contentType) {
        this.contentType = Optional.ofNullable(contentType);
        return this;
    }

    public Optional<String> getContentType() {
        return contentType;
    }

    public TestResponseBuilder putHeader(String header, String value) {
        headers.put(header, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public TestResponseBuilder setBody(String body) {
        this.body = Optional.ofNullable(body);
        return this;
    }

    public Optional<String> getBody() {
        if (status != 200) {
            return Optional.of(String.format("{\"errcode\": \"%s\", \"error\": \"%s\"}", getErrcode(), getError()));
        }

        return body;
    }

    /**
     * Careful: Body takes precedence over bodyFile, if both values are set.
     *
     * @param bodyFile
     *            Path to the bodyFile
     * @return The object itself (builder pattern)
     */
    public TestResponseBuilder setBodyFile(String bodyFile) {
        this.bodyFile = Optional.ofNullable(bodyFile);
        return this;
    }

    public Optional<String> getBodyFile() {
        return bodyFile;
    }

    public String getErrcode() {
        switch (getStatus()) {
        case 403:
            return "M_FORBIDDEN";
        case 404:
            return "M_NOT_FOUND";
        case 429:
            return "M_LIMIT_EXCEEDED";
        default:
            return "";
        }
    }

    public String getError() {
        switch (getStatus()) {
        case 403:
            return "Access denied.";
        case 404:
            return "Element not found.";
        case 429:
            return "Too many requests have been sent in a short period of time. Wait a while then try again.";
        default:
            return "";
        }
    }
}