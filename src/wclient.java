/*
    WUMP (specifically BUMP) in java. starter file
 */
import java.lang.*;     //pld
import java.net.*;      //pld
import java.io.*;
import java.util.ArrayList;

//import wumppkt;         // be sure wumppkt.java is in your current directory
//import java.io.Externalizable;

// As is, this packet should receive data[1] and time out.
// If you send the ACK to the correct port, you should receive data[2]
// If you update expected_block, you should receive the entire file, for "vanilla"
// If you write the sanity checks, you should receive the entire file in all cases


//fixing exception
//implement hump
//implement window size



//wump stuff availble
//port 417
//lines 102-114
//play around with sliding windows
//extra credit options
//w

public class wclient {

    //============================================================
    //============================================================

    static public void main(String args[]) {
        int srcport;
        int destport = wumppkt.SERVERPORT; //defined in wumppkt; server is in 417; sends us with an ACK with a port number to accept
        //from. 417 sends block to us, which tells the port where the handoff port. We can the rest of the transmission that what.
        // 417 has no hand off port. Sends back the next block
        // switch
        //destport = wumppkt.SAMEPORT;		// 4716; server responds from same port
        String filename = "vanilla"; //vanilla is a type of request for the server. each one results in a different problem. normal transfer; \\\
        // filename = "lose"; //Lose everything after the first windowful (min 3). It will be retransmitted when you retransmit the previous ACK.
        // filename = "spray"; //Constant barrage of data[1]. Implies LOSE too. In this case, no timeout events will occur; you must check for elapsed time.
        // filename = "delay"; //Delays sending packet 1, prompting a duplicate REQ and thus results in multiple server instances on multiple ports.
        // filename = "reorder";//  Sends the first windowful in the wrong order.
        //4714
        //dupdata2 DATA[2]
        //lose
        //spray
        //delay
        //


        String desthost = "ulam.cs.luc.edu";


        int winsize = 7; //undergrad
        int latchport = 0; //for hump  maybe
        short THEPROTO = wumppkt.BUMPPROTO; // were usping bumpproto. NO INTERGERS. enum looking things only
        THEPROTO = wumppkt.HUMPPROTO;


        wumppkt.setproto(THEPROTO);

        if (args.length > 0) filename = args[0];
        if (args.length > 1) winsize = Integer.parseInt(args[1]);
        if (args.length > 2) desthost = args[2];

        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
        } catch (SocketException se) {
            System.err.println("no socket available");
            return;
        }

        try {
            socket.setSoTimeout(wumppkt.INITTIMEOUT);       // 3000 milliseconds
        } catch (SocketException se) {
            System.err.println("socket exception: timeout not set!");
        }

        if (args.length > 3) {
            System.err.println("usage: wclient filename  [winsize [hostname]]");
            //exit(1);
        }

        // DNS lookup
        InetAddress dest;
        System.err.print("Looking up address of " + desthost + "...");
        try {
            dest = InetAddress.getByName(desthost);
        } catch (UnknownHostException uhe) {
            System.err.println("unknown host: " + desthost);
            return;
        }
        System.err.println(" got it!");

        // build REQ & send it
        wumppkt.REQ req = new wumppkt.REQ(winsize, filename); // ctor for REQ

        System.err.println("req size = " + req.size() + ", filename=" + req.filename());
        DatagramPacket lastsent;
        DatagramPacket reqDG
                = new DatagramPacket(req.write(), req.size(), dest, destport);
        try {
            socket.send(reqDG);
        } catch (IOException ioe) {
            System.err.println("send() failed");
            return;
        }
        lastsent = reqDG;

        //============================================================

        // now receive the response
        DatagramPacket replyDG            // we don't set the address here!
                = new DatagramPacket(new byte[wumppkt.MAXSIZE], wumppkt.MAXSIZE);
        DatagramPacket ackDG = new DatagramPacket(new byte[0], 0);

        ackDG.setAddress(dest);
        ackDG.setPort(destport);        // this is wrong for wumppkt.SERVERPORT version...SERVERPORT will give us new port, which is why we need a new one.

        int expected_block = 1;
        long starttime = System.currentTimeMillis();
        long sendtime = starttime;

        wumppkt.DATA data = null;
        wumppkt.ERROR error = null;
        wumppkt.ACK ack = new wumppkt.ACK(0);

        int proto;        // for proto of incoming packets
        int opcode;
        int length;
        int blocknum;

        //====== HUMP =====================================================

