/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
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

package io.kamax.matrix.client.regular;

import com.google.gson.JsonObject;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixContent;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._MatrixUser;
import io.kamax.matrix.client.*;
import io.kamax.matrix.hs._MatrixRoom;
import io.kamax.matrix.json.*;
import io.kamax.matrix.room.RoomAlias;
import io.kamax.matrix.room.RoomAliasLookup;
import io.kamax.matrix.room._RoomAliasLookup;
import io.kamax.matrix.room._RoomCreationOptions;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MatrixHttpClient extends AMatrixHttpClient implements _MatrixClient {

    public MatrixHttpClient(String domain) {
        super(domain);
    }

    public MatrixHttpClient(MatrixClientContext context) {
        super(context);
    }

    public MatrixHttpClient(MatrixClientContext context, MatrixClientDefaults defaults) {
        super(context, defaults);
    }

    public MatrixHttpClient(MatrixClientContext context, CloseableHttpClient client) {
        super(context, client);
    }

    protected _MatrixID getMatrixId(String localpart) {
        return new MatrixID(localpart, getHomeserver().getDomain());
    }

    @Override
    protected URIBuilder getClientPathBuilder(String action) {
        URIBuilder builder = super.getClientPathBuilder(action);
        context.getUser().ifPresent(user -> builder.setPath(builder.getPath().replace("{userId}", user.getId())));
        return builder;
    }

    @Override
    public _MatrixID getWhoAmI() {
        URI path = getClientPathWithAccessToken("/account/whoami");
        String body = execute(new HttpGet(path));
        return MatrixID.from(GsonUtil.getStringOrThrow(GsonUtil.parseObj(body), "user_id")).acceptable();
    }

    @Override
    public void setDisplayName(String name) {
        URI path = getClientPathWithAccessToken("/profile/{userId}/displayname");
        HttpPut req = new HttpPut(path);
        req.setEntity(getJsonEntity(new UserDisplaynameSetBody(name)));
        execute(req);
    }

    @Override
    public _RoomAliasLookup lookup(RoomAlias alias) {
        URI path = getClientPath("/directory/room/" + alias.getId());
        HttpGet req = new HttpGet(path);
        String resBody = execute(req);
        RoomAliasLookupJson lookup = GsonUtil.get().fromJson(resBody, RoomAliasLookupJson.class);
        return new RoomAliasLookup(lookup.getRoomId(), alias.getId(), lookup.getServers());
    }

    @Override
    public _MatrixRoom createRoom(_RoomCreationOptions options) {
        URI path = getClientPathWithAccessToken("/createRoom");
        HttpPost req = new HttpPost(path);
        req.setEntity(getJsonEntity(new RoomCreationRequestJson(options)));

        String resBody = execute(req);
        String roomId = GsonUtil.get().fromJson(resBody, RoomCreationResponseJson.class).getRoomId();
        return getRoom(roomId);
    }

    @Override
    public _MatrixRoom getRoom(String roomId) {
        return new MatrixHttpRoom(getContext(), roomId);
    }

    @Override
    public List<_MatrixRoom> getJoinedRooms() {
        URI path = getClientPathWithAccessToken("/joined_rooms");
        HttpGet req = new HttpGet(path);
        JsonObject resBody = GsonUtil.parseObj(execute(req));
        return GsonUtil.asList(resBody, "joined_rooms", String.class).stream().map(this::getRoom)
                .collect(Collectors.toList());
    }

    @Override
    public _MatrixRoom joinRoom(String roomIdOrAlias) {
        URI path = getClientPathWithAccessToken("/join/" + roomIdOrAlias);
        HttpPost req = new HttpPost(path);
        req.setEntity(getJsonEntity(new JsonObject()));

        String resBody = execute(req);
        String roomId = GsonUtil.get().fromJson(resBody, RoomCreationResponseJson.class).getRoomId();
        return getRoom(roomId);
    }

    @Override
    public _MatrixUser getUser(_MatrixID mxId) {
        return new MatrixHttpUser(getContext(), mxId);
    }

    @Override
    public Optional<String> getDeviceId() {
        return Optional.ofNullable(context.getDeviceId());
    }

    protected void updateContext(String resBody) {
        LoginResponse response = gson.fromJson(resBody, LoginResponse.class);
        context.setToken(response.getAccessToken());
        context.setDeviceId(response.getDeviceId());
        context.setUser(MatrixID.asValid(response.getUserId()));
    }

    @Override
    public void register(MatrixPasswordCredentials credentials, String sharedSecret, boolean admin) {
        // As per synapse registration script:
        // https://github.com/matrix-org/synapse/blob/master/scripts/register_new_matrix_user#L28
        String value = credentials.getLocalPart() + "\0" + credentials.getPassword() + "\0"
                + (admin ? "admin" : "notadmin");
        String mac = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, sharedSecret).hmacHex(value);
        JsonObject body = new JsonObject();
        body.addProperty("user", credentials.getLocalPart());
        body.addProperty("password", credentials.getPassword());
        body.addProperty("mac", mac);
        body.addProperty("type", "org.matrix.login.shared_secret");
        body.addProperty("admin", false);
        HttpPost req = new HttpPost(getPath("client", "api/v1", "/register"));
        req.setEntity(getJsonEntity(body));
        updateContext(execute(req));
    }

    @Override
    public void login(MatrixPasswordCredentials credentials) {
        HttpPost request = new HttpPost(getClientPath("/login"));

        LoginPostBody data = new LoginPostBody(credentials.getLocalPart(), credentials.getPassword());
        getDeviceId().ifPresent(data::setDeviceId);
        Optional.ofNullable(context.getInitialDeviceName()).ifPresent(data::setInitialDeviceDisplayName);
        request.setEntity(getJsonEntity(data));
        updateContext(execute(request));
    }

    @Override
    public void logout() {
        URI path = getClientPathWithAccessToken("/logout");
        HttpPost req = new HttpPost(path);
        execute(req);
        context.setToken(null);
        context.setUser(null);
        context.setDeviceId(null);
    }

    @Override
    public _SyncData sync(_SyncOptions options) {
        URIBuilder path = getClientPathBuilder("/sync");

        path.addParameter("timeout", options.getTimeout().map(Long::intValue).orElse(30000).toString());
        options.getSince().ifPresent(since -> path.addParameter("since", since));
        options.getFilter().ifPresent(filter -> path.addParameter("filter", filter));
        options.withFullState().ifPresent(state -> path.addParameter("full_state", state ? "true" : "false"));
        options.getSetPresence().ifPresent(presence -> path.addParameter("presence", presence));

        String body = execute(new HttpGet(getWithAccessToken(path)));
        return new SyncDataJson(GsonUtil.parseObj(body));
    }

    @Override
    public _MatrixContent getMedia(String mxUri) throws IllegalArgumentException {
        return getMedia(URI.create(mxUri));
    }

    @Override
    public _MatrixContent getMedia(URI mxUri) throws IllegalArgumentException {
        return new MatrixHttpContent(context, mxUri);
    }

    @Override
    public String putMedia(InputStream io, long length, String type) {
        HttpPost request = new HttpPost(getMediaPath("/upload"));
        request.setEntity(new InputStreamEntity(io, length));
        request.setHeader("Content-Type", type);
        String body = execute(request);
        return GsonUtil.getStringOrThrow(GsonUtil.parseObj(body), "content_uri");
    }

}
