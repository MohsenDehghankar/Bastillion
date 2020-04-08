package io.bastillion.manage.util;


import io.bastillion.manage.control.SecureShellKtrl;
import io.bastillion.manage.db.UserDB;
import io.bastillion.manage.model.*;

import java.util.Map;


/**
 * representing one line of command ( input and output ) in a terminal
 */
public class CommandLine {
    StringBuilder command;
    Long sessionId;
    Integer instanceId;
    User user;
    HostSystem hostSystem;

    public CommandLine(Integer instanceId, Long sessionId, User user, HostSystem hostSystem) {
        command = new StringBuilder("");
        this.instanceId = instanceId;
        this.sessionId = sessionId;
        this.user = user;
        this.hostSystem = hostSystem;
    }

    public void append(char[] value, int offset, int count) {
        String tmp = String.valueOf(value, offset, count);
        command.append(tmp);
        if (value[0] == 13) {
            // enter pressed
            // create log
            sendLog(command);
            // one command finished
            command = new StringBuilder("");
        }
    }


    private void sendLog(StringBuilder log) {
        SessionOutput output = new SessionOutput(sessionId, hostSystem);
        output.setOutput(log);
        SessionOutputUtil.sendLog(user, output);

        if (user.getUserType().equals("M"))
            return;
        // check prohibited strings
        if (!KeyBoardCapture.isCommandLegal(log.toString())) {
            Map<Long, UserSchSessions> userSchSessionMap = SecureShellKtrl.getUserSchSessionMap();
            UserSchSessions userSchSessions = userSchSessionMap.get(sessionId);
            SchSession schSession = userSchSessions.getSchSessionMap().get(instanceId);
            schSession.getChannel().disconnect();
//            System.out.println("disconnected2");
            KeyBoardCapture.syslogger.error("invalid access to prohibited places" + " sessionId = " + sessionId +
                    " instanceId = " + instanceId);
        }
    }
}
