import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class Transmitter {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
        long t1 = 0, t2 =0;            // long variables to store millisecond values calculated by currentTImeMillis method         
        long [] rtt = new long[10];    // rtt[] array to store round trip time values
		byte[] INIT_BUFFER= new byte[9];  // array to store INIT packet values in bytes
		byte[] IACK_BUFFER= new byte[5];  // array to store IACK packet values in bytes
		byte[] DATA_BUFFER= new byte[305]; // array to store DATA packet values in bytes
		byte[] DACK_BUFFER= new byte[5]; // array to store DACK packet values in bytes
		byte[] PAYLOAD= new byte[300];  // array to store actual payload values in bytes
		byte[] seq_no= new byte[2];  // array to store sequence number in bytes
		
		
		INIT_BUFFER= Init_Packet(INIT_BUFFER);  // calling the Init_Packet method to create INIT_BUFFER array 
		InetAddress address= InetAddress.getLocalHost(); // IP address of the receiver
		DatagramPacket INIT_packet= new DatagramPacket(INIT_BUFFER, INIT_BUFFER.length, address, 9999); // creating the INIT datagram packet to be sent
		DatagramSocket socket= new DatagramSocket(); 
	
		DatagramPacket IACK_packet= new DatagramPacket(IACK_BUFFER, IACK_BUFFER.length); // creating the INIT datagram packet to be received
		int l=0; // counter for number of transmissions
		int m=1; // counter to double timeout value
        
		System.out.println("Transmitter ready to communicate with receiver\n");
		System.out.println("Sending INIT packet...."); 
		System.out.print("INIT packet: ");
		for(int i1=0; i1<INIT_BUFFER.length; i1++) {
			System.out.print(" " + String.format("%02x", INIT_BUFFER[i1]));
		} // Printing the INIT packet
		System.out.println("\n");
		
		while(l<4) { // while loop runs till number of transmissions is less than 4
		try {
		socket.send(INIT_packet); // send INIT_packet-one transmission
		socket.setSoTimeout(m*1000); //set initial timeout value as 1000 ms
		l++; // increment counter after every transmission
		m=2*m; // double j to double timeout value for every transmission
		socket.receive(IACK_packet); // receive incoming IACK packet from receiver
		if(IntegrityCheckProcess(IACK_BUFFER)) {

			if(IACK_BUFFER[0]==(byte) 0xaa) {
				
				int a= INIT_BUFFER[2]; //storing the second byte in the INIT sequence no as a decimal value to increment by 1
				if(IACK_BUFFER[1]==INIT_BUFFER[1] && IACK_BUFFER[2]==(byte)(a+1)) { // If Integrity check method returns true, IACK packet type is correct and IACK sequence number is one greater than the INIT packet sequence no, 

					seq_no[0]=IACK_BUFFER[1];
					seq_no[1]=IACK_BUFFER[2]; // store the incoming IACK packet's sequence number for future reference in the first data packet 
					
					System.out.println("IACK packet received.");
					System.out.print("IACK packet: ");
					for(int k=0; k< IACK_BUFFER.length; k++) {
						System.out.print(" " + String.format("%02x", IACK_BUFFER[k]));
					}// print the received IACK packet if the required conditions are met
					System.out.println("\nTwo way handshake complete.");
				
					l=0;
					m=0;
					break;// break out of the loop if correct IACK packet is received
				  }
				
				
				}
			}}
		catch(Exception e) {
			System.out.println("Handshake no. "+ l +" failed!!!");
			System.out.println("Timeout: "+(m*1000)/2+" milliseconds\n\n"); // if timeout occurs before IACK packet is received and number of transmissions is not yet 4, print the transmission number and the timeout value
			if(l==4) {	System.out.println("Communication failure!!!!!"); // if number of transmissions reaches 4, show communication failure, close transmitter socket and exit
			    socket.close(); 
				System.exit(0); 
				}
		} 
		
		}
		
		System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
		System.out.println("Starting data transfer.....\n\n"); // data transfer process starts
		for(int k=0; k<10; k++) { // for loop to send ten data packets one by one 
			int a=0; //counter for number of transmissions for each data packet
			int b=1; // counter to double timeout value after every transmission for each data packet
			DATA_BUFFER[0]= (byte) 0x33; // packet type for data packets 
				
			int seq= (seq_no[0] << 8)+ (seq_no[1] & 0xFF); // sequence number stored as decimal by concatenating the two bytes in the seq_no array initialized from the received IACK packet
			int seq1= seq+ 300*k; // variable seq1 stores the sequence number for each data packet and is incremented by 300 for each consequent data packet
			if(seq1>65535) { // if the sequence number is greater than 65535 which is the upper limit for a 16 bit number, it will wrap around and become 0 and subsequent incrementing will continue from that number
	    		seq1=seq1-65536;
	    	  }
	    	
			DATA_BUFFER[1]= (byte)(seq1>>8);  // masking lower eight bits and storing higher 8 bits of byte casted seq1 in DATA_BUFFER
			DATA_BUFFER[2]= (byte)(seq1 & 0xFF); // store lower eight bits for byte casted seq1 in the DATA_BUFFER
			Random r= new Random(); 
			r.nextBytes(PAYLOAD); // store random 300 bytes into the PAYLOAD array
			for(int k1=3; k1<303; k1++) {
				DATA_BUFFER[k1]= PAYLOAD[k1-3]; 
			} // storing the PAYLOAD bytes into the proper positions in the DATA_BUFFER arrays
			byte[] c= new byte[2];
			c= DataIntegrityCheckField(DATA_BUFFER);// calling the DataIntegrityCheckField method to calculate the Integrity check value
			DATA_BUFFER[303]=c[0];
			DATA_BUFFER[304]=c[1];
			DatagramPacket DATA_packet= new DatagramPacket(DATA_BUFFER, DATA_BUFFER.length, address, 9999); // creating the data packet to be sent to the receiver

			DatagramPacket DACK_packet= new DatagramPacket(DACK_BUFFER, DACK_BUFFER.length); // creating the dack packet to be received from the receiver
			
            System.out.print((k+1)+") ");
			System.out.print("DATA packet: \n");
			for(int i1=3; i1< 303; i1++) {
				System.out.print(" " + String.format("%02x", DATA_BUFFER[i1]));
			} // printing the payload for each packet in hex format
			while(a<4) { // while loop to transmit each packet while number of transmissions is not more than 4
			
				try {
				System.out.println("\nSending data packet.....");
				t1=System.currentTimeMillis();// method to record exact time of sending a data packet 
				socket.send(DATA_packet);
				
				socket.setSoTimeout(b*1000);

				a++;
				b=2*b; // set initial timeout value as 1000ms and double for every subsequent transmission
				socket.receive(DACK_packet);
				
				int n= (DACK_BUFFER[1] << 8)+ (DACK_BUFFER[2] & 0xFF); // concatenating and storing the sequence number bytes in received DACK_BUFFER as a decimal value

				if(IntegrityCheckProcess(DACK_BUFFER)) {
					if(DACK_BUFFER[0]==(byte)0xCC){

					if(n==(seq1+300)) {
						System.out.println("DACK packet received");
						
						System.out.print("DACK packet: ");
						for(int a1=0; a1< DACK_BUFFER.length; a1++) {
							System.out.print(" " + String.format("%02x", DACK_BUFFER[a1]));
						
						} // if required conditions are met, print the received DACK packet
						t2=System.currentTimeMillis(); // record exact time of receipt of DACK packet for each sent DATA packet 
						System.out.println();
						System.out.println("Round Trip Time: " +RTT(t1,t2)+" ms"); // call the RTT method to calculate the round trip time between sending a DATA packet and successfully receiving the corresponding acknowledgement 
						rtt[k] = RTT(t1,t2); // rtt[] array stores the round trip time value for each data packet
						System.out.println();
						break; // break out when acknowledgement received to send the next DATA packet
					}
					
					
				} 
				
			} }
				catch(Exception e) { System.out.println(e.getMessage());
					System.out.println("Data Packet no. "+ (k+1) +" failed!!!");
					System.out.println("Timeout: "+(b*1000)/2+" milliseconds\n\n"); // print timeout values for every timeout when number of transmissions is not 4
					if(a==4) {	System.out.println("Communication failure!!!!!"); 
					    socket.close();
						System.exit(0); // close socket and exit when number of transmissions becomes 4
						}
				} 

		}
		
		}
		
	    System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
	    System.out.println("Data transfer statistics:\n");
		System.out.println("Average Round Trip Time: " + averageRTT(rtt)+" ms"); 
		System.out.println("Minimum Round Trip Time: " + minRTT(rtt)+" ms");
		System.out.print("Maximum Round Trip Time: " +maxRTT(rtt)+" ms");// calling the appropriate methods to calculate RTT statistics
		
		
		} // main ends
	



	private static long maxRTT(long[] rtt) {
		// TODO Auto-generated method stub
		long max=0;
		for(int i=0; i<10; i++) {
			max=Math.max(max, rtt[i]);
		}
		return max;
	} //method to return maximum calculated RTT




	private static long minRTT(long rtt[]) {
		// TODO Auto-generated method stub
		long min=100000;
		for (int i = 0; i< 10; i++) {
			min=Math.min(min,rtt[i]);
		}
		return min;
	} // method to calculate minimum calculated RTT




	private static double averageRTT(long rtt[]) {
		double avg = 0, sum =0;
		for(int i=0; i < 10; i++) {
    	   sum=(rtt[i]+sum);
		}
        avg=sum/10;
		return avg;
	} // method to calculate average calculated RTT




	private static byte[] DataIntegrityCheckField(byte[] DATA_BUFFER) {
		// TODO Auto-generated method stub
		byte[] array= new byte[2]; // introduce a new 2 byte array variable to XOR with DATA_BUFFER[] values
		array[0]= (byte)0x0;
		array[1]= (byte)0x0;
		for(int i=0; i<301; i+=2) {
			array[0]= (byte)(array[0]^DATA_BUFFER[i]);
			array[1]= (byte)(array[1]^DATA_BUFFER[i+1]);
		}// XORing variable values with data buffer values
	    array[0]=(byte)(array[0]^DATA_BUFFER[302]);
	    array[1]=(byte)(array[1]^(byte)0x0); // for odd numbered bytes consider an extra 0 byte during XOR process
		return array;
		 
	} // method to create Integrity check field in data packets



	private static boolean IntegrityCheckProcess(byte[] IACK_BUFFER) {
		// TODO Auto-generated method stub
		byte[] array= new byte[2];
		
		array[0]= (byte)0x0; 
		array[1]= (byte)0x0;
		
		
		
			array[0]=(byte)(array[0]^IACK_BUFFER[0]);
			array[1]=(byte)(array[1]^IACK_BUFFER[1]);
			array[0]= (byte)(array[0]^IACK_BUFFER[2]);
			array[1]= (byte)(array[1]^(byte)0);
		
		array[0]=(byte)(array[0]^IACK_BUFFER[3]);
		array[1]=(byte)(array[1]^IACK_BUFFER[4]); // XORing all values including Integrity check field values for received packet with variable values 
		
		if(array[0]==0x00 && array[1]==0x00) {
			return true;
		} else {
			return false;
		} // if XORing gives zero, integrity check successful
	
	} // method to validate integrity check for received IACK and DACK packets

	public static byte[] Init_Packet(byte[] INIT_BUFFER) {
		// TODO Auto-generated method stub
        
		
		INIT_BUFFER[0]=(byte)0x55;
		byte[] Integrity_Check= new byte[2];
		Random r= new Random();
		
		int seq= r.nextInt(65535); // initializing a random 16 bit sequence number
		
		INIT_BUFFER[1]= (byte)(seq>>8);
		
		INIT_BUFFER[2]= (byte)(seq & 0xFF); // storing the random sequence number in appropriate INIT buffer positions by breaking into two bytes
		

		INIT_BUFFER[3]= (byte) 0x0;
		INIT_BUFFER[4]= (byte) 0xa; // storing number of data packets in 3 and 4
		INIT_BUFFER[5]= (byte) 0x1;
		INIT_BUFFER[6]= (byte) 0x2c; // storing number of payload bytes in every packet in 5 and 6
		
		Integrity_Check= IntegrityCheckField(INIT_BUFFER); // calling the method to give the integrity check values
		INIT_BUFFER[7]= Integrity_Check[0];
		INIT_BUFFER[8]= Integrity_Check[1]; // storing the integrity check values
		return INIT_BUFFER;
		
		} // method to create and return the INIT buffer

	public static byte[] IntegrityCheckField(byte[] INIT_BUFFER) {
		// TODO Auto-generated method stub
		byte[] array= new byte[2];
		array[0]= (byte)0x0;
		array[1]= (byte)0x0;
		
		
		for(int i=0; i<5; i+=2) {
			array[0]= (byte)(array[0]^INIT_BUFFER[i]);
			array[1]= (byte)(array[1]^INIT_BUFFER[i+1]);
			
		}
		array[0]=(byte)(array[0]^INIT_BUFFER[6]);
		array[1]=(byte)(array[1]^(byte)0x0);
		return array;
	
	} // method to create integrity check field for the INIT buffer

	public static long RTT(long t1, long t2) {
		
		long roundTripTime = 0;
		roundTripTime=t2-t1; // round trip time is calculated by subtracting the time of receipt of acknowledgement and the time of sending the data packet
		return roundTripTime;
	} // method to calculate round trip time for each packet
}


