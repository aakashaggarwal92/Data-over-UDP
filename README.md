# Data-over-UDP
Distributed networking application in Java consisting of a transmitter and a receiver for Reliable Data Transfer over UDP with Data Transfer Statistics

# Reliable Data Transfer over UDP with Data Transfer Statistics

## Overview

This repository contains a distributed networking application implemented in Java that consists of a transmitter and a receiver. It is designed to ensure reliable data transfer over UDP and gather data transfer statistics such as Round-Trip Time (RTT) and overall data rate. 

## Features

- **Integrity Check**: Checks the integrity of each data packet sent.
- **Reliable Data Transfer**: Ensures the packets reach their destination reliably.
- **Data Transfer Statistics**: Measures RTT at the transmitter and data transfer rate at the receiver.

### Data Transfer Scenarios

Statistics are recorded under three different scenarios:
1. Sender and receiver running on the same computer as different processes.
2. Sender and receiver connected via wired Ethernet cable.
3. Sender and receiver connected via WiFi connection.

A comparison of these scenarios is presented in the final statistics.

## Integrity Check Calculation

Detailed explanation of the integrity check algorithm is provided, which involves bitwise XOR operations and a 16-bit variable.


## Protocol Operation

### A. Transmitter Side Operation

- **Initial Handshake Phase**: 
  - Transmitter generates a 16-bit random initial sequence number.
  - Waits for IACK from receiver.
  
- **Data Transmission Phase**:
  - Transmitter sends 300-byte data blocks as DATA packets to the receiver.
  - Uses stop-and-wait protocol for acknowledgements (DACK).

### B. Receiver Side Operation

- **Initial Handshake Phase**: 
  - Receiver waits for INIT packet from transmitter.
  - Sends IACK after successful receipt.

- **Data Transfer Phase**:
  - Receiver accepts DATA packets and sends DACKs.
  - Payloads are displayed on the screen.


