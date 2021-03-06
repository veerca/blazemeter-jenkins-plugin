/**
 Copyright 2016 BlazeMeter Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package hudson.plugins.blazemeter;

import org.junit.Assert;
import org.junit.Test;

public class TestBlazeterCredentialImpl {

    @Test
    public void id() {
        String ak = "e7d12123456rpqifveej";
        String description = "e7d12123456rpqifveejdescription";
        String expectedId = "e7d1...veej";
        BlazemeterCredentialImpl c = new BlazemeterCredentialImpl(ak, description);
        Assert.assertEquals(expectedId,c.getId());
    }
}
