/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2018 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.matrix.room;

import java.util.Optional;

public class MatrixRoomMessageChunckOptions implements _MatrixRoomMessageChunckOptions {

    public static class Builder {

        private MatrixRoomMessageChunckOptions obj;

        public Builder() {
            this.obj = new MatrixRoomMessageChunckOptions();
        }

        public Builder setFromToken(String token) {
            obj.from = token;
            return this;
        }

        public Builder setToToken(String token) {
            obj.to = token;
            return this;
        }

        public Builder setDirection(String direction) {
            obj.dir = direction;
            return this;
        }

        public Builder setDirection(_MatrixRoomMessageChunckOptions.Direction direction) {
            return setDirection(direction.get());
        }

        public Builder setLimit(long limit) {
            obj.limit = limit;
            return this;
        }
    }

    public static Builder build() {
        return new Builder();
    }

    private String from;
    private String to;
    private String dir;
    private Long limit;

    @Override
    public String getFromToken() {
        return from;
    }

    @Override
    public Optional<String> getToToken() {
        return Optional.ofNullable(to);
    }

    @Override
    public String getDirection() {
        return dir;
    }

    @Override
    public Optional<Long> getLimit() {
        return Optional.ofNullable(limit);
    }

}
