/*
    WUMP (specifically BUMP) in java. starter file
 */
import java.lang.*;     //pld
import java.net.*;      //pld
import java.io.*;
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
        destport = wumppkt.SAMEPORT;		// 4716; server responds from same port
        String filename = "vanilla"; //vanilla is a type of request for the server. each one results in a different problem. normal transfer; \\\
        filename = "lose"; //Lose everything after the first windowful (min 3). It will be retransmitted when you retransmit the previous ACK.
        //4714
        //dupdata2 DATA[2]
        //lose
        //spray
        //delay
        //




        String desthost = "ulam.cs.luc.edu";



        int winsize = 1; //undergrad
        int latchport = 0; //for hump  maybe
        short THEPROTO = wumppkt.BUMPPROTO; // were usping bumpproto. NO INTERGERS. enum looking things only



        wumppkt.setproto(THEPROTO);

        if (args.length > 0) filename = args[0];
        if (args.length > 1) winsize = Integer.parseInt(args[1]);
        if (args.length > 2) desthost = args[2];

        DatagramSocket s;
        try {
            s = new DatagramSocket();
        }
        catch (SocketException se) {
            System.err.println("no socket available");
            return;
        }

        try {
            s.setSoTimeout(wumppkt.INITTIMEOUT);       // 3000 milliseconds
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
        }
        catch (UnknownHostException uhe) {
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
        try {s.send(reqDG);}
        catch (IOException ioe) {
            System.err.println("send() failed");
            return;
        }
        lastsent = reqDG;

        //============================================================

        // now receive the response
        DatagramPacket replyDG            // we don't set the address here!
                = new DatagramPacket(new byte[wumppkt.MAXSIZE] , wumppkt.MAXSIZE);
        DatagramPacket ackDG = new DatagramPacket(new byte[0], 0);

        ackDG.setAddress(dest);
        ackDG.setPort(destport);		// this is wrong for wumppkt.SERVERPORT version...SERVERPORT will give us new port, which is why we need a new one.

        int expected_block = 1;
        long starttime = System.currentTimeMillis();
        long sendtime = starttime;

        wumppkt.DATA  data  = null;
        wumppkt.ERROR error = null;
        wumppkt.ACK   ack   = new wumppkt.ACK(0);

        int proto;        // for proto of incoming packets
        int opcode;
        int length;
        int blocknum;

        //====== HUMP =====================================================

        // if you want to implement HUMP, for better NAT-firewall traversal,
        // this is where that part goes. You also need to set THEPROTO=HUMPPROTO,

        // and use SERVERPORT, not SAMEPORT, above.
        // s.receive(replyDG), in the usual try-catch
        // byte[] replybuf = replyDG.getData(); like a handhalf. we need to send a ACK of that.
        // check that replybuf looks like a HANDOFF. If so:
        // wumppkt.HANDOFF handoff = new wumppkt.HANDOFF(replybuf);
        // int newport = handoff.newport();
        // create ACK[0] and send it. Copy the ACK code towards the end of the main loop. //
        // The port to send it too should be newport, extracted above.

        //====== MAIN LOOP ================================================

        while (true) {






            // get packet
            try {
                s.receive(replyDG);
            }
            catch (SocketTimeoutException ste) {
                System.out.println(System.currentTimeMillis());
                System.err.println("hard timeout");
                // what do you do here??; retransmit of previous packet here
                try {s.send(lastsent);}
                catch (IOException ioe) {
                    System.err.println("send() failed");
                    return;
                }


                continue; //right now creates a hard timeout

                //we need to check the last

                ///
            }
            catch (IOException ioe) {
                System.err.println("receive() failed");
                return;
            }

            byte[] replybuf = replyDG.getData();
            proto   = wumppkt.proto(replybuf);
            opcode  = wumppkt.opcode(replybuf);
            length  = replyDG.getLength();
            srcport = replyDG.getPort();

            /* The new packet might not actually be a DATA packet.
             * But we can still build one and see, provided:
             *   1. proto =   THEPROTO
             *   2. opcode =  wumppkt.DATAop
             *   3. length >= wumppkt.DHEADERSIZE
             */

            data = null; error = null;
            blocknum = 0;
            if (  proto == THEPROTO && opcode == wumppkt.DATAop && length >= wumppkt.DHEADERSIZE) {
                data = new wumppkt.DATA(replybuf, length);
                blocknum = data.blocknum();
            } else if ( proto == THEPROTO && opcode == wumppkt.ERRORop && length >= wumppkt.EHEADERSIZE) {
                error = new wumppkt.ERROR(replybuf);
            }

            printInfo(replyDG, data, starttime);

            // now check the packet for appropriateness
            // if it passes all the checks:
            //write data, increment expected_block
            // exit if data size is < 512



            if (error != null) {
                System.err.println("Error packet rec'd; code " + error.errcode());
                continue;
            }
            if (data == null) continue;		// typical error check, but you should

            // The following is for you to do:
            // check port, packet size, type, block, etc
            // latch on to port, if block == 1

            // write data


            System.out.write(data.bytes(), 0, data.size() - wumppkt.DHEADERSIZE);

            // send ack
            ack = new wumppkt.ACK(expected_block++);
            ackDG.setData(ack.write());
            ackDG.setLength(ack.size());
            ackDG.setPort(destport);
            try {s.send(ackDG);}
            catch (IOException ioe) {
                System.err.println("send() failed");
                return;
            }
            lastsent = ackDG;
            sendtime = System.currentTimeMillis();
            if (length< 512){
//                System.out.println(" ");
//                System.out.println("We're breaking because the file is finished!");
                break;
            }

        } // while
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
