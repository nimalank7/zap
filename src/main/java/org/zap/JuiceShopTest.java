package org.zap;

import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class JuiceShopTest {

    private static final String ZAP_ADDRESS = "localhost";
    private static final int ZAP_PORT = 8080;
    private static final String ZAP_API_KEY = null;

    private static final String TARGET = "http://juice-shop:3000";

    public static void main(String[] args) {
        ClientApi api = new ClientApi(ZAP_ADDRESS, ZAP_PORT, ZAP_API_KEY);

        try {
            // Start spidering the target
            System.out.println("Spider : " + TARGET);
            ApiResponse resp = api.spider.scan(TARGET, null, null, null, null);
            String scanid;
            int progress;

            // The scan now returns a scan id to support concurrent scanning
            scanid = ((ApiResponseElement) resp).getValue();

            // Poll the status until it completes
            while (true) {
                Thread.sleep(1000);
                progress =
                        Integer.parseInt(
                                ((ApiResponseElement) api.spider.status(scanid)).getValue());
                System.out.println("Spider progress : " + progress + "%");
                if (progress >= 100) {
                    break;
                }
            }
            System.out.println("Spider complete");

            // Poll the number of records the passive scanner still has to scan until it completes
            while (true) {
                Thread.sleep(1000);
                progress = Integer.parseInt(((ApiResponseElement) api.pscan.recordsToScan()).getValue());
                System.out.println("Passive Scan progress : " + progress + " records left");
                if (progress < 1) {
                    break;
                }
            }
            System.out.println("Passive Scan complete");

            System.out.println("Active scan : " + TARGET);

            // Disable the DOM XSS scan as it spins up too many headless browsers which leads to the OS OOM killing the container
            api.ascan.disableScanners("40026", "Default Policy");

            // Start the active scan
            resp = api.ascan.scan(TARGET, "True", "False", null, null, null);

            // The scan now returns a scan id to support concurrent scanning
            scanid = ((ApiResponseElement) resp).getValue();

            // Poll the status until it completes
            while (true) {
                Thread.sleep(5000);
                progress = Integer.parseInt(((ApiResponseElement) api.ascan.status(scanid)).getValue());
                System.out.println("Active Scan progress : " + progress + "%");
                if (progress >= 100) {
                    break;
                }
            }
            System.out.println("Active Scan complete");

            System.out.println("Writing alerts to file:");
            byte[] alerts = api.core.htmlreport();
            File file = new File("target/zap-reports/alerts.html");
            file.getParentFile().mkdirs();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(alerts);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