        // if you want to implement HUMP, for better NAT-firewall traversal,
        // this is where that part goes. You also need to set THEPROTO=HUMPPROTO,

        // and use SERVERPORT, not SAMEPORT, above.
        // socket.receive(replyDG), in the usual try-catch
        boolean sent = false;
        while (!sent) {
            try {
                socket.receive(replyDG);
                System.err.println("received packet");
            }catch (SocketTimeoutException ste) {
                /*
                try {
                    socket.send(lastsent);
                } catch (IOException ioe) {
                    System.err.println("send() failed");
                    return;
                }
                /* */
                System.out.println(System.currentTimeMillis()-starttime);
                System.err.println("hard timeout");

            } catch (IOException e)
            {
                e.printStackTrace();
            }
            byte[] replybuf = replyDG.getData();
            // first check proto and opcode then:
            proto = wumppkt.proto(replybuf);
            opcode = wumppkt.opcode(replybuf);
            length = replyDG.getLength();
            srcport = replyDG.getPort();
            if (proto == THEPROTO && opcode == wumppkt.HANDOFFop && srcport == wumppkt.SERVERPORT) {
                wumppkt.HANDOFF handoff = new wumppkt.HANDOFF(replybuf);
                int newport = handoff.newport();
                ack = new wumppkt.ACK(0);        // creates ACK[0] object
                ackDG.setData(ack.write());      // build the packet
                ackDG.setLength(ack.size());
                ackDG.setPort(newport);          // set the port!
                System.err.println("handoff port is "+newport);

                try {
                    socket.send(ackDG);
                    lastsent = ackDG;
                    latchport = newport;
                    sent = true;


                } //catch (SocketTimeoutException ste) {
//                    System.out.println(System.currentTimeMillis());
//                    System.err.println("hard timeout");
//                    // what do you do here??; retransmit of previous packet here
//
//
//                    //we need to check the last
//
//                    ///
//                }
                catch (IOException ioe) {
                    System.err.println("send() failed");
                    return;
                }

            } else if (proto == THEPROTO && opcode == wumppkt.ERRORop) {
                error = new wumppkt.ERROR(replybuf);
            } else {
                System.err.println("received unknown packet while expecting handoff");
            }

        }


        // byte[] replybuf = replyDG.getData(); like a handhalf. we need to send a ACK of that.
        // check that replybuf looks like a HANDOFF. If so:
        // wumppkt.HANDOFF handoff = new wumppkt.HANDOFF(replybuf);
        // int newport = handoff.newport();

        // create ACK[0] and send it. Copy the ACK code towards the end of the main loop. //
        // The port to send it too should be newport, extracted above.

        //====== MAIN LOOP ================================================

