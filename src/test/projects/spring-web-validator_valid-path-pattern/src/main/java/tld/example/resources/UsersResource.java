/*
 * Copyright 2015 Hubrick
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

package tld.example.resources;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tld.example.resources.model.User;
import tld.example.util.RegexpPatterns;

@RestController(value = "/users")
public class UsersResource {

    @Override
    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public void postUsers(@RequestBody User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    @RequestMapping(value = "/users/{userId:" + RegexpPatterns.UUID + "}", method = RequestMethod.GET)
    public User getUsers(@PathVariable("userId") final UUID userId) {
        throw new UnsupportedOperationException();
    }

}
