package io.bastillion.manage.util;

import io.bastillion.common.util.AppConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class RemoveDownloadedFilesUtil {
    private Timer timer;
    private static Integer minute = Integer.valueOf(AppConfig.getProperty("removeDownloadInterval"));
    private static Logger log = LoggerFactory.getLogger(RemoveDownloadedFilesUtil.class);

    private RemoveDownloadedFilesUtil() {
        //set interval
        //convert to day
        minute *= 1440;
        timer = new Timer();
        timer.schedule(new RemoveDownloadTimerTask(), minute * 60 * 1000);
    }

    public static void startRemoveDownloadsTask() {
        // validate minute in config file
        if (minute > 0) {
            new RemoveDownloadedFilesUtil();
        }
        //new Remove..
    }

    private class RemoveDownloadTimerTask extends TimerTask {

        @Override
        public void run() {
            //remove downloads
            removeDownloads();
            timer.cancel();

            //create new timer and set interval
            timer = new Timer();
            timer.schedule(new RemoveDownloadTimerTask(), minute * 60 * 1000);
        }
    }

    public static void removeDownloads() {
        try {
            FileUtils.cleanDirectory(new File("./src/main/webapp/download/"));
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

}
