package hudson.plugins.blazemeter.utils;

import hudson.FilePath;
import hudson.model.Result;
import hudson.plugins.blazemeter.BlazeMeterPerformanceBuilderDescriptor;
import hudson.plugins.blazemeter.BlazemeterCredential;
import hudson.plugins.blazemeter.PerformanceBuilder;
import hudson.plugins.blazemeter.api.APIFactory;
import hudson.plugins.blazemeter.api.BlazemeterApi;
import hudson.plugins.blazemeter.entities.TestInfo;
import hudson.plugins.blazemeter.entities.TestStatus;
import hudson.plugins.blazemeter.testresult.TestResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.StdErrLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by dzmitrykashlach on 18/11/14.
 */
public class BzmServiceManager {
    private final static int BUFFER_SIZE = 2048;
    private final static String ZIP_EXTENSION = ".zip";
    private BzmServiceManager() {
    }

    public static String autoDetectApiVersion(String apiKey, AbstractLogger logger) {
        BlazemeterApi api = null;
        APIFactory apiFactory = APIFactory.getApiFactory();
        String detectedApiVersion = null;
            api = apiFactory.getApiFactory().getAPI(apiKey,APIFactory.ApiVersion.v3);
            boolean isV3 = false;
            try {
                isV3 = api.getUser().getJSONObject("features").getBoolean("v3");
                if (isV3) {
                    detectedApiVersion="v3";
                } else {
                    detectedApiVersion="v2";
                }
            } catch (JSONException je) {
                logger.warn("Received JSONException while auto-detecting version: ", je);
            } catch (NullPointerException npe) {
                logger.warn("Received JSONException while auto-detecting version: ", npe);
                return "v3";
            }
        return detectedApiVersion;
    }


    public static JSONObject updateTest(BlazemeterApi api,
                                  String testId,
                                  String updDuration,
                                  JSONObject configNode,
                                  StdErrLog bzmBuildLog) {
        JSONObject updateResult=null;
        try {
            JSONObject result = null;
            if (configNode != null) {
                result=configNode;
                updateResult=api.postJsonConfig(testId, result);
            } else if (updDuration != null && !updDuration.isEmpty()) {
                JSONObject jo = api.getTestInfo(testId);
                result = jo.getJSONObject("result");
                JSONObject configuration = result.getJSONObject("configuration");
                JSONObject plugins = configuration.getJSONObject("plugins");
                String type = configuration.getString("type");
                JSONObject options = plugins.getJSONObject(type);
                JSONObject override = options.getJSONObject("override");
                override.put("duration", updDuration);
                override.put("threads", JSONObject.NULL);
                configuration.put("serversCount", JSONObject.NULL);
                updateResult=api.putTestInfo(testId, result);
            }

        } catch (JSONException je) {
            bzmBuildLog.warn("Received JSONException while saving testDuration: ", je);
        } catch (Exception e) {
            bzmBuildLog.warn("Received JSONException while saving testDuration: ", e);
        }
        return updateResult;
    }

    public static String requestTestDuration(BlazemeterApi api, String testId, StdErrLog bzmBuildLog) {
        String duration = null;
        try {
            JSONObject jo = api.getTestInfo(testId);
            JSONObject result = jo.getJSONObject("result");
            JSONObject configuration = result.getJSONObject("configuration");
            JSONObject plugins = configuration.getJSONObject("plugins");
            String type = configuration.getString("type");
            JSONObject options = plugins.getJSONObject(type);
            JSONObject override = options.getJSONObject("override");
            duration = override.getString("duration");

        } catch (JSONException je) {
            bzmBuildLog.warn("Received JSONException while requesting testDuration: ", je);
        } catch (Exception e) {
            bzmBuildLog.warn("Received Exception while requesting testDuration: ", e);
        }
        return duration;
    }

