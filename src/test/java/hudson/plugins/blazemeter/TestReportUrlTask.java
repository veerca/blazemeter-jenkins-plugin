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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.blazemeter.utils.report.ReportUrlTask;
import hudson.remoting.VirtualChannel;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class TestReportUrlTask {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void run() {
        try {

            FreeStyleProject project = j.createFreeStyleProject();
            FreeStyleBuild b = new FreeStyleBuild(project);
            FreeStyleBuild bm= Mockito.spy(b);
            VirtualChannel c = j.getInstance().getChannel();
            ReportUrlTask t = new ReportUrlTask(bm, "name", c);
            PerformanceBuildAction ba = new PerformanceBuildAction(bm);
            t.run();
            Mockito.verify(bm, times(1)).getId();
            Mockito.verify(bm, never()).addAction(ba);
            Assert.assertEquals(bm.getAllActions().size(),0);
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
