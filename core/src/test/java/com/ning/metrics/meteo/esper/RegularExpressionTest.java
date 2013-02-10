/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.meteo.esper;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpressionTest
{
    @Test
    public void slashTest()
    {
        String pattern = "($|/?+((\\?|\\#|\\&).*$)?+)";
        Assert.assertTrue(testPattern("/main/index", "^/main/index" + pattern));
        Assert.assertTrue(testPattern("/main/index/", "^/main/index" + pattern));
        Assert.assertTrue(testPattern("/main/index?foo=bar", "^/main/index" + pattern));
        Assert.assertTrue(testPattern("/main/index/?foo=bar", "^/main/index" + pattern));
        Assert.assertFalse(testPattern("/main/index/admin", "^/main/index" + pattern));
        Assert.assertFalse(testPattern("/main/index/admin?foo=bar", "^/main/index" + pattern));

        Assert.assertTrue(testPattern("/photo/foo/bar", "^/photo/.*"));
        Assert.assertFalse(testPattern("/main/photo/foo", "^/photo/.*"));
    }

    private boolean testPattern(String s, String pattern)
    {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(s);
        return m.matches();
    }
}
