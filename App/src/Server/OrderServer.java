package Server;




import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;

import DB.*;
import Views.OrderServerView;
import kioskapp.order.Order;
import kioskapp.orderedproduct.OrderedProduct;
import kioskapp.ordertransaction.OrderTransaction;

public class OrderServer {

	@SuppressWarnings({ "unchecked", "resource" })
	public static void main( String [] args) throws IOException, ClassNotFoundException, SQLException {
		

		OrderServerView orderServerGUI = new OrderServerView();
		orderServerGUI.setVisible(true);
		
		int PORT1 = 3215;
		ServerSocket serverSocket = new ServerSocket(PORT1);
		
		Socket clientSocket = serverSocket.accept();
		orderServerGUI.addText("Receive connectino from client 1");// the server gui update thing.all of the addText is for this purpose
		Socket clientSocket2 = serverSocket.accept();
		orderServerGUI.addText("Receive connection from client 2");
		
		int orderReferenceNumber = 1;
		
		while(true) {
			
			DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
			
			
			ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(clientSocket2.getOutputStream());
			DataOutputStream dataOut2 = new DataOutputStream(clientSocket2.getOutputStream());
		
			
			String creditCardNumber = dataIn.readUTF();// used for 2 purpose. to check whethe the user cancel or not and to get credit card
			
			
			if(!creditCardNumber.equalsIgnoreCase( "0")) {// if the client send 0 the it mean the client press the cancel button. if not, then continue
				orderServerGUI.addText("receive the credit card number");
				dataOut2.writeBoolean(true);
				dataOut2.flush();
			
			
				int PORT = 3322;
				String ADDRESS = "localhost";
				Socket socket = new Socket(ADDRESS, PORT);
			
		
				DataInputStream cardDataIn = new DataInputStream(socket.getInputStream());
				DataOutputStream cardDataOut = new DataOutputStream(socket.getOutputStream());
				
			
				orderServerGUI.addText("sending the credit card number to credit card server to validate it");
				cardDataOut.writeUTF(creditCardNumber);
				cardDataOut.flush();
				
				boolean status = cardDataIn.readBoolean();
				orderServerGUI.addText("receive back the validity");
				
				socket.close();// close socket to credit card server after done
			
				////////////////////////////////////////////////receive orderItems and orderTransaction from client
			
				ArrayList<OrderedProduct> orderedItems = new ArrayList<OrderedProduct>();
				orderedItems = (ArrayList<OrderedProduct>) objectInputStream.readObject();
				orderServerGUI.addText("Receive the list of ordered item from client 1");
				
				OrderTransaction orderTransaction = new OrderTransaction();
				orderTransaction = (OrderTransaction) objectInputStream.readObject();
				orderServerGUI.addText("receive the transaction data from client 1");
			
				Order order = new Order();
				
			
				orderServerGUI.addText("settle up the DB");
				order.setOrderedItems(orderedItems);
				order.setTotalAmount(orderTransaction.getAmountCharged());
				order.setOrderReferenceNumber(orderReferenceNumber++);
			
				OrderDatabase orderDatabase = new OrderDatabase();
				OrderedItemDatabase  orderedItemDatabase = new OrderedItemDatabase();
				OrderTransactionDatabase orderTransactionDatabase = new OrderTransactionDatabase();
			
		
				order.setOrderId(orderDatabase.getNextOrderId());
			
				orderTransaction.setOrder(order);
				orderTransaction.setOrderTransactionId(orderTransactionDatabase.getNextOrderId());// because we use this
				orderTransaction.setTransactionStatus(status);
	
				long millis=System.currentTimeMillis();  
				java.sql.Date date=new java.sql.Date(millis); 
				orderTransaction.setTransactioDate(date);
				
			
				orderDatabase.insertOrder(order);
				orderedItemDatabase.insertOrderedItem(order);
				orderTransactionDatabase.insertOrderTransaction(orderTransaction);
				
				
				orderServerGUI.addText("send the receipt to client 1");
				objectOutputStream.writeObject(order);
				objectOutputStream.reset();
				objectOutputStream.writeObject(orderTransaction);
				objectOutputStream.reset();
				
			
			
				if(status) {
					orderServerGUI.addText("send the order to kitchen(client 2)");
					objectOutputStream2.writeObject(order);
					objectOutputStream2.reset();
					objectOutputStream2.writeObject(orderTransaction);
					objectOutputStream2.reset();
					
				}
				else {
					objectOutputStream2.writeObject(null);
				}
			}
			else {
					orderServerGUI.addText("user canceled order");
					dataOut2.writeBoolean(false);
					dataOut2.flush();
			}
		
		
			clientSocket.close();
			clientSocket2.close();
			
		
			clientSocket2 = serverSocket.accept();
			clientSocket = serverSocket.accept();
		}
	}
}
