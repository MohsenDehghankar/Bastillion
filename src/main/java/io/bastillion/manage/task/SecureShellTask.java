/**
 * Copyright (C) 2013 Loophole, LLC
 * <p>
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the GNU Affero General Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 */
package io.bastillion.manage.task;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import io.bastillion.manage.control.SecureShellKtrl;
import io.bastillion.manage.db.UserDB;
import io.bastillion.manage.model.*;
import io.bastillion.manage.util.CommandLine;
import io.bastillion.manage.util.SessionOutputUtil;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Task to watch for output read from the ssh session stream
 */
public class SecureShellTask implements Runnable {

    private static Logger log = LoggerFactory.getLogger(SecureShellTask.class);

    InputStream outFromChannel;
    SessionOutput sessionOutput;
    SchSession schSession;

    // all sessions by sessionId
    private static Map<Long, SessionInstances> allSessions = new ConcurrentHashMap<>();

    public SecureShellTask(SessionOutput sessionOutput, InputStream outFromChannel, Long userId, HostSystem host, SchSession schSession) {
        this.sessionOutput = sessionOutput;
        this.outFromChannel = outFromChannel;
        addThisInstance(sessionOutput.getSessionId(), sessionOutput.getInstanceId(), userId, host);
        this.schSession = schSession;
    }

    public synchronized void addThisInstance(Long sessionId, Integer instanceId, Long userId, HostSystem host) {
        synchronized (SecureShellKtrl.class) {
            SessionInstances sessionInstances = allSessions.get(sessionId);
            if (sessionInstances == null) {
                sessionInstances = new SessionInstances();
            }
            Map<Integer, CommandLine> commandLineMap = sessionInstances.getInstancesCommandLine();
            User user = UserDB.getUser(userId);
            commandLineMap.put(instanceId, new CommandLine(instanceId, sessionId, user, host));
            sessionInstances.setInstancesCommandLine(commandLineMap);
            allSessions.put(sessionId, sessionInstances);
        }
    }

    public synchronized void removeThisInstance() {
        synchronized (SecureShellKtrl.class) {
            SessionInstances sessionInstances = allSessions.get(sessionOutput.getSessionId());
            if (sessionInstances != null) {
                Map<Integer, CommandLine> instancesCommandLine = sessionInstances.getInstancesCommandLine();
                instancesCommandLine.remove(sessionOutput.getInstanceId());
                sessionInstances.setInstancesCommandLine(instancesCommandLine);
                allSessions.put(sessionOutput.getSessionId(), sessionInstances);
            }
        }
    }

    public void run() {
        InputStreamReader isr = new InputStreamReader(outFromChannel);
        BufferedReader br = new BufferedReader(isr);
        try {

            SessionOutputUtil.addOutput(sessionOutput);

            char[] buff = new char[1024];
            int read;
            while ((read = br.read(buff)) != -1) {
                if (!appendCommand(buff, 0, read)) {
                    SessionOutputUtil.addToOutput(sessionOutput.getSessionId(), sessionOutput.getInstanceId(), buff, 0, read);
                }

                if (schSession != null && schSession.getInterrupt()) {
                    SessionOutputUtil.addToOutput(sessionOutput.getSessionId(), sessionOutput.getInstanceId(), "\n \033[31m Interrupting.... \033[0m");
                    schSession.getChannel().sendSignal("2");
                    while ((read = br.read(buff)) != -1) {

                        StringBuilder a = new StringBuilder("");
                        a.append(buff, 0, read);
                        if (a.toString().contains("interrupted")) {
                            SessionOutputUtil.addToOutput(sessionOutput.getSessionId(), sessionOutput.getInstanceId(), "\n \033[32m Interrupted [Press Enter] \033[0m");
                            break;
                        }
                    }
                    schSession.setInterrupt(false);
                }

                Thread.sleep(50);
            }

            // the terminal closed
            // flush
            new Thread(() -> appendCommand(new char[]{13}, 0, 1)).start();

            removeThisInstance();

            SessionOutputUtil.removeOutput(sessionOutput.getSessionId(), sessionOutput.getInstanceId());

        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }
    }

    private boolean appendCommand(char[] buffer, int offset, int count) {
        synchronized (SecureShellTask.class) {
            SessionInstances sessionInstances = allSessions.get(sessionOutput.getSessionId());
            Map<Integer, CommandLine> instancesCommandLine = sessionInstances.getInstancesCommandLine();
            CommandLine commandLine = instancesCommandLine.get(sessionOutput.getInstanceId());
            return commandLine.append(buffer, offset, count);
        }
    }

}
