/*
 * Copyright (C) 2013 Redsolution LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidrss;

/**
 * Internal helper class for integer conversions.
 *
 * @author Alexander Ivanov
 */
final class Integers {

    /* Hide constructor */
    private Integers() {
    }

    /**
     * Parses string as an integer.
     *
     * @throws RSSFault if the string is not a valid integer
     */
    static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new RSSFault(e);
        }
    }

}
