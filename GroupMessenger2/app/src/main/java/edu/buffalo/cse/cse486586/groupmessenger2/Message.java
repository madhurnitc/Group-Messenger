package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by maddy on 3/8/15.
 */
public class Message implements Serializable{

    public int clientPort;
    public int msgId;
    public String msg;
    public double global_seq_number;
    public int msgType;
    public boolean isDeliverable;



public Message(int client,int id,String msg,double global_numb,int type,boolean isdeliverable)
{
    this.clientPort = client;
    this.msgId = id;
    this.msg = msg;
    this.global_seq_number = global_numb;
    this.msgType = type;
    this.isDeliverable = isdeliverable;
}

}
