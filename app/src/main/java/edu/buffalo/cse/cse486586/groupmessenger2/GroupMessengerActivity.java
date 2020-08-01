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
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
class compareValues implements Comparator<String>{


    public int compare(String lhs, String rhs) {
        String[] s1 = lhs.split("&&");
        String[] s2 = rhs.split("&&");

        if (Integer.parseInt(s1[4]) < Integer.parseInt(s2[4]))
            return -1;
        else if (Integer.parseInt(s1[4]) > Integer.parseInt(s2[4]))
            return 1;
        else{
            if(Integer.parseInt(s1[0]) < Integer.parseInt(s2[0]))
                return -1;
            else if(Integer.parseInt(s1[0]) > Integer.parseInt(s2[0]))
                return 1;
            else
                return  0;

        }
    }
}


public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10000;
    ArrayList<String> remotePorts = new ArrayList<String>();
    static int count= 0;
    static final String KEY="key";
    static final String VALUE="value";
    static String myPort;
    int msgNum = 0;
    int failure = 0;
    int proposedNum = 0;
    int agreedNum = 0;
    final String  DELIM= "&&";
    PriorityQueue<String> queue = new PriorityQueue<String>(11, new compareValues());


    Uri providerUri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger2.provider").build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        remotePorts.add("11108");
        remotePorts.add("11112");
        remotePorts.add("11116");
        remotePorts.add("11120");
        remotePorts.add("11124");

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final EditText editText1 = (EditText) findViewById(R.id.editText1);
        Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText1.getText().toString();
                editText1.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            Socket socket = null;
            try {
                while (true) {
                    socket = serverSocket.accept();
                    DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    String message = input.readUTF();
                    String[] split = message.split(DELIM);


                        if (split.length == 5) {
                            failure = (Integer.parseInt(split[4]) > 0) ? Integer.parseInt(split[4]) : 0;
                            proposedNum = (proposedNum >= agreedNum) ? proposedNum + 1 : agreedNum + 1;
                            String inMsg = split[1];
                            String proposedMessage = split[0] + DELIM + inMsg + DELIM + split[2] + DELIM + split[3] + DELIM + proposedNum + DELIM + "UNDEL" + DELIM + failure;
                            queue.add(proposedMessage);
                            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                            output.writeUTF(proposedMessage);
                            output.flush();


                            if (failure != 0) {
                                for (String s : queue) {
                                    if (Integer.parseInt(s.split(DELIM)[2]) == failure && s.split(DELIM)[5].equals("UNDEL"))
                                        queue.remove(s);
                                }

                            }

                        }
                        if (split.length == 7) {
                            failure = (Integer.parseInt(split[6]) > 0) ? Integer.parseInt(split[6]) : 0;
                            agreedNum = Integer.parseInt(split[4]);
                            for (String s : queue) {
                                String[] sSplit = s.split(DELIM);
                                if (split[0].equals(sSplit[0])) {
                                    queue.remove(s);
                                    queue.add(message);
                                }
                            }

                        if(failure!=0) {
                            for (String s : queue) {
                                if (Integer.parseInt(s.split(DELIM)[2]) == failure && s.split(DELIM)[5].equals("DEL"))
                                    queue.remove(s);
                            }

                        }
                            while (true) {
                                if (!queue.isEmpty() && queue.peek().split(DELIM)[5].equals("DEL") ) {
                                    if(failure==Integer.parseInt(split[2])){
                                        queue.poll();
                                    }
                                    else{
                                        String msg = queue.poll();
                                        publishProgress(msg);
                                    }

                                } else
                                    break;
                            }
                        }

                    }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


        synchronized protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            String[] strReceivedSplit = strReceived.split(DELIM);
            TextView textView1 = (TextView) findViewById(R.id.textView1);
            textView1.append(strReceivedSplit[1] + "\n");
            // Reference PA2A Documentation
            ContentValues content = new ContentValues();
            content.put(KEY, count);
            content.put(VALUE, strReceivedSplit[1]);
            count++;
            Uri newUri = getContentResolver().insert(providerUri,content);

        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        int currentMax = 0;
        String maxString="";
        @Override
        protected Void doInBackground(String... msgs) {
            msgNum++;
            String localSeq = String.valueOf(msgNum)+myPort;
            for (String remotePort : remotePorts) {
                try {
                    if (Integer.parseInt(remotePort) == failure)
                        continue;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    socket.setSoTimeout(500);
                    String msgToSend = localSeq + DELIM + msgs[0] + DELIM + myPort + DELIM + remotePort + DELIM + failure;
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF(msgToSend);
                    output.flush();
                    try {
                        DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                        String proposal = input.readUTF();
                        String[] proposalSplit = proposal.split(DELIM);
                        String temp = proposalSplit[4] + proposalSplit[3];
                        if (currentMax < Integer.parseInt(temp)) {
                            currentMax = Integer.parseInt(temp);
                            maxString = proposalSplit[4] + DELIM + proposalSplit[3];
                        }
                        Thread.sleep(100);
                        //socket.close();
                    } catch (UnknownHostException e) {
                        failure = Integer.parseInt(remotePort);
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        failure = Integer.parseInt(remotePort);
                        Log.e(TAG, "ClientTask socket IOException");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (UnknownHostException e) {
                    if (failure == 0) {
                        failure = Integer.parseInt(remotePort);
                    }
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    if (failure == 0) {
                        failure = Integer.parseInt(remotePort);
                    }
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }
            String[] maxStringSplit = maxString.split(DELIM);
            for (String remotePort : remotePorts) {
                try {
                    if(Integer.parseInt(remotePort)==failure)
                        continue;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    socket.setSoTimeout(500);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    String agreedMsg = localSeq + DELIM + msgs[0] + DELIM + myPort + DELIM + remotePort + DELIM + maxStringSplit[0] + DELIM +"DEL" + DELIM + failure;
                    output.writeUTF(agreedMsg);
                    output.flush();
                    Thread.sleep(200);
                    //socket.close();
                } catch (UnknownHostException e) {
                    if(failure==0) {
                        failure = Integer.parseInt(remotePort);
                    }
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    if(failure==0) {
                        failure = Integer.parseInt(remotePort);
                    }
                    Log.e(TAG, "ClientTask socket IOException");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;

        }

    }
}