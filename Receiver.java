import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class Receiver {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		                   
		long t1=0, t2=0; // long variables to store recorded time in milliseconds
		 byte[] INIT_BUFFER= new byte[9]; // INIT buffer array
		 byte[] IACK_BUFFER= new byte[5]; // IACK buffer array
		 byte[] DATA_BUFFER= new byte[305]; // DATA buffer array
		 byte[] DACK_BUFFER= new byte[5]; // DACK buffer array
		 byte[] DATAPACK_NO= new byte[2]; // array to store number of packets expected by receiver
		 byte[] PAYLOAD= new byte[2]; // array to store number of payload bytes per packet expected by the receiver
		 byte[] seq_no= new byte[2]; // array to hold sequence numbers of incoming packets
		    
		    DatagramSocket recSocket= new DatagramSocket(9999); // receiver socket

		    System.out.println("Waiting for INIT packet from transmitter.....");
	    	
               
		    DatagramPacket INIT_packet= new DatagramPacket(INIT_BUFFER, INIT_BUFFER.length);
		    DatagramPacket DATA_packet= new DatagramPacket(DATA_BUFFER, DATA_BUFFER.length); // creating packets to be received

 
		  
		    recSocket.receive(INIT_packet); 
		    System.out.print("INIT packet received\nINIT packet: ");
	    for(int k=0; k<INIT_BUFFER.length; k++) {
	    	System.out.print( " "+String.format("%02x",INIT_BUFFER[k]));
		    } // print incoming INIT buffer
		    InetAddress transmitter_address= INIT_packet.getAddress(); // get transmitter's IP address
		    int transmitter_port= INIT_packet.getPort(); // get transmitter's port number

		    int a= INIT_BUFFER[2];
		    if(IntegrityCheckProcess(INIT_BUFFER)){ 
		    	if(INIT_BUFFER[0]==(byte)0x55) { // if integrity check is successful and packet number is correct
		    	DATAPACK_NO[0]= INIT_BUFFER[3];
		    	DATAPACK_NO[1]= INIT_BUFFER[4]; // hold number of data packets that the transmitter will send
		    	PAYLOAD[0]= INIT_BUFFER[5];
		    	PAYLOAD[1]= INIT_BUFFER[6]; // hold number of payload bytes transmitter will send in each packet
		    	seq_no[0]= INIT_BUFFER[1];
		    	seq_no[1]= (byte)(a+1); // increment received sequence number by 1
		    	IACK_BUFFER=Iack_Packet(INIT_BUFFER); // create IACK packet to be sent
			    DatagramPacket IACK_packet= new DatagramPacket(IACK_BUFFER, IACK_BUFFER.length, transmitter_address, transmitter_port);
                recSocket.send(IACK_packet); // create and send the IACK packet to the transmitter
                System.out.println("\nIACK packet sent");
                	}
                } 
                 // handshake over
		    System.out.println("--------------------------------------------------------------------------------------------------------------------------");
		    System.out.println("Waiting for data......\n"); // start receiving data when handshake over
		    int c=0; // counter for number of data packets received
		    int DataPackets= (DATAPACK_NO[0] << 8)+ (DATAPACK_NO[1] & 0xFF); // finding decimal value of number of data packets expected
            while(c<DataPackets) { // while loop for receiving not more than expected number of data packets    	
		    recSocket.receive(DATA_packet);
            c++; // increment for every received data packet
            System.out.print(c+") ");
			int seq= (seq_no[0] << 8)+ (seq_no[1] & 0xFF); // decimal value for IACK sequence number
			int seq1= seq+300; // incrementing sequence number by 300- seq1 represents sequence number of next expected data packet 
			//int data_seq= (DATA_BUFFER[1] << 8)+ (DATA_BUFFER[2] & 0xFF);

            if(DataIntegrityCheck(DATA_BUFFER)) {
            	if(DATA_BUFFER[0]==0x33) {
            		if(DATA_BUFFER[1]==seq_no[0] && DATA_BUFFER[2]==seq_no[1]) {
            			// if required conditions are met
            			if(c==1) {
            				t1=System.currentTimeMillis();
            			} // recording time in milliseconds for the first received data packet
            		if(c==10) {
            			t2=System.currentTimeMillis();
            			} // recording time for last received data packet
            		
            		seq_no[0]= (byte)(seq1 >> 8);
            		seq_no[1]= (byte)(seq1 & 0xFF); // update seq_no so as to update seq in the next iteration for next expected data packet
            		System.out.println("Receiving data packet....");
            		System.out.print("Data packet: ");
            		for(int i2=3; i2<303; i2++) {
            			System.out.print(" "+ String.format("%02x", DATA_BUFFER[i2]));
            		} // print received data packet
            		
            		DACK_BUFFER= Dack_packet(DATA_BUFFER); // create DACK buffer

    			    DatagramPacket DACK_packet= new DatagramPacket(DACK_BUFFER, DACK_BUFFER.length, transmitter_address, transmitter_port);
                    recSocket.send(DACK_packet); // create and send DACK packet to transmitter
                    System.out.print("\nDACK packet:");
                    for(int b=0; b<DACK_BUFFER.length; b++) {
                    	System.out.print(" "+String.format("%02x", DACK_BUFFER[b]));
                    } // print DACK buffer
            		System.out.println("\nDACK packet sent\n");
            		}
            		
            		}
            
		    	}} // while loop ends
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------------");
                   System.out.println("The data transmission rate for the process is: "+ DataRate(t1, t2, DATA_BUFFER, DataPackets)+" Mbps");
                    // printing the calculated data transmission rate using the DataRate method
		    
           recSocket.close();      

	}
	
	
		    
		    private static double DataRate(long t1, long t2, byte[] DATA_BUFFER, int DataPackets) {
		// TODO Auto-generated method stub
	        int Megabits= ((DATA_BUFFER.length*DataPackets)*8)/1024*1024; // converting bits to megabits
	        long time= (t2-t1); // time elapsed between first data packet received and last data packet received
	        double Rate= Megabits/time; // rate in double 
	        Rate=Rate/1000; // calculating for time in s
			return Rate; 
	        
	} // method to calculate data rate



			private static byte[] Dack_packet(byte[] DATA_BUFFER) {
		// TODO Auto-generated method stub
		    	byte[] DACK_BUFFER= new byte[5];
		    	int data_seq= (DATA_BUFFER[1] << 8)+ (DATA_BUFFER[2] & 0xFF); // received packet sequence number
		    	int seq= data_seq+300; //increment by 300 for DACK acknowledgment number
		    	if(seq>65535) {
		    		seq=seq-65536;
		    	  } // wrap around
                   DACK_BUFFER[0]=(byte)0xCC;
                   DACK_BUFFER[1]=(byte)(seq >> 8);
                   DACK_BUFFER[2]=(byte)(byte)(seq & 0xFF);
                   byte[] bytes= new byte[2];
                   bytes= IntegrityCheckField(DACK_BUFFER);
                   DACK_BUFFER[3]=bytes[0];
                   DACK_BUFFER[4]=bytes[1];
                   return DACK_BUFFER;
                   
                   
                   
		    } // method to create DACK buffer



			private static boolean DataIntegrityCheck(byte[] DATA_BUFFER) {
		// TODO Auto-generated method stub
		    	byte[] array= new byte[2];
				array[0]= (byte)0x0;
				array[1]= (byte)0x0;
				for(int i=0; i<301; i+=2) {
					array[0]= (byte)(array[0]^DATA_BUFFER[i]);
					array[1]= (byte)(array[1]^DATA_BUFFER[i+1]);
				}
			    array[0]=(byte)(array[0]^DATA_BUFFER[302]);
			    array[1]=(byte)(array[1]^(byte)0x0);
                array[0]= (byte)(array[0]^DATA_BUFFER[303]);
                array[1]= (byte)(array[1]^DATA_BUFFER[304]); // XORing all fields
			
                if(array[0]==(byte)0x0 && array[1]==(byte)0x0) {
                	return true;
                }
                else {
                	return false;
                } // if 0 integrity check successful
		    } // method to do integrity check on data packets
		    
		    
	



			private static byte[] Iack_Packet(byte[] INIT_BUFFER) {
		// TODO Auto-generated method stub
			    byte[] IACK_BUFFER= new byte[5];
		    	int a= INIT_BUFFER[2];
		    	byte[] Integrity_Check= new byte[2];

		    	byte[] seq_no= new byte[2];
				seq_no[0]= INIT_BUFFER[1];
		    	seq_no[1]= (byte)(a+1); // acknowledgment number is one greater than received INIT sequence number
		    	int seq= (seq_no[0] << 8)+ (seq_no[1] & 0xFF);
		    	if(seq>65535) {
		    		seq=seq-65536;
		    	  } // checking for exceeding sequence number and wrapping around
		    	seq_no[0]= (byte)(seq >> 8);
		    	seq_no[1]= (byte)(seq & 0xFF);
		    	IACK_BUFFER[0]=(byte)(0xaa & 0xFF);
		    	IACK_BUFFER[1]=seq_no[0];
		    	IACK_BUFFER[2]=seq_no[1];
		    	Integrity_Check= IntegrityCheckField(IACK_BUFFER);
		    	IACK_BUFFER[3]=Integrity_Check[0];
		    	IACK_BUFFER[4]= Integrity_Check[1];
				return IACK_BUFFER;
		    	
		    	
		    	
		    	
	} // method to create IACK buffer

			private static byte[] IntegrityCheckField(byte[] IACK_BUFFER) {
				// TODO Auto-generated method stub
				byte[] array= new byte[2];
				array[0]= (byte)0;
				
				array[1]= (byte)0;
				
				for(int i=0; i<2; i+=2) {
					array[0]= (byte)(array[0]^IACK_BUFFER[i]);
					array[1]= (byte)(array[1]^IACK_BUFFER[i+1]);
				}
				array[0]=(byte)(array[0]^IACK_BUFFER[2]);
				array[1]=(byte)(array[1]^(byte)0);
				return array;

			} // method to calculate Integrity check field for IACK and DACK buffers

			private static boolean IntegrityCheckProcess(byte[] INIT_BUFFER) {
				// TODO Auto-generated method stub
				byte[] array= new byte[2];
				
				array[0]= (byte)0x00;
				array[1]= (byte)0x00;
				
				for(int i=0; i<5; i+=2) {
					array[0]=(byte)(array[0]^INIT_BUFFER[i]);
					array[1]=(byte)(array[1]^INIT_BUFFER[i+1]);
				}
				array[0]=(byte)(array[0]^INIT_BUFFER[6]);
				array[1]=(byte)(array[1]^(byte)0);
			
				array[0]=(byte)(array[0]^INIT_BUFFER[7]);
				array[1]=(byte)(array[1]^INIT_BUFFER[8]); //XORing all INIT values
				
				
				if(array[0]==0x00 && array[1]==0x00) {
					return true;
				} else {
					return false;
				} 
				} // method for integrity check process for INIT packet
 
		   }