    public static void uploadDataFolderFiles(String dataFolder, String mainJMX, String testId,
                                             BlazemeterApi bmAPI, StdErrLog bzmBuildLog) {

        if (dataFolder == null || dataFolder.isEmpty())
            return;

        File folder = new File(dataFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            bzmBuildLog.info("dataFolder " + dataFolder + " could not be found on local file system, please check that the folder exists.");
            return;
        }

        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                String fileName;
                if (file.isFile()) {
                    fileName = file.getName();

                    if (fileName.endsWith(mainJMX))
                        bmAPI.uploadJmx(testId, file);
                    else
                        uploadFile(testId, bmAPI, file, bzmBuildLog);
                }
            }
        }
    }

    public static void waitForFinish(BlazemeterApi api, String apiVersion, String testId, AbstractLogger bzmBuildLog,
                                     String session) throws InterruptedException {
        Date start = null;
        long lastPrint = 0;
        while (true) {
            Thread.sleep(15000);
            TestInfo info = api.getTestRunStatus(apiVersion.equals("v2") ? testId : session);

            if (!info.getStatus().equals(TestStatus.Running)) {
                bzmBuildLog.info("TestStatus for session " + (apiVersion.equals("v2") ? testId : session)
                        + info.getStatus());
                bzmBuildLog.info("BlazeMeter TestStatus for session" +
                        (apiVersion.equals("v2") ? testId : session)
                        + " is not 'Running': finishing build.... ");
                bzmBuildLog.info("Timestamp: " + Calendar.getInstance().getTime());
                break;
            }

            if (start == null)
                start = Calendar.getInstance().getTime();
            long now = Calendar.getInstance().getTime().getTime();
            long diffInSec = (now - start.getTime()) / 1000;
            if (now - lastPrint > 10000) { //print every 10 sec.
                bzmBuildLog.info("BlazeMeter test# " + testId + ", session # " + session + " running from " + start + " - for " + diffInSec + " seconds");
                lastPrint = now;
            }

            if (Thread.interrupted()) {
                bzmBuildLog.info("Test was interrupted: throwing Interrupted Exception");
                throw new InterruptedException();
            }
        }
    }

    private static String createTest(BlazemeterApi api, JSONObject configNode,
                                     String testId,StdErrLog jenBuildLog) throws JSONException {
        try{

        if(testId.equals(Constants.CREATE_BZM_TEST_NOTE)){
            JSONObject jo = api.createTest(configNode);
            if(jo.has("error")&&!jo.getString("error").equals("null")){
                jenBuildLog.warn("Failed to create test: "+jo.getString("error"));
                testId="";
            }else{
                testId = jo.getJSONObject("result").getString("id");
            }
        }
        }catch (Exception e){
            jenBuildLog.warn("Unable to create test: check user-key and server-url");
        }

        return testId;
    }

    public static String getReportUrl(BlazemeterApi api, String sessionId,
                                      StdErrLog jenBuildLog, StdErrLog bzmBuildLog) {
        JSONObject jo=null;
        String publicToken="";
        String reportUrl=null;
        try {
            jo = api.generatePublicToken(sessionId);
            if(jo.get("error").equals(JSONObject.NULL)){
                JSONObject result=jo.getJSONObject("result");
                publicToken=result.getString("publicToken");
                reportUrl=APIFactory.getApiFactory().getBlazeMeterUrl()+"/app/?public-token="+publicToken+"#reports/"+sessionId+"/summary";
            }else{
                jenBuildLog.warn("Problems with generating public-token for report URL: "+jo.get("error").toString());
                bzmBuildLog.warn("Problems with generating public-token for report URL: "+jo.get("error").toString());
                reportUrl=APIFactory.getApiFactory().getBlazeMeterUrl()+"/app/#reports/"+sessionId+"/summary";
            }
        } catch (Exception e){
          jenBuildLog.warn("Problems with generating public-token for report URL");
          bzmBuildLog.warn("Problems with generating public-token for report URL",e);
        }finally {
                return reportUrl;
        }
    }

    public static String prepareTestRun(PerformanceBuilder builder) {
        BlazemeterApi api = builder.getApi();
        FilePath jsonConfigPath = null;
        StdErrLog bzmBuildLog = PerformanceBuilder.getBzmBuildLog();
        StdErrLog jenBuildLog = PerformanceBuilder.getJenBuildLog();
        String testId = builder.getTestId();
        try {
            JSONObject configNode=null;
            if(!StringUtils.isBlank(builder.getJsonConfig())){
                jsonConfigPath=new FilePath(builder.getBuild().getWorkspace(), builder.getJsonConfig());
                configNode = new JSONObject(jsonConfigPath.readToString());
            }

            if (testId.contains("create")) {
                if (configNode != null) {
                    testId = createTest(api, configNode, testId, jenBuildLog);
                    builder.setTestId(testId);
                } else {
                    testId="";
                    return testId;
                }
            }


            if(configNode!=null|!builder.getTestDuration().isEmpty()) {
                JSONObject updateResult=updateTest(api,testId,builder.getTestDuration(), configNode, bzmBuildLog);
                if(updateResult.has("error")&&!updateResult.get("error").equals(null)){
                    jenBuildLog.warn("Failed to update test with JSON configuration");
                    jenBuildLog.warn("Error:"+updateResult.getString("error"));
                    testId="";
                }else{
                    jenBuildLog.info("Test "+testId+" was started on server");
                    }
            }

            String testDuration = (builder.getTestDuration() != null && !builder.getTestDuration().isEmpty()) ?
                    builder.getTestDuration() : requestTestDuration(api, builder.getTestId(), bzmBuildLog);
            builder.setTestDuration(testDuration);
        } catch (IOException e) {
            jenBuildLog.info("Failed to read JSON configuration from file " + builder.getJsonConfig() + ": " + e.getMessage());
            bzmBuildLog.info("Failed to read JSON configuration from file " + builder.getJsonConfig() + ": " + e.getMessage());
            testId="";
        } catch (JSONException je) {
            jenBuildLog.info("Failed to read JSON configuration from file " + builder.getJsonConfig() + ": " + je.getMessage());
            bzmBuildLog.info("Failed to read JSON configuration from file " + builder.getJsonConfig() + ": " + je.getMessage());
            testId="";
        } catch (Exception e){
            jenBuildLog.info("Unknown error while preparing test for execution: " +e.getMessage());
            bzmBuildLog.info("Unknown error while preparing test for execution: " +e.getMessage());
            testId="";
        }

        finally {
/*          TODO
            These calls are not implemented for APIv3
            Should be fixed in v.2.1

            uploadDataFolderFiles(builder.getDataFolder(),builder.getMainJMX(),testId, api,bzmBuildLog);
*/
            return testId;
        }
    }

    public static void uploadFile(String testId, BlazemeterApi bmAPI, File file, StdErrLog bzmBuildLog) {
        String fileName = file.getName();
        org.json.JSONObject json = bmAPI.uploadBinaryFile(testId, file);
        try {
            if (!json.get("response_code").equals(200)) {
                bzmBuildLog.info("Could not upload file " + fileName + " " + json.get("error").toString());
            }
        } catch (JSONException e) {
            bzmBuildLog.info("Could not upload file " + fileName + " " + e.getMessage());
        }
    }

    public static void saveReport(String filename,
                                  String report,
                                  FilePath filePath,
                                  StdErrLog bzmBuildLog,
                                  StdErrLog jenBuildLog) {
        File reportFile = new File(filePath.getParent()
                + "/" + filePath.getName() + "/" + filename + ".xml");
        try {
            if (!reportFile.exists()) {
                reportFile.createNewFile();
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(reportFile));
            out.write(report);
            out.close();

        } catch (FileNotFoundException fnfe) {
            bzmBuildLog.info("ERROR: Failed to save XML report to workspace " + fnfe.getMessage());
            jenBuildLog.info("Unable to save XML report to workspace - check that test is finished on server or turn to support ");
        } catch (IOException e) {
            bzmBuildLog.info("ERROR: Failed to save XML report to workspace " + e.getMessage());
            jenBuildLog.info("Unable to save XML report to workspace - check that test is finished on server or turn to support ");
        }
    }

    public static Result validateServerTresholds(BlazemeterApi api,String session,StdErrLog jenBuildLog){
        Result result;
        JSONObject jo;
        boolean success;
        try {
            jo=api.getTresholds(session);
            jenBuildLog.info("Treshold object = " + jo.toString());
            success=jo.getJSONObject("result").getJSONObject("data").getBoolean("success");
        } catch (JSONException je) {
            jenBuildLog.warn("No tresholds on server: setting SUCCESS for build ");
            success=true;
        } catch (Exception e) {
            jenBuildLog.warn("No tresholds on server: setting SUCCESS for build ");
            success=true;
        }
        jenBuildLog.info("Validating server tresholds: " + (success ? "PASSED" : "FAILED") + "\n");

        result = success?Result.SUCCESS:Result.FAILURE;
        if(result.equals(Result.FAILURE)){
            return result;
        }
        return result;
    }

    public static String selectUserKeyOnId(BlazeMeterPerformanceBuilderDescriptor descriptor,
                                           String id){
        String userKey=null;
        List<BlazemeterCredential> credentialList=descriptor.getCredentials("Global");
        if(credentialList.size()==1){
            userKey=credentialList.get(0).getApiKey();
        }else{
            for(BlazemeterCredential c:credentialList){
                if(c.getId().equals(id)){
                    userKey=c.getApiKey();
                    break;
                }
            }
        }
        return userKey;
    }

    public static void getJTL(BlazemeterApi api,String session,FilePath filePath,
                              StdErrLog jenBuildLog,
                              StdErrLog bzmBuildLog){
       JSONObject jo=api.retrieveJTLZIP(session);
       String dataUrl=null;
        URL url=null;
        try {
            JSONArray data=jo.getJSONObject("result").getJSONArray("data");
            for(int i=0;i<data.length();i++){
                String title=data.getJSONObject(i).getString("title");
                if(title.equals("Zip")){
                  dataUrl=data.getJSONObject(i).getString("dataUrl");
                    break;
                }
            }
            File jtlZip=new File(filePath.getParent()
                    + "/" + filePath.getName() + "/" + session + ".zip");
            url=new URL(dataUrl+"?api_key="+api.getApiKey());

            FileUtils.copyURLToFile(url, jtlZip);
            jenBuildLog.info("Downloading JTLZIP from "+url+"to "+jtlZip.getCanonicalPath());
            unzip(jtlZip.getAbsolutePath(), jtlZip.getParent(), jenBuildLog);
        } catch (JSONException e) {
            bzmBuildLog.warn("Unable to get  JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get  JTLZIP from "+url+" "+e.getMessage());
        } catch (MalformedURLException e) {
            bzmBuildLog.warn("Unable to get  JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get  JTLZIP from "+url+" "+e.getMessage());
        } catch (IOException e) {
            bzmBuildLog.warn("Unable to get JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get JTLZIP from "+url+" "+e.getMessage());
        }
    }

    public static void unzip(String srcZipFileName,
                             String destDirectoryName, StdErrLog jenBuildLog) {
        try {
            BufferedInputStream bufIS = null;
            // create the destination directory structure (if needed)
            File destDirectory = new File(destDirectoryName);
            destDirectory.mkdirs();

            // open archive for reading
            File file = new File(srcZipFileName);
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);

            //for every zip archive entry do
            Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                if(entry.getName().substring((entry.getName().length()-4)).equals(".jtl")&!entry.isDirectory()){
                    jenBuildLog.info("\tExtracting jtl report: " + entry);

                    //create destination file
                    File destFile = new File(destDirectory, entry.getName());

                    //create parent directories if needed
                    File parentDestFile = destFile.getParentFile();
                    parentDestFile.mkdirs();

                        bufIS = new BufferedInputStream(
                                zipFile.getInputStream(entry));
                        int currentByte;

                        // buffer for writing file
                        byte data[] = new byte[BUFFER_SIZE];

                        // write the current file to disk
                        FileOutputStream fOS = new FileOutputStream(destFile);
                        BufferedOutputStream bufOS = new BufferedOutputStream(fOS, BUFFER_SIZE);

                        while ((currentByte = bufIS.read(data, 0, BUFFER_SIZE)) != -1) {
                            bufOS.write(data, 0, currentByte);
                        }

                        // close BufferedOutputStream
                        bufOS.flush();
                        bufOS.close();

                }

            }
            bufIS.close();
        } catch (Exception e) {
            jenBuildLog.warn("Failed to unzip report: check that it is downloaded");
        }
    }


    public static Result validateLocalTresholds(TestResult testResult,
                                                PerformanceBuilder builder,
                                                StdErrLog jenBuildLog) {
        Result result=null;
        try {
            int responseTimeUnstable = Integer.valueOf(builder.getResponseTimeUnstableThreshold().isEmpty()
                    ? "-1" : builder.getResponseTimeUnstableThreshold());
            int responseTimeFailed = Integer.valueOf(builder.getResponseTimeFailedThreshold().isEmpty()
                    ? "-1" : builder.getResponseTimeFailedThreshold());
            int errorUnstable = Integer.valueOf(builder.getErrorUnstableThreshold().isEmpty()
                    ? "-1" : builder.getErrorUnstableThreshold());
            int errorFailed = Integer.valueOf(builder.getErrorFailedThreshold().isEmpty()
                    ? "-1" : builder.getErrorFailedThreshold());

            if (errorUnstable < 0) {
                jenBuildLog.info("ErrorUnstable percentage validation will be skipped: value was not set in configuration");
            }
            if (errorFailed < 0) {
                jenBuildLog.info("ErrorFailed percentage validation will be skipped: value was not set in configuration");
            }
            if (responseTimeUnstable < 0) {
                jenBuildLog.info("ResponseTimeUnstable validation will be skipped: value was not set in configuration");
            }
            if (responseTimeFailed < 0) {
                jenBuildLog.info("ResponseTimeFailed validation will be skipped: value was not set in configuration");
            }

            if (responseTimeUnstable >= 0 & testResult.getAverage() > responseTimeUnstable) {
                jenBuildLog.info("Validating responseTimeUnstable...\n");
                jenBuildLog.info("Actual average_response_time="+testResult.getAverage()+" is higher than RESPONSE_TIME_UNSTABLE_treshold="+responseTimeUnstable+"\n");
                jenBuildLog.info("Marking build as unstable");
                result = Result.UNSTABLE;
            }

            if (errorUnstable >= 0 & testResult.getErrorPercentage() > errorUnstable) {
                jenBuildLog.info("Validating errorPercentageUnstable...\n");
                jenBuildLog.info("Actual error_percentage="+testResult.getErrorPercentage()+" is higher than ERROR_PERCENTAGE_UNSTABLE_treshold="+errorUnstable+"\n");
                jenBuildLog.info("Marking build as unstable");
                result = Result.UNSTABLE;
            }

            if (responseTimeFailed >= 0 & testResult.getAverage() >= responseTimeFailed) {
                jenBuildLog.info("Validating responseTimeFailed...\n");
                jenBuildLog.info("Actual average_response_time="+testResult.getAverage()+" is higher than RESPONSE_TIME_FAILED treshold="+responseTimeFailed+"\n");
                jenBuildLog.info("Marking build as failed");
                result = Result.FAILURE;
                return result;
            }

            if (errorFailed >= 0 & testResult.getErrorPercentage() >= errorFailed) {
                jenBuildLog.info("Validating errorPercentageFailed...\n");
                jenBuildLog.info("Actual error_percentage="+testResult.getErrorPercentage()+" is higher than ERROR_PERCENTAGE_FAILED treshold="+errorFailed+"\n");
                jenBuildLog.info("Marking build as failed");
                result = Result.FAILURE;
                return result;
            }

        } catch (Exception e) {
            jenBuildLog.info("Error occured while validating local tresholds. Check that test was finished correctly or turn to customer support");
        } finally {
            return result;
        }
}

    public static String getVersion() {
        Properties props = new Properties();
        try {
            props.load(BzmServiceManager.class.getResourceAsStream("version.properties"));
        } catch (IOException ex) {
            props.setProperty("version", "N/A");
        }
        return props.getProperty("version");
    }
}
