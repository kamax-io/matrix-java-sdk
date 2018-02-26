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

package io.kamax.matrix.json.event;

import com.google.gson.JsonObject;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.event._MatrixEvent;
import io.kamax.matrix.json.MatrixJsonObject;

import java.time.Instant;

public class MatrixJsonEvent extends MatrixJsonObject implements _MatrixEvent {

    private String id;
    private String type;
    private Instant time;
    private int age;
    private _MatrixID sender;

    public MatrixJsonEvent(JsonObject obj) {
        super(obj);

        id = getString("event_id");
        type = getString("type");
        time = Instant.ofEpochMilli(obj.get("origin_server_ts").getAsLong());
        age = getInt("age", -1);
        sender = MatrixID.asAcceptable(getString("sender"));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Instant getTime() {
        return time;
    }

    @Override
    public _MatrixID getSender() {
        return sender;
    }

}
