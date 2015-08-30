package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static int messageCount = 0;
    static int globally_agreed_seq_number = 0;
    static int proposed_seq_number = 0;
    static String clientPort;
    static int noOfClients = 5;
    static double MaxProposed = 0.0;
    static int msgID;
    static int contentProviderCount = 0;
    static TreeMap<Integer,Message> msg_mapping_queue;
    static TreeMap<Double,Message> hold_back_queue;
    static HashMap<Integer,ArrayList<Double>> msg_proposal_count;
    static int socketTimeout =500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        // Reference : Below piece of code is taken from PA1
        msg_mapping_queue = new TreeMap<Integer,Message>();
        hold_back_queue  = new TreeMap<Double,Message>();
        msg_proposal_count = new HashMap<>();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        clientPort = myPort;
        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                remoteTextView.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                //return true;
            }
        });
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            /*Reference: Logic for following code has been taken from below URL
            http://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
             */
            try {
                while(true) {
                    Socket acceptSocket = serverSocket.accept();
                    ObjectInputStream ois = new ObjectInputStream(acceptSocket.getInputStream());
                    Message receivedMessage = (Message)ois.readObject();

                    if( receivedMessage.msgType == 0 )
                    {
                        proposed_seq_number = Math.max(globally_agreed_seq_number,proposed_seq_number) +1;
                        String temp = Integer.toString(proposed_seq_number) + "." + clientPort;
                        double proposed_seq_number_with_clientID = Double.parseDouble(temp);

                        Message proposalMessage = new Message(Integer.parseInt(clientPort),receivedMessage.msgId,"###",proposed_seq_number_with_clientID,1,false);
                        msg_mapping_queue.put(receivedMessage.msgId,receivedMessage);
                        hold_back_queue.put(proposed_seq_number_with_clientID,receivedMessage);
                        try
                        {
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                      receivedMessage.clientPort);

                                 /*Reference: Logic for following code has been taken from below URL
                                  http://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                                 */
                                socket.setSoTimeout(socketTimeout);
                                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                oos.writeObject(proposalMessage);
                                oos.close();
                                socket.close();

                         }catch (UnknownHostException e) {
                                Log.e(TAG, "ServerTask UnknownHostException");
                         }catch (SocketTimeoutException e){
                                Log.e(TAG,"ServerTask Socket TimeOutException");
                         }catch (IOException e) {
                                Log.e(TAG, "ServerTask socket IOException");
                         }


                    }
                    if( receivedMessage.msgType == 1)
                    {
                        if ( msg_proposal_count.containsKey(receivedMessage.msgId))
                        {
                             msg_proposal_count.get(receivedMessage.msgId).add(receivedMessage.global_seq_number);
                        }
                        else
                        {
                            ArrayList<Double> proposalList = new ArrayList<>();
                            proposalList.add(receivedMessage.global_seq_number);
                            msg_proposal_count.put(msgID,proposalList);
                        }
                        if (msg_proposal_count.get(receivedMessage.msgId).size() == noOfClients )
                        {
                            ArrayList<Double> finalProposalList = msg_proposal_count.get(receivedMessage.msgId);
                            Collections.sort(finalProposalList);
                            MaxProposed = finalProposalList.get(finalProposalList.size()-1);
                            Message agreedMessage = new Message(Integer.parseInt(clientPort),receivedMessage.msgId,"####",MaxProposed,2,false);
                            for (int i=0 ; i<5 ; i++ )
                            {
                                try {

                                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                            Integer.parseInt(REMOTE_PORTS[i]));
                                    socket.setSoTimeout(socketTimeout);
                                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                    oos.writeObject(agreedMessage);
                                    oos.close();
                                    socket.close();

                                } catch (UnknownHostException e) {
                                    Log.e(TAG, "ClientTask UnknownHostException");
                                } catch (SocketTimeoutException e){
                                    Log.e(TAG,"ServerTask Socket TimeOutException");
                                } catch (IOException e) {
                                    Log.e(TAG, "ClientTask socket IOException");
                                }

                            }


                        }

                    }
                    if(receivedMessage.msgType == 2)
                    {
                        globally_agreed_seq_number = Math.max(globally_agreed_seq_number,(int)receivedMessage.global_seq_number);
                        Set<Map.Entry<Double,Message>> entryset = hold_back_queue.entrySet();
                        for(Map.Entry entry : entryset)
                        {
                            Message m = (Message)entry.getValue();
                            if ( m == msg_mapping_queue.get(receivedMessage.msgId) )
                            {
                                Double d = (Double)entry.getKey();
                                hold_back_queue.remove(d);
                                m.isDeliverable = true;
                                hold_back_queue.put(receivedMessage.global_seq_number,m);
                                break;
                            }
                        }
                        while(hold_back_queue.size()>0 && hold_back_queue.firstEntry().getValue().isDeliverable )
                        {
                            String receivedmsg = hold_back_queue.firstEntry().getValue().msg;
                            publishProgress(receivedmsg);
                            ContentValues contentValue = new ContentValues();
                            contentValue.put("key",contentProviderCount);
                            contentValue.put("value",receivedmsg);
                            Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                            Uri newUri = getContentResolver().insert(providerUri,contentValue);
                            hold_back_queue.remove(hold_back_queue.firstEntry().getKey());
                            contentProviderCount++;
                        }
                    }
                }
            } catch (IOException e) {
               Log.v("message",e.getMessage());
            } catch(ClassNotFoundException e){
                Log.v("message",e.getMessage());
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            //Reference : Following piece of code is taken from PA1
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "GroupMessengerOutPut";
            String string = strReceived + "\n";
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0];
            Random rand = new Random();
            msgID = rand.nextInt(10000) + 1;

            Message m = new Message(Integer.parseInt(clientPort),msgID,msgToSend,0,0,false);
            for (int i=0 ; i<5 ; i++ )
            {
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));
                    socket.setSoTimeout(socketTimeout);

                /*Reference: Logic for following code has been taken from below URL
                 http://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                */

                   ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                   oos.writeObject(m);
                   oos.close();
                   socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                }catch (SocketTimeoutException e){
                    Log.e(TAG,"Client Task Socket Timeout Exception");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }
            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
