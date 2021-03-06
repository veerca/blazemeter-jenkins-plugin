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

import hudson.plugins.blazemeter.utils.Constants;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class TestPerformanceBuilderDescriptor {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    public BlazeMeterPerformanceBuilderDescriptor bd=null;

    @Test
    public void constructor() {
        bd = new BlazeMeterPerformanceBuilderDescriptor();
        Assert.assertEquals(Constants.A_BLAZEMETER_COM, bd.getBlazeMeterURL());
        bd = new BlazeMeterPerformanceBuilderDescriptor(Constants.A_BLAZEMETER_COM + 123);
        Assert.assertEquals(Constants.A_BLAZEMETER_COM + 123, bd.getBlazeMeterURL());

    }

    @Test
    public void no_api_key() {
       BlazeMeterPerformanceBuilderDescriptor bd = new BlazeMeterPerformanceBuilderDescriptor();
        try {
            ListBoxModel lbm = bd.doFillTestIdItems(TestConstants.MOCKED_USER_KEY_VALID, TestConstants.TEST_5039530_ID);
            Assert.assertTrue(lbm.size()==1);
            Assert.assertEquals(lbm.get(0).name,Constants.NO_API_KEY);
        } catch (FormValidation formValidation) {
            formValidation.printStackTrace();
        }
    }

    @Test
    public void do_fill_api_key_items() {
        BlazeMeterPerformanceBuilderDescriptor bd = new BlazeMeterPerformanceBuilderDescriptor();
        try {
            bd.setBlazeMeterURL(TestConstants.mockedApiUrl);
            ListBoxModel itemds=bd.doFillJobApiKeyItems(TestConstants.MOCKED_USER_KEY_VALID);
            Assert.assertEquals(0,itemds.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
