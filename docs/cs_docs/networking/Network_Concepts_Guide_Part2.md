# Network Concepts Guide Part 2 - Network Layer to Security

*Continuation of comprehensive networking guide covering IP, transport protocols, network devices, and security*

## Table of Contents (Part 2)
6. [Network Layer & IP](#network-layer)
7. [Transport Layer - TCP/UDP](#transport-layer)
8. [Network Devices](#network-devices)
9. [Application Layer Protocols](#application-protocols)
10. [Network Security & Firewalls](#security-firewalls)
11. [Advanced Networking Concepts](#advanced-concepts)
12. [MCQ Practice Questions](#mcq-questions)

---

## 6. Network Layer & IP {#network-layer}

### Network Layer Functions

**Primary Responsibilities**
- **Logical Addressing**: IP address assignment and management
- **Routing**: Path determination between networks
- **Packet Forwarding**: Moving packets toward destination
- **Fragmentation**: Breaking large packets into smaller ones
- **Congestion Control**: Managing network traffic flow

### Internet Protocol (IP)

**IPv4 (Internet Protocol version 4)**

**IPv4 Header Format**
```
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|Version|  IHL  |Type of Service|          Total Length         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         Identification        |Flags|      Fragment Offset    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Time to Live |    Protocol   |         Header Checksum       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Source Address                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Destination Address                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Options                    |    Padding    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

**Header Fields Explanation**
- **Version (4 bits)**: IP version (4 for IPv4)
- **IHL (4 bits)**: Internet Header Length in 32-bit words
- **Type of Service (8 bits)**: QoS and priority information
- **Total Length (16 bits)**: Total packet length including header
- **Identification (16 bits)**: Unique identifier for fragmented packets
- **Flags (3 bits)**: Control fragmentation (DF, MF, Reserved)
- **Fragment Offset (13 bits)**: Position of fragment in original packet
- **Time to Live (8 bits)**: Maximum hops before packet discarded
- **Protocol (8 bits)**: Next layer protocol (TCP=6, UDP=17, ICMP=1)
- **Header Checksum (16 bits)**: Error detection for header only
- **Source Address (32 bits)**: Sender's IP address
- **Destination Address (32 bits)**: Receiver's IP address
- **Options (variable)**: Optional fields for special features

### IPv4 Addressing

**Address Structure**
- **Length**: 32 bits (4 bytes)
- **Notation**: Dotted decimal (192.168.1.1)
- **Range**: 0.0.0.0 to 255.255.255.255
- **Total Addresses**: 2^32 = 4,294,967,296

**Address Classes (Classful Addressing)**

**Class A**
- **Range**: 1.0.0.0 to 126.255.255.255
- **Default Mask**: 255.0.0.0 (/8)
- **Networks**: 126 networks
- **Hosts per Network**: 16,777,214
- **Use**: Large organizations

**Class B**
- **Range**: 128.0.0.0 to 191.255.255.255
- **Default Mask**: 255.255.0.0 (/16)
- **Networks**: 16,384 networks
- **Hosts per Network**: 65,534
- **Use**: Medium organizations

**Class C**
- **Range**: 192.0.0.0 to 223.255.255.255
- **Default Mask**: 255.255.255.0 (/24)
- **Networks**: 2,097,152 networks
- **Hosts per Network**: 254
- **Use**: Small organizations

**Class D (Multicast)**
- **Range**: 224.0.0.0 to 239.255.255.255
- **Use**: Multicast groups
- **No host addresses**: Used for group communication

**Class E (Experimental)**
- **Range**: 240.0.0.0 to 255.255.255.255
- **Use**: Research and experimental
- **Not for general use**

**Special Addresses**
- **0.0.0.0**: This network
- **127.0.0.1**: Loopback (localhost)
- **255.255.255.255**: Limited broadcast
- **169.254.x.x**: Link-local (APIPA)

### Subnetting

**Purpose of Subnetting**
- **Address Conservation**: Better utilization of IP addresses
- **Network Segmentation**: Divide large networks into smaller ones
- **Security**: Isolate network segments
- **Performance**: Reduce broadcast domains

**Subnet Mask**
- **Function**: Distinguishes network and host portions
- **Format**: Dotted decimal or CIDR notation
- **Example**: 255.255.255.0 = /24

**CIDR (Classless Inter-Domain Routing)**
- **Notation**: IP address followed by /prefix length
- **Example**: 192.168.1.0/24
- **Benefits**: More flexible than classful addressing
- **VLSM**: Variable Length Subnet Masking

**Subnetting Example**
```
Network: 192.168.1.0/24
Requirement: 4 subnets with 60 hosts each

Solution:
- Need 2 bits for 4 subnets (2^2 = 4)
- New mask: /26 (255.255.255.192)
- Subnet size: 64 addresses (62 usable hosts)

Subnets:
1. 192.168.1.0/26   (192.168.1.1 - 192.168.1.62)
2. 192.168.1.64/26  (192.168.1.65 - 192.168.1.126)
3. 192.168.1.128/26 (192.168.1.129 - 192.168.1.190)
4. 192.168.1.192/26 (192.168.1.193 - 192.168.1.254)
```

### IPv6 (Internet Protocol version 6)

**IPv6 Features**
- **Address Length**: 128 bits
- **Address Space**: 2^128 addresses (virtually unlimited)
- **Notation**: Hexadecimal with colons (2001:db8::1)
- **No Fragmentation**: At intermediate routers
- **Built-in Security**: IPSec mandatory
- **Auto-configuration**: Stateless address configuration

**IPv6 Address Types**
- **Unicast**: One-to-one communication
- **Multicast**: One-to-many communication
- **Anycast**: One-to-nearest communication
- **No Broadcast**: Replaced by multicast

**IPv6 Address Format**
```
2001:0db8:85a3:0000:0000:8a2e:0370:7334

Compression rules:
- Leading zeros can be omitted: 2001:db8:85a3:0:0:8a2e:370:7334
- Consecutive zero groups can be replaced with ::: 2001:db8:85a3::8a2e:370:7334
```

### Routing

**Routing Concepts**
- **Routing Table**: Database of network destinations
- **Next Hop**: Next router in path to destination
- **Metric**: Cost of using a particular route
- **Administrative Distance**: Trustworthiness of routing source

**Routing Types**

**Static Routing**
- **Manual Configuration**: Administrator configures routes
- **Advantages**: Predictable, secure, no bandwidth overhead
- **Disadvantages**: Not scalable, no automatic adaptation
- **Use Cases**: Small networks, stub networks

**Dynamic Routing**
- **Automatic**: Routers exchange routing information
- **Adaptation**: Automatically adjusts to topology changes
- **Overhead**: Uses bandwidth for routing updates
- **Complexity**: More complex configuration and troubleshooting

**Routing Algorithms**

**Distance Vector**
- **Principle**: Share routing table with neighbors
- **Metric**: Hop count or composite metric
- **Examples**: RIP, EIGRP
- **Problems**: Slow convergence, routing loops
- **Solutions**: Split horizon, poison reverse, hold-down timers

**Link State**
- **Principle**: Each router knows complete network topology
- **Process**: Flood link state information, calculate shortest path
- **Examples**: OSPF, IS-IS
- **Advantages**: Fast convergence, loop-free
- **Disadvantages**: Higher memory and CPU requirements

**Routing Protocols**

**RIP (Routing Information Protocol)**
- **Type**: Distance vector
- **Metric**: Hop count (maximum 15)
- **Updates**: Every 30 seconds
- **Versions**: RIPv1 (classful), RIPv2 (classless)
- **Use**: Small networks

**OSPF (Open Shortest Path First)**
- **Type**: Link state
- **Metric**: Cost (based on bandwidth)
- **Algorithm**: Dijkstra's shortest path first
- **Features**: Areas, VLSM support, fast convergence
- **Use**: Large enterprise networks

**BGP (Border Gateway Protocol)**
- **Type**: Path vector
- **Use**: Internet routing between autonomous systems
- **Attributes**: AS path, next hop, local preference
- **Policy**: Route selection based on policies
- **Scalability**: Handles Internet-scale routing

### ICMP (Internet Control Message Protocol)

**Purpose**
- **Error Reporting**: Report delivery problems
- **Network Diagnostics**: Tools like ping and traceroute
- **Control Messages**: Network management information

**ICMP Message Types**
- **Echo Request/Reply (Type 8/0)**: Ping functionality
- **Destination Unreachable (Type 3)**: Cannot reach destination
- **Time Exceeded (Type 11)**: TTL expired or fragment timeout
- **Redirect (Type 5)**: Better route available
- **Source Quench (Type 4)**: Congestion control (deprecated)

**Ping Operation**
1. Send ICMP Echo Request to destination
2. Destination responds with ICMP Echo Reply
3. Measure round-trip time
4. Determine reachability and latency

**Traceroute Operation**
1. Send packets with incrementing TTL values
2. Each router decrements TTL and sends Time Exceeded if TTL=0
3. Build list of routers in path to destination
4. Continue until destination reached

---

## 7. Transport Layer - TCP/UDP {#transport-layer}

### Transport Layer Functions

**Primary Responsibilities**
- **Process-to-Process Communication**: Port-based addressing
- **Segmentation**: Break application data into segments
- **Reassembly**: Reconstruct original data at destination
- **Error Control**: Detect and handle transmission errors
- **Flow Control**: Manage data transmission rate
- **Connection Management**: Establish, maintain, terminate connections

### Port Numbers

**Port Number Ranges**
- **Well-Known Ports (0-1023)**: System/privileged services
- **Registered Ports (1024-49151)**: User/application services
- **Dynamic/Private Ports (49152-65535)**: Client connections

**Common Well-Known Ports**
- **HTTP**: 80
- **HTTPS**: 443
- **FTP**: 20 (data), 21 (control)
- **SSH**: 22
- **Telnet**: 23
- **SMTP**: 25
- **DNS**: 53
- **DHCP**: 67 (server), 68 (client)
- **POP3**: 110
- **IMAP**: 143
- **SNMP**: 161

### TCP (Transmission Control Protocol)

**TCP Characteristics**
- **Connection-Oriented**: Establishes connection before data transfer
- **Reliable**: Guarantees delivery and correct order
- **Full-Duplex**: Bidirectional communication
- **Stream-Oriented**: Treats data as continuous stream
- **Flow Control**: Prevents receiver buffer overflow
- **Congestion Control**: Adapts to network conditions

**TCP Header Format**
```
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Source Port          |       Destination Port        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        Sequence Number                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Acknowledgment Number                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Data |           |U|A|P|R|S|F|                               |
| Offset| Reserved  |R|C|S|S|Y|I|            Window             |
|       |           |G|K|H|T|N|N|                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Checksum            |         Urgent Pointer        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Options                    |    Padding    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

**TCP Header Fields**
- **Source/Destination Port (16 bits each)**: Process identification
- **Sequence Number (32 bits)**: Position of data in stream
- **Acknowledgment Number (32 bits)**: Next expected sequence number
- **Data Offset (4 bits)**: TCP header length in 32-bit words
- **Flags (6 bits)**: Control bits (URG, ACK, PSH, RST, SYN, FIN)
- **Window (16 bits)**: Flow control - available buffer space
- **Checksum (16 bits)**: Error detection for header and data
- **Urgent Pointer (16 bits)**: Points to urgent data

**TCP Connection Management**

**Three-Way Handshake (Connection Establishment)**
```
Client                    Server
  |                         |
  |    SYN (seq=x)         |
  |----------------------->|
  |                         |
  |  SYN-ACK (seq=y,ack=x+1)|
  |<-----------------------|
  |                         |
  |    ACK (ack=y+1)       |
  |----------------------->|
  |                         |
  |   Connection Established|
```

1. **Client → Server**: SYN segment with initial sequence number
2. **Server → Client**: SYN-ACK segment acknowledging client's SYN
3. **Client → Server**: ACK segment acknowledging server's SYN

**Four-Way Handshake (Connection Termination)**
```
Client                    Server
  |                         |
  |    FIN (seq=x)         |
  |----------------------->|
  |                         |
  |    ACK (ack=x+1)       |
  |<-----------------------|
  |                         |
  |    FIN (seq=y)         |
  |<-----------------------|
  |                         |
  |    ACK (ack=y+1)       |
  |----------------------->|
  |                         |
  |   Connection Closed     |
```

**TCP Flow Control**

**Sliding Window Protocol**
- **Window Size**: Amount of unacknowledged data allowed
- **Dynamic**: Window size changes based on receiver capacity
- **Zero Window**: Receiver buffer full, sender must stop
- **Window Update**: Receiver advertises available space

**Flow Control Example**
```
Sender          Receiver
  |               |
  | Data (1-100)  |
  |-------------->| Window = 200
  |               |
  | Data (101-200)|
  |-------------->| Window = 100
  |               |
  |   ACK (201)   |
  |<--------------| Window = 0 (buffer full)
  |               |
  | (Wait for     |
  |  window       |
  |  update)      |
  |               |
  |   ACK (201)   |
  |<--------------| Window = 150 (space available)
```

**TCP Congestion Control**

**Congestion Window (cwnd)**
- **Purpose**: Limit sending rate based on network conditions
- **Effective Window**: min(advertised window, congestion window)
- **Dynamic**: Adjusts based on network feedback

**Congestion Control Algorithms**

**Slow Start**
- **Initial**: cwnd = 1 MSS (Maximum Segment Size)
- **Growth**: cwnd doubles every RTT (exponential growth)
- **Threshold**: Switch to congestion avoidance at ssthresh

**Congestion Avoidance**
- **Growth**: cwnd increases by 1 MSS per RTT (linear growth)
- **Conservative**: Slower growth to avoid congestion

**Fast Retransmit**
- **Trigger**: Three duplicate ACKs received
- **Action**: Immediately retransmit lost segment
- **Assumption**: Segment lost, not delayed

**Fast Recovery**
- **After Fast Retransmit**: Don't go back to slow start
- **cwnd**: Set to ssthresh + 3 MSS
- **Continue**: Congestion avoidance phase

### UDP (User Datagram Protocol)

**UDP Characteristics**
- **Connectionless**: No connection establishment
- **Unreliable**: No delivery guarantee
- **Message-Oriented**: Preserves message boundaries
- **Low Overhead**: Minimal header (8 bytes)
- **Fast**: No connection setup or flow control delays
- **Broadcast/Multicast**: Supports one-to-many communication

**UDP Header Format**
```
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Source Port          |       Destination Port        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|            Length             |           Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

**UDP Header Fields**
- **Source Port (16 bits)**: Sending process port number
- **Destination Port (16 bits)**: Receiving process port number
- **Length (16 bits)**: UDP header + data length
- **Checksum (16 bits)**: Error detection (optional in IPv4)

**UDP Applications**
- **DNS**: Quick query/response
- **DHCP**: Address assignment
- **SNMP**: Network management
- **TFTP**: Simple file transfer
- **Real-time Applications**: Video streaming, online gaming
- **Broadcast Services**: Network discovery

### TCP vs UDP Comparison

| Feature | TCP | UDP |
|---------|-----|-----|
| Connection | Connection-oriented | Connectionless |
| Reliability | Reliable | Unreliable |
| Ordering | Ordered delivery | No ordering guarantee |
| Flow Control | Yes | No |
| Congestion Control | Yes | No |
| Header Size | 20+ bytes | 8 bytes |
| Speed | Slower | Faster |
| Broadcast | No | Yes |
| Applications | Web, email, file transfer | DNS, streaming, gaming |

### Socket Programming Concepts

**Socket Types**
- **Stream Sockets**: TCP-based, reliable
- **Datagram Sockets**: UDP-based, unreliable
- **Raw Sockets**: Direct IP access

**Socket Operations**
- **socket()**: Create socket
- **bind()**: Assign address to socket
- **listen()**: Wait for connections (TCP server)
- **accept()**: Accept connection (TCP server)
- **connect()**: Establish connection (TCP client)
- **send()/recv()**: Data transfer (TCP)
- **sendto()/recvfrom()**: Data transfer (UDP)
- **close()**: Close socket

---

## 8. Network Devices {#network-devices}

### Physical Layer Devices

**Repeater**
- **Function**: Amplify and regenerate signals
- **OSI Layer**: Physical (Layer 1)
- **Purpose**: Extend network distance
- **Limitation**: Cannot filter or make intelligent decisions
- **Usage**: Legacy networks, now mostly replaced

**Hub**
- **Function**: Multi-port repeater
- **OSI Layer**: Physical (Layer 1)
- **Operation**: Broadcasts all data to all ports
- **Collision Domain**: Single collision domain for all ports
- **Duplex**: Half-duplex operation
- **Obsolete**: Replaced by switches

### Data Link Layer Devices

**Bridge**
- **Function**: Connect two network segments
- **OSI Layer**: Data Link (Layer 2)
- **Learning**: Builds MAC address table
- **Filtering**: Forwards frames only when necessary
- **Collision Domains**: Separate collision domain per segment
- **Spanning Tree**: Prevents loops in bridged networks

**Switch**
- **Function**: Multi-port bridge with enhanced features
- **OSI Layer**: Data Link (Layer 2)
- **Operation**: Learns MAC addresses, forwards intelligently
- **Full-Duplex**: Simultaneous send/receive on each port
- **Collision Domains**: Each port is separate collision domain
- **Broadcast Domain**: Single broadcast domain (unless VLANs used)

**Switch Types**

**Unmanaged Switch**
- **Configuration**: Plug-and-play, no configuration
- **Features**: Basic switching functionality
- **Cost**: Lower cost
- **Use**: Small networks, home use

**Managed Switch**
- **Configuration**: Web interface, CLI, SNMP
- **Features**: VLANs, QoS, port mirroring, security
- **Monitoring**: Traffic statistics, port status
- **Use**: Enterprise networks

**Layer 3 Switch**
- **Function**: Switching + routing capabilities
- **OSI Layers**: Layer 2 and Layer 3
- **Performance**: Hardware-based routing (wire speed)
- **Features**: Inter-VLAN routing, static/dynamic routing
- **Use**: Core/distribution layer in enterprise networks

**Switch Operations**

**MAC Address Learning**
1. **Frame Arrival**: Frame arrives on port
2. **Source Learning**: Record source MAC and port in table
3. **Destination Lookup**: Check if destination MAC in table
4. **Forward/Flood**: Forward to specific port or flood all ports

**Switching Methods**
- **Store-and-Forward**: Receive entire frame, check for errors, then forward
- **Cut-Through**: Start forwarding as soon as destination address read
- **Fragment-Free**: Read first 64 bytes, then forward

### Network Layer Devices

**Router**
- **Function**: Route packets between different networks
- **OSI Layer**: Network (Layer 3)
- **Addressing**: Uses IP addresses for routing decisions
- **Broadcast Domains**: Each interface is separate broadcast domain
- **Routing Table**: Maintains table of network destinations
- **Protocols**: Supports routing protocols (RIP, OSPF, BGP)

**Router Components**
- **CPU**: Processes routing protocols and management
- **Memory**: 
  - **RAM**: Running configuration, routing tables, packet buffers
  - **NVRAM**: Startup configuration
  - **Flash**: Operating system (IOS)
  - **ROM**: Bootstrap program, basic IOS
- **Interfaces**: Network connections (Ethernet, Serial, etc.)

**Routing Process**
1. **Packet Reception**: Receive packet on interface
2. **Destination Analysis**: Extract destination IP address
3. **Routing Table Lookup**: Find best match for destination
4. **TTL Decrement**: Decrease TTL by 1
5. **Checksum Recalculation**: Update IP header checksum
6. **Frame Encapsulation**: Create new frame for next hop
7. **Packet Forwarding**: Send packet out appropriate interface

**Default Gateway**
- **Purpose**: Router interface for local network
- **Function**: Route packets to destinations outside local network
- **Configuration**: Set on all network devices
- **Example**: Home router connecting to Internet

### Application Layer Devices

**Gateway**
- **Function**: Protocol conversion between different network types
- **OSI Layer**: All layers (1-7)
- **Purpose**: Connect networks with different protocols
- **Examples**: Email gateways, protocol converters
- **Intelligence**: Application-aware processing

**Proxy Server**
- **Function**: Intermediary for client requests
- **OSI Layer**: Application (Layer 7)
- **Types**: Forward proxy, reverse proxy
- **Benefits**: Caching, security, content filtering
- **Examples**: Web proxy, email proxy

**Load Balancer**
- **Function**: Distribute traffic across multiple servers
- **OSI Layer**: Layer 4 (transport) or Layer 7 (application)
- **Methods**: Round-robin, least connections, weighted
- **Benefits**: High availability, scalability, performance
- **Types**: Hardware, software, cloud-based

### Wireless Devices

**Wireless Access Point (WAP)**
- **Function**: Provide wireless connectivity to wired network
- **OSI Layer**: Data Link (Layer 2)
- **Standards**: 802.11a/b/g/n/ac/ax
- **Features**: SSID broadcast, security (WPA/WPA2/WPA3)
- **Modes**: Access point, bridge, repeater

**Wireless Router**
- **Function**: Combines router, switch, and access point
- **Features**: NAT, DHCP, firewall, wireless
- **Use**: Home and small office networks
- **Antennas**: Internal or external for better coverage

**Wireless Controller**
- **Function**: Centralized management of multiple access points
- **Features**: Configuration, monitoring, security policies
- **Benefits**: Simplified management, consistent policies
- **Use**: Enterprise wireless networks

### Network Device Comparison

| Device | OSI Layer | Collision Domains | Broadcast Domains | Intelligence |
|--------|-----------|-------------------|-------------------|--------------|
| Hub | 1 | 1 | 1 | None |
| Bridge | 2 | 2 | 1 | MAC learning |
| Switch | 2 | Per port | 1 (or per VLAN) | MAC learning, VLANs |
| Router | 3 | Per interface | Per interface | Routing, ACLs |
| Gateway | 1-7 | Varies | Varies | Protocol conversion |

### Device Selection Criteria

**Performance Requirements**
- **Throughput**: Data transfer rate needed
- **Latency**: Delay requirements
- **Packet Processing**: Packets per second capability
- **Concurrent Connections**: Number of simultaneous connections

**Feature Requirements**
- **Layer 2**: VLANs, STP, link aggregation
- **Layer 3**: Routing protocols, ACLs, QoS
- **Security**: Firewall, VPN, access control
- **Management**: SNMP, web interface, CLI

**Scalability**
- **Port Density**: Number of ports needed
- **Stacking**: Ability to combine multiple devices
- **Modular**: Expandable with additional modules
- **Performance Growth**: Upgrade path for future needs

---

## 9. Application Layer Protocols {#application-protocols}

### Domain Name System (DNS)

**Purpose and Function**
- **Name Resolution**: Translate domain names to IP addresses
- **Hierarchical**: Distributed database system
- **Caching**: Improves performance and reduces traffic
- **Redundancy**: Multiple servers for reliability

**DNS Hierarchy**
```
Root (.)
├── Top-Level Domains (TLD)
│   ├── Generic TLD (.com, .org, .net)
│   ├── Country Code TLD (.us, .uk, .jp)
│   └── Infrastructure TLD (.arpa)
├── Second-Level Domains
│   ├── example.com
│   └── google.com
└── Subdomains
    ├── www.example.com
    └── mail.google.com
```

**DNS Record Types**
- **A Record**: Maps hostname to IPv4 address
- **AAAA Record**: Maps hostname to IPv6 address
- **CNAME**: Canonical name (alias) record
- **MX**: Mail exchange server
- **NS**: Name server record
- **PTR**: Pointer record (reverse DNS)
- **SOA**: Start of authority
- **TXT**: Text record (often used for verification)

**DNS Query Process**
1. **Client Query**: Application requests IP for domain name
2. **Local Cache**: Check local DNS cache
3. **Recursive Query**: Query local DNS server
4. **Root Server**: Query root name server if needed
5. **TLD Server**: Query top-level domain server
6. **Authoritative Server**: Query authoritative name server
7. **Response**: Return IP address to client
8. **Caching**: Cache result for future queries

**DNS Message Format**
```
Header (12 bytes)
├── ID (16 bits): Query identifier
├── Flags (16 bits): QR, Opcode, AA, TC, RD, RA, Z, RCODE
├── QDCOUNT (16 bits): Number of questions
├── ANCOUNT (16 bits): Number of answers
├── NSCOUNT (16 bits): Number of authority records
└── ARCOUNT (16 bits): Number of additional records

Question Section
├── QNAME: Domain name
├── QTYPE: Query type (A, AAAA, MX, etc.)
└── QCLASS: Query class (usually IN for Internet)

Answer Section (variable)
Resource Record Section (variable)
Additional Section (variable)
```

### Email Protocols

**SMTP (Simple Mail Transfer Protocol)**
- **Purpose**: Send email between servers and from clients to servers
- **Port**: 25 (standard), 587 (submission), 465 (SMTPS)
- **Protocol**: Text-based, command-response
- **Security**: STARTTLS for encryption
- **Authentication**: SMTP AUTH for client authentication

**SMTP Commands**
- **HELO/EHLO**: Identify client to server
- **MAIL FROM**: Specify sender address
- **RCPT TO**: Specify recipient address
- **DATA**: Begin message content
- **QUIT**: End session
- **RSET**: Reset session
- **VRFY**: Verify email address

**SMTP Session Example**
```
Client: EHLO client.example.com
Server: 250-server.example.com Hello client.example.com
Server: 250-SIZE 52428800
Server: 250-AUTH PLAIN LOGIN
Server: 250 STARTTLS

Client: MAIL FROM:<sender@example.com>
Server: 250 OK

Client: RCPT TO:<recipient@example.com>
Server: 250 OK

Client: DATA
Server: 354 Start mail input; end with <CRLF>.<CRLF>
Client: Subject: Test Message
Client: 
Client: This is a test message.
Client: .
Server: 250 OK Message accepted

Client: QUIT
Server: 221 Bye
```

**POP3 (Post Office Protocol version 3)**
- **Purpose**: Download email from server to client
- **Port**: 110 (standard), 995 (POP3S)
- **Operation**: Download and delete from server
- **States**: Authorization, Transaction, Update
- **Limitation**: Single device access, no server-side folders

**POP3 Commands**
- **USER**: Specify username
- **PASS**: Specify password
- **STAT**: Get mailbox statistics
- **LIST**: List messages
- **RETR**: Retrieve message
- **DELE**: Mark message for deletion
- **QUIT**: End session and expunge deleted messages

**IMAP (Internet Message Access Protocol)**
- **Purpose**: Access email stored on server
- **Port**: 143 (standard), 993 (IMAPS)
- **Operation**: Email remains on server
- **Features**: Multiple device access, server-side folders, search
- **Synchronization**: Changes synchronized across devices

**IMAP vs POP3 Comparison**
| Feature | IMAP | POP3 |
|---------|------|------|
| Email Storage | Server | Client |
| Multiple Devices | Yes | No |
| Server-side Folders | Yes | No |
| Offline Access | Limited | Full |
| Bandwidth Usage | Higher | Lower |
| Server Storage | More required | Less required |

### File Transfer Protocols

**FTP (File Transfer Protocol)**
- **Purpose**: Transfer files between client and server
- **Ports**: 21 (control), 20 (data)
- **Connections**: Separate control and data connections
- **Authentication**: Username/password or anonymous
- **Modes**: Active and passive

**FTP Connection Modes**

**Active Mode**
1. Client connects to server port 21 (control)
2. Client sends PORT command with client IP and port
3. Server connects back to client for data transfer
4. Problem: Client firewall may block incoming connection

**Passive Mode**
1. Client connects to server port 21 (control)
2. Client sends PASV command
3. Server responds with IP and port for data connection
4. Client connects to server for data transfer
5. Solution: Client initiates both connections

**FTP Commands**
- **USER**: Username
- **PASS**: Password
- **PWD**: Print working directory
- **CWD**: Change working directory
- **LIST**: List directory contents
- **RETR**: Download file
- **STOR**: Upload file
- **DELE**: Delete file
- **QUIT**: End session

**SFTP (SSH File Transfer Protocol)**
- **Purpose**: Secure file transfer over SSH
- **Port**: 22 (SSH)
- **Security**: Encryption and authentication
- **Features**: Directory browsing, file permissions
- **Advantage**: Single encrypted connection

**TFTP (Trivial File Transfer Protocol)**
- **Purpose**: Simple file transfer
- **Port**: 69 (UDP)
- **Features**: No authentication, no directory listing
- **Use Cases**: Boot images, configuration files
- **Simplicity**: Minimal implementation

### Web Protocols

**HTTP (Hypertext Transfer Protocol)**
- **Purpose**: Transfer web pages and resources
- **Port**: 80
- **Protocol**: Request-response, stateless
- **Methods**: GET, POST, PUT, DELETE, HEAD, OPTIONS
- **Versions**: HTTP/1.0, HTTP/1.1, HTTP/2, HTTP/3

**HTTP Request Format**
```
GET /index.html HTTP/1.1
Host: www.example.com
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
Accept: text/html,application/xhtml+xml
Accept-Language: en-US,en;q=0.9
Connection: keep-alive

[Optional message body]
```

**HTTP Response Format**
```
HTTP/1.1 200 OK
Date: Mon, 01 Jan 2024 12:00:00 GMT
Server: Apache/2.4.41
Content-Type: text/html; charset=UTF-8
Content-Length: 1234
Connection: keep-alive

<!DOCTYPE html>
<html>
<head><title>Example</title></head>
<body><h1>Hello World</h1></body>
</html>
```

**HTTP Status Codes**
- **1xx Informational**: 100 Continue, 101 Switching Protocols
- **2xx Success**: 200 OK, 201 Created, 204 No Content
- **3xx Redirection**: 301 Moved Permanently, 302 Found, 304 Not Modified
- **4xx Client Error**: 400 Bad Request, 401 Unauthorized, 404 Not Found
- **5xx Server Error**: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable

**HTTPS (HTTP Secure)**
- **Purpose**: Secure web communication
- **Port**: 443
- **Security**: TLS/SSL encryption
- **Authentication**: Server certificates
- **Process**: TLS handshake, then encrypted HTTP

**HTTP/2 Features**
- **Multiplexing**: Multiple requests over single connection
- **Server Push**: Server can send resources before requested
- **Header Compression**: HPACK compression
- **Binary Protocol**: More efficient than text-based HTTP/1.1

### Other Application Protocols

**DHCP (Dynamic Host Configuration Protocol)**
- **Purpose**: Automatic IP address assignment
- **Ports**: 67 (server), 68 (client)
- **Process**: DORA (Discover, Offer, Request, Acknowledge)
- **Lease**: Temporary IP address assignment
- **Options**: Subnet mask, default gateway, DNS servers

**DHCP Process**
1. **DHCP Discover**: Client broadcasts request for IP
2. **DHCP Offer**: Server offers IP address and configuration
3. **DHCP Request**: Client requests specific IP address
4. **DHCP Acknowledge**: Server confirms assignment

**SNMP (Simple Network Management Protocol)**
- **Purpose**: Network device management and monitoring
- **Port**: 161 (agent), 162 (manager for traps)
- **Versions**: SNMPv1, SNMPv2c, SNMPv3
- **Components**: Manager, Agent, MIB (Management Information Base)
- **Operations**: GET, SET, GETNEXT, GETBULK, TRAP

**Telnet**
- **Purpose**: Remote terminal access
- **Port**: 23
- **Security**: No encryption (plaintext)
- **Usage**: Legacy remote access, network device configuration
- **Replacement**: SSH for secure remote access

**SSH (Secure Shell)**
- **Purpose**: Secure remote access and file transfer
- **Port**: 22
- **Security**: Encryption, authentication, integrity
- **Features**: Terminal access, file transfer (SCP/SFTP), tunneling
- **Authentication**: Password, public key, certificates

This completes Part 2 of the networking guide, covering the Network Layer through Application Layer protocols. The content provides comprehensive coverage of IP addressing, routing, transport protocols, network devices, and application layer services essential for networking fundamentals and exam preparation.