        while (true) {
           int W = winsize;
           int E = expected_block;

            if (System.currentTimeMillis() - sendtime >= wumppkt.INITTIMEOUT) {
                sendtime = System.currentTimeMillis();

                try {
                    socket.receive(replyDG);
                } catch (SocketTimeoutException ste) {
                    System.out.println(System.currentTimeMillis()-starttime);
                    System.err.println("hard timeout");

                    try {
                        socket.send(lastsent);
                    } catch (IOException ioe) {
                        System.err.println("send() failed");
                        return;
                    }
                    continue;
                } catch (IOException ioe) {
                    System.err.println("receive() failed");
                    return;
                }

                byte[] replybuf = replyDG.getData();
                proto = wumppkt.proto(replybuf);
                opcode = wumppkt.opcode(replybuf);
                length = replyDG.getLength();

                data = null;
                error = null;
                blocknum = 0;
                if (proto == THEPROTO && opcode == wumppkt.DATAop && length >= wumppkt.DHEADERSIZE) {
                    data = new wumppkt.DATA(replybuf, length);
                    blocknum = data.blocknum();
                } else if (proto == THEPROTO && opcode == wumppkt.ERRORop && length >= wumppkt.EHEADERSIZE) {
                    error = new wumppkt.ERROR(replybuf);
                }
                if (error != null) {
                    System.err.println("Error packet rec'd; code " + error.errcode());
                    continue;
                }
                if (data == null) continue;

                ArrayList<wumppkt.DATA> EarlyArrivals = new ArrayList<wumppkt.DATA>(W);
                for (int i = 0; i < 10; i++) {
                    EarlyArrivals.add(null);
                }

                int M = blocknum;

                if ((M < E) || (M > E + W)){// Out of Bounds
                    System.out.println("recieved Data out of window: " + M);
                    continue;}
                if (M>E){ // Early but acceptable
                    System.out.println("recieved Data, not expected, but in window: " + M);
                    int index = blocknum-E;
                    EarlyArrivals.add(index,data);
                }
                if (M==E){ // Just right
                    System.out.println("recieved expected Data: " + M);
                    System.out.println("latchport: "+latchport);
                    int temp_expectedblock = expected_block;
                    expected_block = printandack(replyDG,data,starttime,latchport,socket,expected_block,dest);
                    if (expected_block < 0)
                    {
                        System.err.println("SW packet error: " + expected_block) ;
                        expected_block = temp_expectedblock;
                        continue;

                    }else {
                        boolean HeadnotNull = true;
                        while (HeadnotNull) {
                            data = EarlyArrivals.get(0);
                            if (data != null) {
                                expected_block = printandack(replyDG,data,starttime,destport,socket,expected_block,dest);

                                for (int i = 1;i<EarlyArrivals.size();i++){ // shift over to left 1
                                    EarlyArrivals.add(i-1,EarlyArrivals.get(i));
                                }
                                EarlyArrivals.add(EarlyArrivals.size(),null);
                            }
                            else {
                                HeadnotNull=false;

                            }
                        }
                    }

                }
                lastsent = ackDG;
                sendtime = System.currentTimeMillis();
                if (length < 512) {
                    break;
                }

            }
        }
    }

    static public int printandack(DatagramPacket replyDG,wumppkt.DATA data,long starttime,int destport,DatagramSocket s, int expected_block, InetAddress dest){
        printInfo(replyDG, data, starttime);
        byte[] replybuf = replyDG.getData();
        int proto = wumppkt.proto(replybuf);
        int opcode = wumppkt.opcode(replybuf);
        int length = replyDG.getLength();
        wumppkt.ERROR error =null;
        if (proto == wumppkt.THEPROTO && opcode == wumppkt.DATAop && length >= wumppkt.DHEADERSIZE) {
            data = new wumppkt.DATA(replybuf, length);

        } else if (proto == wumppkt.THEPROTO && opcode == wumppkt.ERRORop && length >= wumppkt.EHEADERSIZE) {
            error = new wumppkt.ERROR(replybuf);
        }

        if (error != null) {
            System.err.println("Error packet rec'd; code " + error.errcode());
            return -1;
        }
        if (data == null) return -1;        // typical error check, but you should

        System.out.write(data.bytes(), 0, data.size() - wumppkt.DHEADERSIZE);

        DatagramPacket ackDG = new DatagramPacket(new byte[0], 0);
        wumppkt.ACK ack = new wumppkt.ACK(expected_block++);

        ackDG.setData(ack.write());
        ackDG.setLength(ack.size());
        ackDG.setPort(destport);
        ackDG.setAddress(dest);


        try {
            System.err.println("Trying to send ackDG: " + (expected_block-1));
            s.send(ackDG);
            System.out.println("sent ackDG:" + (expected_block-1));
        } catch (IOException ioe) {
            System.err.println("send() failed");
            return -1;
        }catch (java.lang.NullPointerException npe){
            System.err.println("send() failed due to nullpointerException: "+ npe);

            return -1;
        }
        return expected_block;




    }
    // print packet length, protocol, opcode, source address/port, time, blocknum
    static public void printInfo(DatagramPacket pkt, wumppkt.DATA data, long starttime) {
        byte[] replybuf = pkt.getData();
        int proto = wumppkt.proto(replybuf);
        int opcode = wumppkt.opcode(replybuf);
        int length = replybuf.length;
        // the following seven items we can print always
        System.err.print("rec'd packet: len=" + length);
        System.err.print("; proto=" + proto);
        System.err.print("; opcode=" + opcode);
        System.err.print("; src=(" + pkt.getAddress().getHostAddress() + "/" + pkt.getPort()+ ")");
        System.err.print("; time=" + (System.currentTimeMillis()-starttime));
        System.err.println();
        if (data==null)
            System.err.println("         packet does not seem to be a data packet");
        else
            System.err.println("         DATA packet blocknum = " + data.blocknum());
    }

    // extracts blocknum from raw packet
    // blocknum is laid out in big-endian order in b[4]..b[7]
    static public int getblock(byte[] buf) {
        //if (b.length < 8) throw new IOException("buffer too short");
        return  (((buf[4] & 0xff) << 24) |
                ((buf[5] & 0xff) << 16) |
                ((buf[6] & 0xff) <<  8) |
                ((buf[7] & 0xff)      ) );
    }


}
