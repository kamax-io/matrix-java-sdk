/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2017 Maxime Dor
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

package io.kamax.matrix.hs;

import io.kamax.matrix._MatrixID;
import io.kamax.matrix.room._MatrixRoomMessageChunck;
import io.kamax.matrix.room._MatrixRoomMessageChunckOptions;

import java.util.List;
import java.util.Optional;

public interface _MatrixRoom {

    _MatrixHomeserver getHomeserver();

    String getAddress();

    Optional<String> getName();

    Optional<String> getTopic();

    void join();

    void leave();

    void sendText(String message);

    void sendFormattedText(String formatted, String rawFallback);

    void sendNotice(String message);

    void sendNotice(String formatted, String plain);

    void invite(_MatrixID mxId);

    List<_MatrixID> getJoinedUsers();

    _MatrixRoomMessageChunck getMessages(_MatrixRoomMessageChunckOptions options);

}
