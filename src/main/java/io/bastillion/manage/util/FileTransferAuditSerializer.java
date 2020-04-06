package io.bastillion.manage.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.bastillion.manage.model.AuditWrapper;
import io.bastillion.manage.model.FileTransferAuditWrapper;

import java.lang.reflect.Type;
import java.util.Date;

public class FileTransferAuditSerializer implements JsonSerializer<Object> {
    @Override
    public JsonElement serialize(Object src, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        if (type.equals(FileTransferAuditWrapper.class)) {
            FileTransferAuditWrapper transferAuditWrapper = (FileTransferAuditWrapper) src;
            object.addProperty("user_id", transferAuditWrapper.getUser().getId());
            object.addProperty("username", transferAuditWrapper.getUser().getUsername());
            object.addProperty("user_type", transferAuditWrapper.getUser().getUserType());
            object.addProperty("first_nm", transferAuditWrapper.getUser().getFirstNm());
            object.addProperty("last_nm", transferAuditWrapper.getUser().getLastNm());
            object.addProperty("email", transferAuditWrapper.getUser().getEmail());
            object.addProperty("session_id", transferAuditWrapper.getSessionID());
            object.addProperty("instance_id", transferAuditWrapper.getHostSystem().getInstanceId());
            object.addProperty("host_id", transferAuditWrapper.getHostSystem().getId());
            object.addProperty("host", transferAuditWrapper.getHostSystem().getDisplayLabel());
            object.addProperty("transfer_type", transferAuditWrapper.getType());
            object.addProperty("source_path", transferAuditWrapper.getSourcePath());
            object.addProperty("dest_path", transferAuditWrapper.getDestPath());
            object.addProperty("timestamp", new Date().getTime());
        }
        return object;
    }

}
