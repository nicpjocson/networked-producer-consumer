# Networked Producer and Consumer
This project consists of a **Producer** and a **ConsumerApp**, designed to run on **separate machines over a network**.  

## Prerequisite: Set the Consumer's IP Address  
Before compiling, open the `Producer.java` file and locate the line:  
```
String consumerHost = "localhost";
```  
Replace `"localhost"` with the IPv4 address of the machine running ConsumerApp (e.g., `"192.168.1.42"`):  
```
String consumerHost = "192.168.1.42"; // IP address of the Consumer
```  
To get the consumer's IP on Windows, run `ipconfig` in CMD. On macOS/Linux, use `ifconfig` or `ip a`.  

## Compilation and Execution In structions using Command-line interface (CLI)  
### 1. Navigate to the Project Directory  
Open a terminal and navigate to the root folder of the project:  
```
cd /path/to/networked-producer-consumer
```  

### 2. Compile the Java Files
Use the following command to compile all `.java` files and output the class files to the `src/` directory:  
```
javac -d src src/*.java
```  

### 3. Run the ConsumerApp (MUST be started first)
On **Machine A** (the consumer), execute the following command:  
```
java -cp src ConsumerApp
```  
This machine should be reachable over the network by the producer.  
### 4. Run the Producer
On **Machine B** (a separate machine from the consumer), run the producer using:  
```
java -cp src Producer
```  
Make sure `Producer` connects to the correct IP address and port where `ConsumerApp` is running.