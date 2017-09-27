/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2017 Maxime Dor
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

import com.google.gson.JsonObject;

import io.kamax.matrix.MatrixErrorInfo;
import io.kamax.matrix._MatrixContent;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._MatrixUser;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Optional;

public class MatrixHttpUser extends AMatrixHttpClient implements _MatrixUser {

    private Logger log = LoggerFactory.getLogger(MatrixHttpUser.class);

    private _MatrixID mxId;

    public MatrixHttpUser(MatrixClientContext context, _MatrixID mxId) {
        super(context);

        this.mxId = mxId;
    }

    @Override
    public _MatrixID getId() {
        return mxId;
    }

    @Override
    public Optional<String> getName() {
        try {
            URI path = getClientPath("/profile/" + mxId.getId() + "/displayname");
            try (CloseableHttpResponse res = client.execute(log(new HttpGet(path)))) {
                Charset charset = ContentType.getOrDefault(res.getEntity()).getCharset();
                String body = IOUtils.toString(res.getEntity().getContent(), charset);

                if (res.getStatusLine().getStatusCode() != 200) {
                    if (res.getStatusLine().getStatusCode() == 404) {
                        // No display name has been set
                        return Optional.empty();
                    }

                    // TODO handle rate limited
                    if (res.getStatusLine().getStatusCode() == 429) {
                        log.warn("Request was rate limited", new Exception());
                        return Optional.empty();
                    }

                    MatrixErrorInfo info = gson.fromJson(body, MatrixErrorInfo.class);
                    log.error("Couldn't get the displayname of {}: {} - {}", mxId, info.getErrcode(), info.getError());
                    return Optional.empty();
                }

                JsonObject displayNameObj = jsonParser.parse(body).getAsJsonObject();
                if (!displayNameObj.has("displayname")) {
                    return Optional.empty();
                }

                return Optional.of(displayNameObj.get("displayname").getAsString());
            }
        } catch (IOException e) {
            throw new MatrixClientRequestException(e);
        }
    }

    @Override
    public Optional<_MatrixContent> getAvatar() {
        try {
            URI path = getClientPath("/profile/" + mxId.getId() + "/avatar_url");
            try (CloseableHttpResponse res = client.execute(log(new HttpGet(path)))) {
                Charset charset = ContentType.getOrDefault(res.getEntity()).getCharset();
                String body = IOUtils.toString(res.getEntity().getContent(), charset);
                log.info("Got body");

                if (res.getStatusLine().getStatusCode() != 200) {
                    if (res.getStatusLine().getStatusCode() == 404) {
                        // No avatar url has been set
                        return Optional.empty();
                    }

                    // TODO handle rate limited
                    if (res.getStatusLine().getStatusCode() == 429) {
                        log.warn("Request was rate limited", new Exception());
                        return Optional.empty();
                    }

                    MatrixErrorInfo info = gson.fromJson(body, MatrixErrorInfo.class);
                    log.error("Couldn't get the avatar_url of {}: {} - {}", mxId, info.getErrcode(), info.getError());
                    return Optional.empty();
                }

                JsonObject urlObj = jsonParser.parse(body).getAsJsonObject();
                if (!urlObj.has("avatar_url")) {
                    return Optional.empty();
                }

                String uriRaw = urlObj.get("avatar_url").getAsString();
                log.info("Avatar URI of {}: {}", mxId, uriRaw);
                try {
                    return Optional.of(new MatrixHttpContent(getContext(), new URI(uriRaw)));
                } catch (URISyntaxException e) {
                    log.warn("{} is not a valid URI for avatar, returning empty", uriRaw);
                    return Optional.empty();
                }
            }
        } catch (IOException e) {
            throw new MatrixClientRequestException(e);
        }

    }

}
