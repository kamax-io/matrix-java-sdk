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

import com.github.tomakehurst.wiremock.client.MappingBuilder;

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public abstract class AMatrixHttpPutTester extends AMatrixHttpTester {
    protected final Consumer<String> putMethod;
    protected final String valueToConsume;

    public AMatrixHttpPutTester(Consumer<String> putMethod, String valueToConsume) {
        this.putMethod = putMethod;
        this.valueToConsume = valueToConsume;
    }

    @Override
    protected MappingBuilder createUrlMappingBuilder(String url) {
        return put(urlEqualTo(url));
    }
}
