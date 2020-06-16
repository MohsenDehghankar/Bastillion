package io.bastillion.manage.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcraft.jsch.Channel;
import io.bastillion.common.util.AppConfig;
import io.bastillion.manage.control.SecureShellKtrl;
import io.bastillion.manage.db.UserDB;
import io.bastillion.manage.socket.SecureShellWS;
import io.bastillion.manage.util.DBUtils;
import io.bastillion.manage.util.SessionOutputSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

public class KeyBoardCapture {

    public static final String DIR = DBUtils.class.getClassLoader().getResource(".").getPath() + "../capture";
    public static Logger syslogger = LoggerFactory.getLogger("audit-sysLogger");
    private static Gson gson = new GsonBuilder().registerTypeAdapter(AuditWrapper.class, new SessionOutputSerializer()).create();
    private static Logger log = LoggerFactory.getLogger(KeyBoardCapture.class);

    // prohibited strings
    private static String[] PROHIBITS;

    static {
        PROHIBITS = AppConfig.getProperty("prohibits").split(";");
    }


    String filePath;
    StringBuilder keyBoardInput;
    StringBuilder lastInput;
    int pointer;
    Long sessionId;
    Integer instanceId;
    FileWriter fileWriter;
    Long userId;
    HostSystem hostSystem;
    Channel channel;
    String userType;
    SecureShellWS webSocket;

