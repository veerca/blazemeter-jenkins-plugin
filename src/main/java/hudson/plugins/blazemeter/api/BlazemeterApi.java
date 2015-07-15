package hudson.plugins.blazemeter.api;

import com.google.common.collect.LinkedHashMultimap;
import hudson.plugins.blazemeter.api.urlmanager.BmUrlManager;
import hudson.plugins.blazemeter.entities.TestInfo;
import org.eclipse.jetty.util.log.StdErrLog;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * User: Vitali
 * Date: 4/2/12
 * Time: 14:05
 * <p/>
 * Updated
 * User: Doron
 * Date: 8/7/12
 *
 * Updated (proxy)
 * User: Marcel
 * Date: 9/23/13

 */

public interface BlazemeterApi {

    String APP_KEY = "jnk100x987c06f4e10c4";

    void uploadJmx(String testId, File file);

    JSONObject uploadBinaryFile(String testId, File file);

    TestInfo getTestInfo(String id);

    int getTestSessionStatusCode(String id);

    JSONObject startTest(String testId);

    int getTestCount() throws JSONException, IOException, ServletException;

    JSONObject stopTest(String testId);

    JSONObject terminateTest(String testId);

    JSONObject testReport(String reportId);

    LinkedHashMultimap<String, String> getTestsMultiMap() throws IOException, MessagingException;

    JSONObject getTestsJSON();

    JSONObject getUser();

    JSONObject getTresholds(String sessionId);

    JSONObject getTestConfig(String testId);

    JSONObject postJsonConfig(String testId, JSONObject data);

    JSONObject createTest(JSONObject data);

    String retrieveJUNITXML(String sessionId);

    JSONObject retrieveJTLZIP(String sessionId);

    JSONObject putTestInfo(String testId, JSONObject data);

    void setBzmHttpWr(BzmHttpWrapper bzmhc);

    BzmHttpWrapper getBzmHttpWr();

    StdErrLog getLogger();

    void setLogger(StdErrLog logger);

    JSONObject generatePublicToken(String sessionId);

    String getApiKey();

    String getBlazeMeterURL();

    BmUrlManager getUrlManager();
}
