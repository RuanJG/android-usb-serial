/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE GIMBAL_HOME_OFFSET_CALIBRATION_RESULT PACKING
package com.MAVLink.ardupilotmega;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;
        
/**
* 
            Sent by the gimbal after it receives a SET_HOME_OFFSETS message to indicate the result of the home offset calibration
        
*/
public class msg_gimbal_home_offset_calibration_result extends MAVLinkMessage{

    public static final int MAVLINK_MSG_ID_GIMBAL_HOME_OFFSET_CALIBRATION_RESULT = 205;
    public static final int MAVLINK_MSG_LENGTH = 1;
    private static final long serialVersionUID = MAVLINK_MSG_ID_GIMBAL_HOME_OFFSET_CALIBRATION_RESULT;


      
    /**
    * The result of the home offset calibration
    */
    public short calibration_result;
    

    /**
    * Generates the payload for a mavlink message for a message of this type
    * @return
    */
    public MAVLinkPacket pack(){
        MAVLinkPacket packet = new MAVLinkPacket();
        packet.len = MAVLINK_MSG_LENGTH;
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_GIMBAL_HOME_OFFSET_CALIBRATION_RESULT;
              
        packet.payload.putUnsignedByte(calibration_result);
        
        return packet;
    }

    /**
    * Decode a gimbal_home_offset_calibration_result message into this class fields
    *
    * @param payload The message to decode
    */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
              
        this.calibration_result = payload.getUnsignedByte();
        
    }

    /**
    * Constructor for a new message, just initializes the msgid
    */
    public msg_gimbal_home_offset_calibration_result(){
        msgid = MAVLINK_MSG_ID_GIMBAL_HOME_OFFSET_CALIBRATION_RESULT;
    }

    /**
    * Constructor for a new message, initializes the message with the payload
    * from a mavlink packet
    *
    */
    public msg_gimbal_home_offset_calibration_result(MAVLinkPacket mavLinkPacket){
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_GIMBAL_HOME_OFFSET_CALIBRATION_RESULT;
        unpack(mavLinkPacket.payload);        
    }

      
    /**
    * Returns a string with the MSG name and data
    */
    public String toString(){
        return "MAVLINK_MSG_ID_GIMBAL_HOME_OFFSET_CALIBRATION_RESULT -"+" calibration_result:"+calibration_result+"";
    }
}
        