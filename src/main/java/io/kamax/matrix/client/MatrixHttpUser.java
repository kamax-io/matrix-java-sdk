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

import io.kamax.matrix._MatrixContent;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._MatrixUser;

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
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
        URI path = getClientPathWithAccessToken("/profile/" + mxId.getId() + "/displayname");

        MatrixHttpRequest request = new MatrixHttpRequest(new HttpGet(path));
        request.addIgnoredErrorCode(404);
        String body = execute(request);
        return extractAsStringFromBody(body, "displayname");
    }

    @Override
    public Optional<_MatrixContent> getAvatar() {
        URI path = getClientPathWithAccessToken("/profile/" + mxId.getId() + "/avatar_url");

        MatrixHttpRequest request = new MatrixHttpRequest(new HttpGet(path));
        request.addIgnoredErrorCode(404);
        String body = execute(request);
        Optional<String> uri = extractAsStringFromBody(body, "avatar_url");
        if (uri.isPresent()) {
            try {
                return Optional.of(new MatrixHttpContent(getContext(), new URI(uri.get())));
            } catch (URISyntaxException e) {
                log.debug("{} is not a valid URI for avatar, returning empty", uri.get());
            }
        }
        return Optional.empty();
    }
}
