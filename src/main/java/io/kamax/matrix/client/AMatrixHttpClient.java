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

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import io.kamax.matrix.MatrixErrorInfo;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs._MatrixHomeserver;
import io.kamax.matrix.json.LoginPostBody;
import io.kamax.matrix.json.LoginResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AMatrixHttpClient implements _MatrixClientRaw {

    private Logger log = LoggerFactory.getLogger(AMatrixHttpClient.class);

    protected MatrixClientContext context;

    protected Gson gson = new Gson();
    protected JsonParser jsonParser = new JsonParser();
    private CloseableHttpClient client = HttpClients.createDefault();
    private Pattern accessTokenUrlPattern = Pattern.compile("\\?access_token=(?<token>[^&]*)");

    public AMatrixHttpClient(MatrixClientContext context) {
        this.context = context;
        if (!context.getToken().isPresent()) {
            login();
        }
    }

    @Override
    public MatrixClientContext getContext() {
        return context;
    }

    @Override
    public _MatrixHomeserver getHomeserver() {
        return context.getHs();
    }

    @Override
    public String getAccessToken() {
        return context.getToken()
                .orElseThrow(() -> new IllegalStateException("This method can only be used with a valid token."));
    }

    @Override
    public _MatrixID getUser() {
        return context.getUser();
    }

    @Override
    public Optional<String> getDeviceId() {
        return context.getDeviceId();
    }

    @Override
    public void logout() {
        URI path = getClientPath("/logout");
        HttpPost req = new HttpPost(path);
        execute(req);
    }

    protected String execute(HttpRequestBase request) {
        return execute(new MatrixHttpRequest(request));
    }

    protected String execute(MatrixHttpRequest matrixRequest) {
        log(matrixRequest.getHttpRequest());
        try (CloseableHttpResponse response = client.execute(matrixRequest.getHttpRequest())) {

            String body = getBody(response.getEntity());
            int responseStatus = response.getStatusLine().getStatusCode();

            if (responseStatus == 200) {
                log.debug("Request successfully executed.");
            } else if (matrixRequest.getIgnoredErrorCodes().contains(responseStatus)) {
                log.debug("Error code ignored: " + responseStatus);
                return "";
            } else {
                MatrixErrorInfo info = createErrorInfo(body, responseStatus);

                body = handleError(matrixRequest, responseStatus, info);
            }
            return body;

        } catch (IOException e) {
            throw new MatrixClientRequestException(e);
        }
    }

    /**
     * Default handling of errors. Can be overwritten by a custom implementation in inherited classes.
     *
     * @param matrixRequest
     * @param responseStatus
     * @param info
     * @return body of the response of a repeated call of the request, else this methods throws a
     *         MatrixClientRequestException
     */
    protected String handleError(MatrixHttpRequest matrixRequest, int responseStatus, MatrixErrorInfo info) {
        String message = String.format("Request failed with status code: %s", responseStatus);

        if (responseStatus == 429) {
            return handleRateLimited(matrixRequest, info);
        }

        throw new MatrixClientRequestException(info, message);
    }

    /**
     * Default handling of rate limited calls. Can be overwritten by a custom implementation in inherited classes.
     *
     * @param matrixRequest
     * @param info
     * @return body of the response of a repeated call of the request, else this methods throws a
     *         MatrixClientRequestException
     */
    protected String handleRateLimited(MatrixHttpRequest matrixRequest, MatrixErrorInfo info) {
        throw new MatrixClientRequestException(info, "Request was rate limited.");
        // TODO Add default handling of rate limited call, i.e. repeated call after given time interval.
        // 1. Wait for timeout
        // 2. return execute(request)
    }

    protected MatrixHttpContentResult executeContentRequest(MatrixHttpRequest matrixRequest) {
        log(matrixRequest.getHttpRequest());
        try (CloseableHttpResponse response = client.execute(matrixRequest.getHttpRequest())) {

            HttpEntity entity = response.getEntity();
            int responseStatus = response.getStatusLine().getStatusCode();

            MatrixHttpContentResult result = new MatrixHttpContentResult(response);

            if (responseStatus == 200) {
                log.debug("Request successfully executed.");

                if (entity == null) {
                    log.debug("No data received.");
                } else if (entity.getContentType() == null) {
                    log.debug("No content type was given.");
                }

            } else if (matrixRequest.getIgnoredErrorCodes().contains(responseStatus)) {
                log.debug("Error code ignored: " + responseStatus);
            } else {
                String body = getBody(entity);
                MatrixErrorInfo info = createErrorInfo(body, responseStatus);

                result = handleErrorContentRequest(matrixRequest, responseStatus, info);
            }
            return result;

        } catch (IOException e) {
            throw new MatrixClientRequestException(e);
        }
    }

    protected MatrixHttpContentResult handleErrorContentRequest(MatrixHttpRequest matrixRequest, int responseStatus,
            MatrixErrorInfo info) {
        String message = String.format("Request failed with status code: %s", responseStatus);

        if (responseStatus == 429) {
            return handleRateLimitedContentRequest(matrixRequest, info);
        }

        throw new MatrixClientRequestException(info, message);
    }

    protected MatrixHttpContentResult handleRateLimitedContentRequest(MatrixHttpRequest matrixRequest,
            MatrixErrorInfo info) {
        throw new MatrixClientRequestException(info, "Request was rate limited.");
        // TODO Add default handling of rate limited call, i.e. repeated call after given time interval.
        // 1. Wait for timeout
        // 2. return execute(request)
    }

    protected Optional<String> extractAsStringFromBody(String body, String jsonObjectName) {
        if (StringUtils.isNotEmpty(body)) {
            return Optional.of(new JsonParser().parse(body).getAsJsonObject().get(jsonObjectName).getAsString());
        }
        return Optional.empty();
    }

    private String getBody(HttpEntity entity) throws IOException {
        Charset charset = ContentType.getOrDefault(entity).getCharset();
        return IOUtils.toString(entity.getContent(), charset);
    }

    private MatrixErrorInfo createErrorInfo(String body, int responseStatus) {
        MatrixErrorInfo info = gson.fromJson(body, MatrixErrorInfo.class);
        log.debug("Request returned with an error. Status code: {}, errcode: {}, error: {}", responseStatus,
                info.getErrcode(), info.getError());
        return info;
    }

    private void log(HttpRequestBase req) {
        String reqUrl = req.getURI().toASCIIString();
        Matcher m = accessTokenUrlPattern.matcher(reqUrl);
        if (m.find()) {
            StringBuilder b = new StringBuilder();
            b.append(reqUrl.substring(0, m.start("token")));
            b.append("<redacted>");
            b.append(reqUrl.substring(m.end("token"), reqUrl.length()));
            reqUrl = b.toString();
        }

        log.debug("Doing {} {}", req.getMethod(), reqUrl);
    }

    private URIBuilder getClientPathBuilder(URI path) {
        URIBuilder builder = new URIBuilder(path);

        Optional<String> token = context.getToken();
        builder.setParameter("access_token",
                token.orElseThrow(() -> new IllegalStateException("This method can only be used with a valid token.")));

        if (context.isVirtualUser()) {
            builder.addParameter("user_id", context.getUser().getId());
        }
        return builder;
    }

    protected URI getPath(String module, String version, String action) throws URISyntaxException {
        return new URI(context.getHs().getClientEndpoint() + "/_matrix/" + module + "/" + version + action);
    }

    protected URI getClientPath(String action) {
        try {
            return getClientPathBuilder(getPath("client", "r0", action)).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected URI getMediaPath(String action) {
        try {
            return getClientPathBuilder(getPath("media", "v1", action)).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected HttpEntity getJsonEntity(Object o) {
        return EntityBuilder.create().setText(gson.toJson(o)).setContentType(ContentType.APPLICATION_JSON).build();
    }

    private void login() {
        HttpPost request = new HttpPost(getLoginClientPath());
        _MatrixID user = context.getUser();
        String password = context.getPassword()
                .orElseThrow(() -> new IllegalStateException("You have to provide a password to be able to login."));
        if (context.getDeviceId().isPresent()) {
            request.setEntity(
                    getJsonEntity(new LoginPostBody(user.getLocalPart(), password, context.getDeviceId().get())));
        } else {
            request.setEntity(getJsonEntity(new LoginPostBody(user.getLocalPart(), password)));
        }

        String body = execute(request);
        LoginResponse response = gson.fromJson(body, LoginResponse.class);
        context.setToken(response.getAccessToken());
        context.setDeviceId(response.getDeviceId());
    }

    private URI getLoginClientPath() {
        try {
            return new URIBuilder(getPath("client", "r0", "/login")).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
