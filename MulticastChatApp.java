/**
 * 
 */
package multicast_app;

/**
 * 
 * Simple Multicast Chat application 
 * You can run multiple instances that can send messages to each other
 * text displaying is a little broken but otherwise works for educational purposes
 * 
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Scanner;

public class MulticastChatApp {
  private static final String MC_GRP_ADR = "239.0.0.1";
  private static final int PORT = 42000;
  private static final int version = 1;
  private static final int messageCode = 3;
  private static final String clientName = "TIEA322saeijoto";
  
  /**
   * 
   * Creates a custom byte frame for sent Multicast package
   * 
   * these 2 are hardcoded 
   * @param version = 1 
   * @param message = 3
   * @param day Date of birth
   * @param month Month of birth
   * @param year Year of birth
   * @param clientName Hardcoded, for my course
   * @param userName user input
   * @param text user input
   * @return
   */
  public static byte[] setBytes(
      int version, int message, int day,
      int month, int year, String clientName,
      String userName, String text) {
    
    // Calculate field lengths
    int clientLength = clientName.getBytes(StandardCharsets.UTF_8).length;
    int userLength = userName.getBytes(StandardCharsets.UTF_8).length;
    int dataLength = text.getBytes(StandardCharsets.UTF_8).length;
    
    // Calculate the total byte array length
    // 7 is the default in this case
     int totalLength = 7 + clientLength + userLength + dataLength;
     
     byte[] byteArr = new byte[totalLength];
     
     // set the first bytes
     byteArr[0] = (byte) ((version << 4) | message);
     byteArr[1] = (byte) ((day << 3) | (month >> 1));
     byteArr[2] = (byte) ((month << 7) | (year >> 4));
     byteArr[3] = (byte) (((year & 0xf) << 4) | 0);
     byteArr[4] = (byte) clientLength;
     
     // set client name bytes
     byte[] clientBytes = clientName.getBytes(StandardCharsets.UTF_8);
     int index = 5;
     for (byte b : clientBytes) {
         byteArr[index++] = b;
     }

     // set user name bytes
     byte[] userBytes = userName.getBytes(StandardCharsets.UTF_8);
     byteArr[index++] = (byte) userLength;
     for (byte b : userBytes) {
         byteArr[index++] = b;
     }

     // set text bytes
     byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
     byteArr[index++] = (byte) dataLength;
     for (byte b : textBytes) {
         byteArr[index++] = b;
     }

     return byteArr;
  }
  
  @SuppressWarnings("deprecation")
  public static void main(String[] args) {
    try {
      InetAddress group = InetAddress.getByName(MC_GRP_ADR);
      // Open the socket
      MulticastSocket socket = new MulticastSocket(PORT);
      
      // Deprecated, but not caring about that now
      socket.joinGroup(group);
      
      // Start a thread that handles receiving a package
      Thread receiverThread = new Thread(() -> {
        try {
          byte[] buffer = new byte[1024];
          while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String receivedMsg = new String(packet.getData(), 0, packet.getLength());
            System.out.println(receivedMsg);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      
      // Start the receiver package
      receiverThread.start();
      
      // Start listening for input stream
      Scanner scanner = new Scanner(System.in);
      System.out.println("Enter your name: ");
      String username = scanner.nextLine();
      System.out.println("Enter your birth date in this form: yyyy-mm-dd ");
      String bDate = scanner.nextLine();
      LocalDate date = LocalDate.parse(bDate);
      int day = date.getDayOfMonth();
      int month = date.getMonthValue();
      int year = date.getYear();
      
      while (true) {
        System.out.println("Type your message: ");
        String message = scanner.nextLine();
        
        if (message.contains("exit")) {
          socket.leaveGroup(group);
          System.out.println(String.format("user %s has left", username));
          break;
        }
        /**
         *    int version, int message, int day,
         *    int month, int year, String clientName,
         *    String userName, String text)
         */
        byte[] messageBytes = setBytes(
            version, messageCode, day, month, year, clientName, username, message 
            );
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, group, PORT);
        socket.send(packet);
      }
      
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}