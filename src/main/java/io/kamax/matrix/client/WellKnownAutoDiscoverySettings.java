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

package io.kamax.matrix.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.InvalidJsonException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WellKnownAutoDiscoverySettings implements _AutoDiscoverySettings {

    private final Logger log = LoggerFactory.getLogger(WellKnownAutoDiscoverySettings.class);

    private JsonObject raw;

    private List<URL> hsBaseUrls = new ArrayList<>();
    private List<URL> isBaseUrls = new ArrayList<>();

    /**
     * Build .well-known auto-discovery settings from a .well-known source.
     * 
     * @param raw
     *            The raw JSON data
     * @throws IllegalArgumentException
     *             if the data is invalid and couldn't be parsed.
     */
    public WellKnownAutoDiscoverySettings(String raw) {
        try {
            setRaw(GsonUtil.parseObj(raw));
        } catch (JsonParseException | InvalidJsonException e) {
            throw new IllegalArgumentException("Invalid JSON data for .well-known string");
        }
    }

    private void setRaw(JsonObject raw) {
        this.raw = raw;
        process();
    }

    private List<URL> getUrls(JsonArray array) {
        List<URL> urls = new ArrayList<>();

        array.forEach(el -> {
            if (!el.isJsonPrimitive()) {
                log.warn("Ignoring invalid Base URL entry in well-known: {}", GsonUtil.get().toJson(el));
                return;
            }

            String rawUrl = el.getAsString();
            try {
                urls.add(new URL(rawUrl));
            } catch (MalformedURLException e) {
                log.warn("Ignoring invalid Base URL entry in well-known: {}", rawUrl);
            }
        });

        return urls;
    }

    private void process() {
        log.info("Processing Homeserver Base URLs");
        GsonUtil.findObj(raw, "m.homeserver").ifPresent(cfg -> {
            log.info("Found Homeserver data");
            GsonUtil.findArray(cfg, "base_urls").ifPresent(arr -> {
                log.info("Found base URL(s)");
                hsBaseUrls = getUrls(arr);
            });
        });
        log.info("Found {} valid URL(s)", hsBaseUrls.size());

        log.info("Processing Identity server Base URLs");
        GsonUtil.findObj(raw, "m.identity_server").ifPresent(cfg -> {
            log.info("Found Identity server data");
            GsonUtil.findArray(cfg, "base_urls").ifPresent(arr -> {
                log.info("Found base URL(s)");
                isBaseUrls = getUrls(arr);
            });
        });
        log.info("Found {} valid URL(s)", isBaseUrls.size());
    }

    @Override
    public JsonObject getRaw() {
        return raw;
    }

    @Override
    public List<URL> getHsBaseUrls() {
        return Collections.unmodifiableList(hsBaseUrls);
    }

    @Override
    public List<URL> getIsBaseUrls() {
        return Collections.unmodifiableList(isBaseUrls);
    }

}