    static {
        try {
            Files.createDirectory(Paths.get(DIR));
        } catch (FileAlreadyExistsException e) {
            // file exists
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    public KeyBoardCapture(Long sessionId, Integer instanceId, Long userId, HostSystem hostSystem, Channel channel) {
        createRandomName();
        this.keyBoardInput = new StringBuilder("");
        this.channel = channel;
        this.userId = userId;
        this.userType = UserDB.getUser(userId).getUserType();
        this.hostSystem = hostSystem;
        this.instanceId = instanceId;
        this.sessionId = sessionId;
        this.lastInput = new StringBuilder("");
        initializeFile();
    }

    private void initializeFile() {
        try {
            Files.createFile(Paths.get(filePath));
            pointer = Files.readAllBytes(Paths.get(filePath)).length;
            this.fileWriter = new FileWriter(filePath, true);
            fileWriter.write("<-- sessionID = " + sessionId + " -->\n<-- instanceId = " + instanceId + " -->");
            fileWriter.flush();
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    public void createRandomName() {
        this.filePath = DIR + "/" + UUID.randomUUID().toString().replace("-", "");
    }

    public void append(String s) {
        String pre = "";
        String past = "";
        if (pointer == 0) {
            pre = "";
            past = "";
        } else {
            pre = keyBoardInput.substring(0, pointer);
            if (pointer == keyBoardInput.length())
                past = "";
            else
                past = keyBoardInput.substring(pointer);
        }
        keyBoardInput = new StringBuilder(pre + s + past);
        pointer += 1;
        try {
            fileWriter.write("\n" + keyBoardInput.toString());
            fileWriter.flush();
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    public void append(int keyCode) {
        boolean write = false;
        switch (keyCode) {
            //ESC
            case 27:
                reset();
                break;
            //ENTER
            case 13:
                reset();
                break;
            //LEFT
            case 37:
                if (pointer > 0)
                    pointer -= 1;
                break;
            //UP
            case 38:
                reset();
                break;
            //RIGHT
            case 39:
                if (pointer < keyBoardInput.length())
                    pointer += 1;
                break;
            //DOWN
            case 40:
                break;
            //BS
            case 8:
                if (keyBoardInput.length() > 0) {
                    keyBoardInput = new StringBuilder(keyBoardInput.substring(0, keyBoardInput.length() - 1));
                    pointer -= 1;
                }
                write = true;
                break;
            //TAB
            case 9:
                //reset();
                break;
            //CTR
            case 17:
                break;
            //DEL
            case 46:
                break;
            //CTR-A
            case 65:
                reset();
                break;
            //CTR-B
            case 66:
                reset();
                break;
            //CTR-C
            case 67:
                reset();
                break;
            //CTR-D
            case 68:
                reset();
                break;
            //CTR-E
            case 69:
                reset();
                break;
            //CTR-F
            case 70:
                reset();
                break;
            //CTR-G
            case 71:
                reset();
                break;
            //CTR-H
            case 72:
                reset();
                break;
            //CTR-I
            case 73:
                reset();
                break;
            //CTR-J
            case 74:
                reset();
                break;
            //CTR-K
            case 75:
                reset();
                break;
            //CTR-L
            case 76:
                reset();
                break;
            //CTR-M
            case 77:
                reset();
                break;
            //CTR-N
            case 78:
                reset();
                break;
            //CTR-O
            case 79:
                reset();
                break;
            //CTR-P
            case 80:
                reset();
                break;
            //CTR-Q
            case 81:
                reset();
                break;
            //CTR-R
            case 82:
                reset();
                break;
            //CTR-S
            case 83:
                reset();
                break;
            //CTR-T
            case 84:
                reset();
                break;
            //CTR-U
            case 85:
                reset();
                break;
            //CTR-V
            case 86:
                reset();
                break;
            //CTR-W
            case 87:
                reset();
                break;
            //CTR-X
            case 88:
                reset();
                break;
            //CTR-Y
            case 89:
                reset();
                break;
            //CTR-Z
            case 90:
                reset();
                break;
            //CTR-[
            case 219:
                reset();
                break;
            //CTR-]
            case 221:
                reset();
                break;
            //INSERT
            case 45:
                keyBoardInput = new StringBuilder("[INSERT]");
                try {
                    fileWriter.write("\n" + keyBoardInput.toString());
                    fileWriter.flush();
                } catch (IOException e) {
                    log.error(e.toString(), e);
                }
                reset();
                break;
            //PG UP
            case 33:
                reset();
                break;
            //PG DOWN
            case 34:
                reset();
                break;
            //END
            case 35:
                reset();
                break;
            //HOME
            case 36:
                reset();
                break;
        }
        if (write) {
            try {
                fileWriter.write("\n" + keyBoardInput.toString());
                fileWriter.flush();
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }
    }

    private void reset() {
        // check for prohibited regex
        if (userType.equals("A")) {

            if (!isCommandLegal(keyBoardInput.toString(), hostSystem.user, true)) {
                channel.disconnect();
                saveCapture();
                syslogger.error("invalid access to prohibited places" + " sessionId = " + sessionId +
                        " instanceId = " + instanceId);
            }
        }

        keyBoardInput = new StringBuilder("");
        pointer = 0;
    }


    static int counter = 0;

    public static boolean isCommandLegal(String cmd, String systemName, boolean directInput) {

        cmd = cmd.trim();
        String[] cms = cmd.split("(\n|\r)");
        String cm = cms[cms.length - 1];
        cm = cm.replace("\t", "");
        cm = cm.replace(String.valueOf((char) 7), "");
        cmd = cmd.replace(String.valueOf((char) 7), "");

        /*for (char c : cm.toCharArray()) {
            System.out.print(Integer.valueOf(c) + " ");
        }*/

        for (String prohibit : PROHIBITS) {
            if (cmd.contains(prohibit)) {
//                System.out.println("[All Input Contains]" + counter++);
                if (cms.length == 1) {
//                    System.out.println("[Disconnect1]" + counter++);
                    return false;
                } else {
                    if (cm.contains(prohibit)) {
//                        System.out.println("[Disconnect2]" + counter++);
//                        System.out.println(cm);
                        return false;
                    }
                    if (cms[cms.length - 2].contains(prohibit)) {
//                        System.out.println("[Disconnect3]" + counter++);
//                        System.out.println(cms[cms.length - 2]);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void saveCapture() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
        // map file name and instance id and session id (log)
        SessionOutput output = new SessionOutput(sessionId, hostSystem);
        output.setOutput(new StringBuilder("Keyboard captured file path: " + filePath));
        syslogger.info(gson.toJson(new AuditWrapper(UserDB.getUser(userId), output)));
    }
}
