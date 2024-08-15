package part1.common.Message;

import lombok.AllArgsConstructor;

/**
 * 消息类型
 */
@AllArgsConstructor
public enum MessageType {
    REQUEST(0),RESPONSE(1);
    private int code;
    public int getCode(){
        return code;
    }
}