![protocol-state-diagram](https://github.com/aakashaggarwal92/Data-over-UDP/assets/8023490/c78bd706-890e-4dc1-91bb-73eac91ef4bd)

 
## RTT & Data Rate Measurements

### Round-Trip Time (RTT) Measurement at Transmitter

The transmitter calculates the RTT for each acknowledged DATA packet using the following steps:

1. Record the current time using Java's `System.currentTimeMillis()` method just before sending each DATA packet.
2. Record the current time again upon receiving the corresponding DACK packet.
3. Calculate the RTT (in milliseconds) as the difference between the two recorded times.

Note: This RTT measurement is considered valid for first-time DACK packets only. If a timeout occurs, and the DATA packet is retransmitted, the RTT measurement process starts over.

At the end of the data transmission, the transmitter displays:
- RTT values for all DATA packets
- Average RTT
- Maximum RTT
- Minimum RTT

### Data Rate Measurement at Receiver

The receiver calculates the overall data rate during the data transmission phase:

1. Record the current time using Java's `System.currentTimeMillis()` method upon receiving the first DATA packet.
2. Record the current time again upon receiving the last DATA packet.
3. Calculate the total transmission time.
4. Calculate the effective data rate in megabits per second (Mbit/s) by dividing the total number of received bytes by the total transmission time.

At the end of the data transmission phase, the receiver prints out the total effective data rate in Mbit/s.


## Packet Structure

The application uses four types of packets for communication:

1. INIT (Initial Packet from Transmitter)
2. IACK (Acknowledgement for INIT packet)
3. DATA (Data Packet)
4. DACK (Acknowledgement for DATA packet)

All multibyte fields in these packets are sent in network byte order (most significant byte first).

### INIT Packet

Fields in the INIT Packet:

- **Packet type (1 byte)**: `55h`, indicates the packet type.
- **Initial sequence number (2 bytes)**: A 16-bit unsigned integer for initial sequence count.
- **Number of data packets (2 bytes)**: Total DATA packets to be sent in the session.
- **Number of payload bytes (2 bytes)**: The number of bytes in each DATA packet payload.
- **Integrity check (2 bytes)**: Check value calculated over the entire packet, excluding this field.

### IACK Packet

Fields in the IACK Packet:

- **Packet type (1 byte)**: `AAh`, indicates the packet type.
- **Acknowledgment (ACK) number (2 bytes)**: Initial sequence number from the INIT packet plus one.
- **Integrity check (2 bytes)**: Check value calculated over the entire packet, excluding this field.

### DATA Packet

Fields in the DATA Packet:

- **Packet type (1 byte)**: `33h`, indicates the packet type.
- **Sequence number (2 bytes)**: The sequence number of the first payload byte in the packet.
- **Payload**: The actual data bytes.
- **Integrity check (2 bytes)**: Check value calculated over the entire packet, excluding this field.

### DACK Packet

Fields in the DACK Packet:

- **Packet type (1 byte)**: `cch`, indicates the packet type.
- **Acknowledgment (ACK) number (2 bytes)**: Sequence number of the next data byte expected from the transmitter.
- **Integrity check (2 bytes)**: Check value calculated over the entire packet, excluding this field.

### Integrity Check Calculation

Integrity check value is calculated using bitwise XOR over all 16-bit segments of the packet, excluding the integrity check field itself. Refer to the Integrity Check section for the algorithm.

## Integrity Check Algorithm

The integrity check aims to ensure that the data packets have not been corrupted during the transmission. The integrity check value is a 16-bit field in the packet header.

### Algorithm Steps

1. **Initialize a 16-bit variable to zero**: This variable will store the integrity check value.

2. **Segment Packet into 16-bit words**: Divide the entire packet, except for the integrity check field, into 16-bit chunks. If the packet has an odd number of bytes, pad the last 16-bit word with a zero at its least significant byte.

3. **Bitwise XOR**: For each 16-bit word in the packet,
    1. Perform bitwise XOR between the 16-bit word and the integrity check variable.
    2. Store the result back in the integrity check variable.

4. **Final Result**: The final value in the integrity check variable is the integrity check value that should be set in the packet before sending it.

### Validation at Receiver Side

At the receiver side, the integrity check is validated using the following steps:

1. **Segment Received Packet into 16-bit words**: Include the integrity check field received in the packet.

2. **Initialize a 16-bit variable to zero**.

3. **Bitwise XOR**: Perform the bitwise XOR as described in the Algorithm Steps.

4. **Check**: If the resulting value of the integrity check variable is zero, then the integrity check passes. Any other value indicates that the packet is corrupted.

The above steps should be followed both at the transmitter and the receiver ends to ensure data integrity during communication.

## Function Descriptions

### Transmitter Functions

1. **`maxRTT()`**:  
   Calculates the maximum Round Trip Time (RTT) from all the RTT values stored in the `rtt[]` array.

2. **`minRTT()`**:  
   Calculates the minimum RTT from all the RTT values stored in the `rtt[]` array.

3. **`averageRTT()`**:  
   Computes the average RTT from all the values in the `rtt[]` array, given in milliseconds.

4. **`DataIntegrityCheckField()`**:  
   Generates the integrity check field value for the DATA packet using bitwise XOR operations. It returns the integrity check value, which is then stored at the 304th and 305th positions in the data buffer.

5. **`IntegrityCheckProcess()`**:  
   Validates the Integrity check for received IACK and DACK packets. If successful, it proceeds with the handshake or sending the next DATA packet.

6. **`Init_Packet()`**:  
   Constructs the INIT packet with appropriate fields like packet type, initial sequence number, etc.

7. **`IntegrityCheckField()`**:  
   Creates the checksum value for the INIT packet and returns it to be stored in the appropriate positions within the packet.

8. **`RTT()`**:  
   Measures the Round Trip Time for each sent and acknowledged packet.

### Receiver Functions

1. **`DataRate()`**:  
   Calculates the overall data rate in Mega Bits per Second (Mbit/s). Takes four arguments: start time, end time, count of DATA packets, and size of each DATA packet.

2. **`Dack_packet()`**:  
   Constructs the DACK packet with the correct acknowledgment number, packet type, and integrity check value.

3. **`DataIntegrityCheck()`**:  
   Validates the integrity check for each received DATA packet. If successful, sends a DACK packet back to the transmitter.

4. **`Iack_Packet()`**:  
   Creates the IACK packet with appropriate field values for the handshake process.

5. **`IntegrityCheckField()`**:  
   Computes the integrity check values for both the IACK and DACK packets.

6. **`IntegrityCheckProcess()`**:  
   Validates the integrity check for the received INIT packet. If successful, sends an IACK packet to initiate the handshake.



## Installation and Usage

Instructions for setting up and running the application.

## License

This project is licensed under the MIT License.

## Contributors

Aakash Aggarwal
Vinayak Agnihotri 

